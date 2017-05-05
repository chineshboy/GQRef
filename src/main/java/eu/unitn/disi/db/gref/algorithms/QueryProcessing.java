/*
 * The MIT License
 *
 * Copyright 2014 Davide Mottin <mottin@disi.unitn.eu>.
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
import edu.psu.chemxseer.structure.iso.FastSUCompleteEmbedding;
import eu.unitn.disi.db.command.algorithmic.Algorithm;
import eu.unitn.disi.db.command.algorithmic.AlgorithmInput;
import eu.unitn.disi.db.command.exceptions.AlgorithmExecutionException;
import eu.unitn.disi.db.gref.lattice.Query;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Finds the results of a query over a graph database
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class QueryProcessing extends Algorithm {
    @AlgorithmInput(
            description = "The query to be executed", 
            mandatory = true, 
            defaultValue = ""
    )
    private Query query; 
    @AlgorithmInput(
            description = "The graph database", 
            mandatory = true,
            defaultValue = ""
    )    
    private Graph[] gdb; 
    
    private Graph[] results; 
    private ReformulationLattice lattice;
    
    /**
     * Default constructor
     */
    public QueryProcessing() {}
    
    @Override
    public void compute() throws AlgorithmExecutionException {
        int multipleResults = 0, resultsNum = 0;
        List<Integer> resultsIds = new ArrayList<>();
        ReformulatedQuery currentQuery;
        HashSet<Integer> candidates, mapped;
        Graph graph;
        FastSUCompleteEmbedding isoProcessor;
        int mappedNode;
        int edgeId;
        int[][] maps;
        //Iterators
        int i;
        HashMap<Integer, Integer> nodeMap;
        
        lattice = new ReformulationLattice(query);
        currentQuery = lattice.getRoot();
        for (i = 0; i < gdb.length; i++) {
            graph = gdb[i];
            //System.out.println(GraphUtilities.printGraphDot(graph));
            isoProcessor = new FastSUCompleteEmbedding(query, graph);
            if (isoProcessor.issubIsomorphic()) { //We found an answer to the query
                //currentQuery.addResult(i);
                maps = isoProcessor.getMaps(); //Isomorphic mapping. qNode -> gNode
                if (maps.length > 1) {
                    multipleResults++;
                }
                for (int[] map : maps) {
                    candidates = new HashSet<>();
                    mapped = new HashSet<>();
                    nodeMap = new HashMap<>();
                    for (int col = 0; col < map.length; col++) {
                        mappedNode = map[col];
                        nodeMap.put(mappedNode, col);
                        //query.get
                        candidates.add(mappedNode);
                    }
                    for (int col = 0; col < query.getEdgeCount(); col++) {
                        edgeId = graph.getEdge(map[query.getNodeA(col)], map[query.getNodeB(col)]);
                        mapped.add(edgeId);
                        //nodeMap.put(edgeId, j);
                    }
                    currentQuery.addResult(resultsNum);
                    currentQuery.addCandidates(candidates);
                    currentQuery.addMappedEdges(mapped);
                    currentQuery.addMapping(nodeMap);
                }
                resultsIds.add(i);
                resultsNum++;
            }
        }
        results = new Graph[resultsNum];
        i = 0; 
        for (Integer res : resultsIds) {
            results[i++] = gdb[res];
        }
        info("Number of graphs with multiple answers: %d/%d", multipleResults, resultsNum);
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setGdb(Graph[] gdb) {
        this.gdb = gdb;
    }
    
    public Graph[] getResults() {
        return results;
    }

    public ReformulationLattice getLattice() {
        return lattice;
    }
}

