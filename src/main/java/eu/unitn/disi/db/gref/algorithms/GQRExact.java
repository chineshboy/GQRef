/*
 * The MIT License
 *
 * Copyright 2013 Davide Mottin <mottin@disi.unitn.eu>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package eu.unitn.disi.db.gref.algorithms;

import de.parmol.graph.Graph;
import de.parmol.graph.MutableGraph;
import eu.unitn.disi.db.command.algorithmic.AlgorithmInput;
import eu.unitn.disi.db.command.exceptions.AlgorithmExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * This algorithm computes the reformulations of a figiven query over the given
 * Graph database using the exact algorithm described in the paper. GQR (Graph
 * Query Reformulation)
 *
 * The objective function is  
 * <code> 
 * maximize cov(S) + \lambda * \sum_{q1,q2} div(q1, q2)
 * subject to |S| < k
 * </code>
 *
 * where k and lambda ar user-defined parameters.
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
//TODO: Split query answering and reformulation algorithm
public class GQRExact extends LatticeAlgorithm {

    @AlgorithmInput(
            description = "Diversification factor",
            mandatory = false,
            defaultValue = "0.5"
    )
    protected float lambda;


    protected int callToExtend = 0;
    
    protected Set<ReformulatedQuery> checkQueries; 
    
    protected static final int REFORMULATION_PRINT_COUNT = 1000;


    protected int queryCount; 
    private int treeCount = 0;
    
    public GQRExact() {
    }

    @Override
    public void compute() throws AlgorithmExecutionException {
        int i;
        StopWatch watch = new StopWatch();
        LinkedList<ReformulatedQuery> queue;
        ReformulatedQuery currentQuery;//, candidateQuery;
        
        //Step 2: construct the lattice (this is exact, first we need the lattice)
        callToExtend = 0;
        queryCount = 0; 
        queue = new LinkedList<>();
        queue.add(lattice.getRoot());
        watch.start();

        //TODO: (b) Add a level indication
        //TODO: (c) Manage directions as well
        info("Starting lattice generation");
        while (!queue.isEmpty()) {
            //Collections.shuffle(queue);
            currentQuery = queue.poll();
            if (currentQuery.resultsNumber() > 1) { //Speed-up optimization 
                extend(lattice, currentQuery, queue);
            }
            currentQuery.clear();//Optimize the space, remove unused structures. 
        }
        algorithmTime = watch.getElapsedTimeMillis();
        info("Time to build the lattice: %dms", watch.getElapsedTimeMillis());
        //debug("Reformulation lattice\n%s", lattice);
        info("Total number of reformulations: %d", lattice.size());

        //Step 3: greedy algorithm to compute the final set s
        //maximize cov(S) + \lambda * \sum_{q1,q2} div(q1, q2)
        watch.reset();
        s = new LinkedHashSet<>();
        
        TreeMap<Double,Set<ReformulatedQuery>> orderedReformulations;
        Set<ReformulatedQuery> queries; 
        double score; 
        List<ReformulatedQuery> checkList = null; 
        i = 0;
        if (checkQueries != null) {
            checkList = new ArrayList<>(checkQueries);
        }
        
        while (s.size() < k) {
            orderedReformulations = new TreeMap<>();
            for (ReformulatedQuery q : lattice.getIndex().values()) {
                if (!s.contains(q)) {
                    score = coverageDiff(s, q) / 2.0 + lambda * diversityDiff(s, q);
                    q.setScore(score);
                    queries = orderedReformulations.get(score);
                    if (queries == null) {
                        queries = new HashSet<>();
                    }
                    queries.add(q);
                    orderedReformulations.put(score, queries);
                }
            }
            queries = orderedReformulations.lastEntry().getValue();
            if (checkList != null) {
                ReformulatedQuery qPrime = checkList.get(i);
                if (queries.contains(qPrime)) {
                    currentQuery = qPrime;
                } else {
                    error("Query %s with marginal gain %f is not the maximum!", checkList.get(i), coverageDiff(s, qPrime) / 2.0 + lambda * diversityDiff(s, qPrime));
                    currentQuery = queries.iterator().next();
                }
                i++;
            } else {
                currentQuery = queries.iterator().next();
            }
            info("Reformulated Query %s obj marginal gain: %f, size: %d", currentQuery, coverageDiff(s, currentQuery) / 2.0 + lambda * diversityDiff(s, currentQuery), currentQuery.resultsNumber());
            s.add(currentQuery);
            if (orderedReformulations.size() == 1 && orderedReformulations.lastEntry().getValue().size() == 1) {
                break;//Optimization check
            }
        }
        algorithmTime += watch.getElapsedTimeMillis();
        info("Time to compute the reformulations using greedy algorithm: %dms", watch.getElapsedTimeMillis());
        Set<Integer> finalResults = new HashSet<>(); 
        for (ReformulatedQuery q : s) {
            finalResults.addAll(q.getResults());
        }
        coverage = finalResults.size()/(double)lattice.getRoot().resultsNumber();
        diversity = diversitySum(s);
        info("Coverage of the result set: %.2f%%", coverage * 100);
        info("Diversity of the result set: %d", diversity);
        info("Number of call to extend: %d", callToExtend);
        info("Size of the final result set: %d", s.size());
    }
        

    protected void extend(ReformulationLattice lattice, ReformulatedQuery currentQuery, LinkedList<ReformulatedQuery> queue) {
        Map<Integer, Integer>[] duplicateMappings;
        Set<Integer> tmpCandidates, tmpMapped;
        Map<Integer, Integer> tmpNodeMap;
        Set<Integer>[] duplicateCandidates, duplicateMappedEdges;
        Graph graph;
        int degree;
        int edge, adjNode;
        int[] newDFSInverseMapping, actualDFSMapping; 
        Integer adjMappedNode;
        //Iterators
        int i, j;
        Integer candidateMappedNode;
        MutableGraph candidateReformulation;
        HashMap<Integer, Integer> nodeMap;


        HashSet<Integer> candidates, mapped;
        ReformulatedQuery candidateQuery;
        ReformulatedQuery previousQuery; 
        boolean add = false; 
        
        callToExtend++;
        for (int gId : currentQuery.getResults()) {
            graph = gdb[gId];
            //Each graph may have mulitple instances per query (different paths, same query)
            duplicateMappings = currentQuery.getMappings(gId);
            duplicateCandidates = currentQuery.getCandidates(gId);
            duplicateMappedEdges = currentQuery.getMappedEdges(gId);
            //For each of the different instances
            for (i = 0; i < currentQuery.numberOfDuplicates(gId); i++) {
                mapped = (HashSet) duplicateMappedEdges[i];
                nodeMap = (HashMap) duplicateMappings[i];
                candidates = (HashSet) duplicateCandidates[i];
                checkRemove(candidates, mapped, graph);
                for (int candidate : candidates) { //For each candidate node (nodes to explore)
                    //And now expand!!! 
                    degree = graph.getDegree(candidate);
                    for (j = 0; j < degree; j++) {
                        edge = graph.getNodeEdge(candidate, j);
                        adjNode = graph.getOtherNode(edge, candidate);
                        //If it is not mapped and not visited
                        if (!mapped.contains(edge)) {
                            //Generate the new graph and check whether it is already in 
                            //the lattice. 
                            assert currentQuery.getGraph() instanceof MutableGraph;
                            candidateReformulation = (MutableGraph) currentQuery.getGraph().clone();
                            //add the new edge to the reformulation
                            // I need a node with a predefined id
                            candidateMappedNode = nodeMap.get(candidate);
                            assert candidateMappedNode != null;
                            adjMappedNode = nodeMap.get(adjNode);
                            if (adjMappedNode == null) {
                                adjMappedNode = candidateReformulation.addNode(graph.getNodeLabel(adjNode));
                            } 
                            candidateReformulation.addEdge(candidateMappedNode, adjMappedNode, graph.getEdgeLabel(edge));
                            //check if the reformulation already exists and
                            //update maps
                            candidateQuery = new ReformulatedQuery(candidateReformulation);
                            previousQuery = candidateQuery; 
                            tmpNodeMap = (HashMap<Integer, Integer>) nodeMap.clone();
                            //Reformulation already present in the lattice
                            if (lattice.containsReformulation(candidateQuery)) {
                                candidateQuery = lattice.findReformulation(candidateQuery);                                
                                //Prevent the creation of a DAG, check the father (is this correct?)
                                if (candidateQuery.hasFather(currentQuery)) {
                                    //Duplicate or not it does not matter, we create a new result
                                    tmpNodeMap.put(adjNode, adjMappedNode);
                                    add = true;
                                    actualDFSMapping = candidateQuery.getNodeMapping();
                                    //Optimization: check if we need to remap the nodes. 
                                    if (!Arrays.equals(previousQuery.getNodeMapping(), actualDFSMapping)) {
                                        newDFSInverseMapping = previousQuery.getInverseMapping();
                                        //Use dfs code mapping to map nodes to nodes (this is ensured by the optimiality of the 
                                        //dfs codes ;)
                                        for(Entry<Integer,Integer> mapping : tmpNodeMap.entrySet()) {
                                            mapping.setValue(actualDFSMapping[newDFSInverseMapping[mapping.getValue()]]);
                                        }
                                    }
                                    assert tmpNodeMap.size() == candidateQuery.getNodeCount();
                                } else {
                                    //Security check! 
                                    if (!candidateQuery.containsResult(gId)) {
                                        warn("Reformulated query %s does not contain the result %d", candidateQuery.toString(), gId);
                                    }
                                }
                            } else {
                                tmpNodeMap.put(adjNode, adjMappedNode);
                                candidateQuery.setLastAddedNode(adjMappedNode);
                                lattice.addReformulation(candidateQuery);
                                if (candidateQuery.isTree()) {
                                    treeCount++;
                                }
                                if (queue != null) {
                                    queue.add(candidateQuery);
                                }
                                queryCount++;
                                add = true;
                                if (queryCount % REFORMULATION_PRINT_COUNT == 0) {
                                    info("Inserted %d queries", queryCount);
                                }
                            }
                            //we are considering the same query result
                            if (add) {
                                currentQuery.addQuery(candidateQuery);
                                candidateQuery.addResult(gId);
                                tmpCandidates = (HashSet<Integer>) candidates.clone();
                                tmpMapped = (HashSet<Integer>) mapped.clone();
                                //Update the maps for adjacent node
                                
                                tmpCandidates.add(adjNode);
                                tmpMapped.add(edge);
                                checkRemove(tmpCandidates, tmpMapped, graph, candidate);                                

                                candidateQuery.addCandidates(tmpCandidates);
                                candidateQuery.addMapping(tmpNodeMap);
                                candidateQuery.addMappedEdges(tmpMapped);
                                add = false; 
                            }
                        }
                    }
                }
            }
        }
    } 

    @Override
    public int getNumberOfExpansions() {
        return callToExtend;
    }
    
    private void checkRemove(Set<Integer> candidates, Set<Integer> mappedEdges, Graph g) {
        Set<Integer> tmpCandidates = new HashSet<>(candidates);
        for (Integer candidate : tmpCandidates) {
            checkRemove(candidates, mappedEdges, g, candidate);
        }
    }
    
    private void checkRemove(Set<Integer> candidates, Set<Integer> mappedEdges, Graph g, Integer candidate) {
        int degree = g.getDegree(candidate);
        int edge;
        for (int i = 0; i < degree; i++) {
            edge = g.getNodeEdge(candidate, i);
            if (!mappedEdges.contains(edge)) {
                return; 
            }
        }
        candidates.remove(candidate);
    }
    
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    
    public void setCheckQueries(Set<ReformulatedQuery> checkQueries) {
        this.checkQueries = checkQueries;
    }

    @Override
    public int getNumberOfReformulations() {
        return lattice.size();
    }
}
