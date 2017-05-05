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

package eu.unitn.disi.db.gref.lattice;

import de.parmol.graph.Graph;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a reformulated query, which contains a {@link Set} of 
 * ReformulatedQueries
 * The list constructs the {@link ReformulationLattice} which is used by the various 
 * algorithms. 
 * 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class ReformulatedQuery extends Query implements Comparable<ReformulatedQuery>, Serializable {
    ReformulatedQuery father;  
    Set<ReformulatedQuery> reformulations;
    private Map<Integer,List<Integer>> resultsToIndexes;
    private transient int lastIndex;
    private transient double score; 
    
    //Maybe move to maps instead of arrays. 
    private List<Set<Integer>> candidateNodes;
    private List<Set<Integer>> mappedEdges;
    private List<Map<Integer,Integer>> mappings;
    
    int lastAddedNode = -1;
    
    public ReformulatedQuery(Graph graph) {
        super(graph);
        reformulations = new HashSet<>();
        father = null;
        resultsToIndexes = new HashMap<>();
        mappings = new ArrayList<>();
        candidateNodes = new ArrayList<>();
        mappedEdges = new ArrayList<>();
        lastIndex = 0;
//        System.out.println(toString());
    }
    
    
    public boolean addQuery(ReformulatedQuery query)
            throws NullPointerException
    {
        query.father = this;
        return reformulations.add(query);
    }
    
    public boolean hasFather(ReformulatedQuery query) {
        return father == query;
    }
    
    public boolean removeQuery(ReformulatedQuery query)
            throws NullPointerException
    {
        return reformulations.remove(query);
    }
    
    public boolean containsQuery(ReformulatedQuery query)
            throws NullPointerException
    {
        return reformulations.contains(query);
    }

    public boolean containsMapping(int gId, Map<Integer,Integer> mappings) {
        Map<Integer, Integer>[] maps = getMappings(gId);
        if (maps != null) {
            Set<Integer> keySet = mappings.keySet(); 
            for (Map<Integer, Integer> map : maps) {
                if (map.keySet().equals(keySet)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isLeaf()
    {
        return reformulations.isEmpty();
    }

    public Set<ReformulatedQuery> getReformulations() {
        return reformulations;
    }

    public int getLastAddedNode() {
        return lastAddedNode;
    }

    public void setLastAddedNode(int lastAddedNode) {
        this.lastAddedNode = lastAddedNode;
    }

    @Override
    public boolean addResult(int gId) throws NullPointerException {
        List<Integer> duplicates = resultsToIndexes.get(gId);
        if (duplicates == null) {
            duplicates = new ArrayList<>();
        }
        duplicates.add(lastIndex++);
        resultsToIndexes.put(gId, duplicates);
        return super.addResult(gId); 
    }

    public boolean addResults(Collection<Integer> results) {
        boolean success = true;
        for (Integer res : results) {
            success = success && addResult(res);
        }
        return success; 
    }
    
    public boolean addMapping(Map<Integer, Integer> mapping) 
            throws IndexOutOfBoundsException
    {
        if (mappings.size() + 1 != lastIndex) {
            throw new IndexOutOfBoundsException("The mapping you are adding does not correspond to any result");
        }
        return mappings.add(mapping);
    }

    public Map<Integer, Integer>[] getMappings(int gId) 
            throws IndexOutOfBoundsException 
    {
        List<Integer> indexes = resultsToIndexes.get(gId);
        Map<Integer, Integer>[] maps = null;
        if (indexes != null) {
            maps = new Map[indexes.size()];
            int i = 0;
            for (Integer index : indexes) {
                maps[i++] = mappings.get(index);
            }
        }
        return maps;
    }

//    public boolean addCandidate(int gId, int candidate) 
//            throws IndexOutOfBoundsException
//    {
//        return candidateNodes.get(resultsToIndexes.get(gId)).add(candidate);
//    }
    
    public boolean addCandidates(Set<Integer> candidates) 
            throws IndexOutOfBoundsException
    {
        if (candidateNodes.size() + 1 != lastIndex) {
            throw new IndexOutOfBoundsException("The candidate you are adding does not correspond to any result");
        }
        return candidateNodes.add(candidates);
    }

    public Set<Integer>[] getCandidates(int gId) 
            throws IndexOutOfBoundsException
    {   
        List<Integer> indexes = resultsToIndexes.get(gId);
        Set<Integer>[] cands = null;
        if (indexes != null) {
            cands = new Set[indexes.size()];
            int i = 0;
            for (Integer index : indexes) {
                cands[i++] = candidateNodes.get(index);
            }
        }
        return cands;
    }

    public boolean addMappedEdges(Set<Integer> mapped) 
            throws IndexOutOfBoundsException
    {   
        if (mappedEdges.size() + 1 != lastIndex) {
            throw new IndexOutOfBoundsException("The mapped edges you are adding does not correspond to any result");
        }

        return mappedEdges.add(mapped);
    }

    public Set<Integer>[] getMappedEdges(int gId) 
            throws IndexOutOfBoundsException
    {        
        List<Integer> indexes = resultsToIndexes.get(gId);
        Set<Integer>[] maps = null;
        if (indexes != null) {
            maps = new Set[indexes.size()];
            int i = 0;
            for (Integer index : indexes) {
                maps[i++] = mappedEdges.get(index);
            }
        }
        return maps;
    } 
    
    public int numberOfDuplicates(int gId) throws NullPointerException
    {
        return resultsToIndexes.get(gId).size();
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
    
    @Override
    public int compareTo(ReformulatedQuery o) {
        if (this.score < o.score) {
            return -1;
        } else if (this.score > o.score) {
            return 1;
        }
        return 0;
    }

    public ReformulatedQuery getFather() {
        return father;
    }
    
    public void clear() {
        resultsToIndexes = new HashMap<>();
        mappings = new ArrayList<>();
        candidateNodes = new ArrayList<>();
        mappedEdges = new ArrayList<>();
        lastIndex = 0;
    }
}
