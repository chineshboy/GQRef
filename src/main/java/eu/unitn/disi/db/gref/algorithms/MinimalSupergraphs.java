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
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.iso.CanonicalDFS;
import edu.psu.chemxseer.structure.postings.Interface.IGraphDatabase;
import edu.psu.chemxseer.structure.subsearch.Interfaces.SearchStatus;
import edu.psu.chemxseer.structure.subsearch.Lindex.LindexTerm;
import edu.psu.chemxseer.structure.subsearch.Lindex.SubSearch_LindexSimple;
import edu.psu.chemxseer.structure.subsearch.Lindex.SubSearch_LindexSimpleBuilder;
import eu.unitn.disi.db.command.exceptions.AlgorithmExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.utils.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Algorithm based on LIndex + Discriminant Frequent Graph (DFG) features. 
 * The algorithm returns the set of minimal supergraphs for a query q as reformulations
 * If the set is &lt;k then it returns the children of minimal supergraphs, if 
 * present. 
 * 
 * Dayu Yuan, Prasenjit Mitra: Lindex: a lattice-based index for graph databases. 
 * VLDB J. 22(2): 229-252 (2013)
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class MinimalSupergraphs extends IndexAlgorithm {
    private int callToExtend; 
    
    
    public MinimalSupergraphs(IGraphDatabase gdb) {
        super(gdb);
    }

    @Override
    public int getNumberOfReformulations() {
        return s.size(); //Maybe imprecise
    }

    @Override
    public int getNumberOfExpansions() {
        return callToExtend;
    }
    
    @Override
    public void compute() throws AlgorithmExecutionException {
        SubSearch_LindexSimpleBuilder builder = new SubSearch_LindexSimpleBuilder();
        SubSearch_LindexSimple index;
        List<Integer> maxSubs, minSups, reformulations, realSubgraphs;
        CanonicalDFS dfsParser = MyFactory.getDFSCoder();
        Set<Integer> queryResults, refResults, intersection; 
        Graph refQueryGraph; 
        StopWatch watch = new StopWatch(); 
        ReformulatedQuery refQuery;
        int[] res;
        callToExtend = 0; 
        s = new HashSet<>(); 
        
        try {
            watch.start();
            index = builder.loadIndex(gdb, indexPath, gdb.getParser(), false);
            info("Time to load index: %dms", watch.getElapsedTimeMillis());
            watch.reset();
            maxSubs = index.indexSearcher.maxSubgraphs(query, new SearchStatus());            
            info("Maximal Subgraphs: %s", maxSubs.toString());
            
            realSubgraphs = new ArrayList<>(); 
            for (Integer maxSub : maxSubs) {
                if (maxSub != -1) {
                    realSubgraphs.add(maxSub);
                } else {
                    debug("Input query corresponds to a feature");
                }
            }
            callToExtend++;
            //Execute the query for each of the found reformulations
            minSups = index.indexSearcher.minimalSupergraphs(query, new SearchStatus(), realSubgraphs);
            info("Minimial Supergraphs: %s", minSups.toString());
            
            res = index.getAnswerIDs(query)[0];
            queryResults = Utils.intArrayToSet(res);
            if (!minSups.isEmpty()) {
                if (minSups.size() >= k) {
                    reformulations = minSups.subList(0, k);
                } else {
                    reformulations = new ArrayList<>(minSups); 
                    LinkedList<Integer> queue = new LinkedList<>(reformulations);
                    LindexTerm[] children; 
                    Integer ref; 
                    //Find sons. 
                    while(!queue.isEmpty() && reformulations.size() < k) {
                        ref = queue.poll();
                        children = index.indexSearcher.indexTerms[ref].getChildren();
                        callToExtend++;
                        //TODO: add condition on k
                        for (int i = 0; reformulations.size() < k && i < children.length; i++) {
                            reformulations.add(children[i].getId());
                            queue.add(children[i].getId());
                        }
                    }
                }
                if (reformulations.size() != k) {
                    warn("Found less than %d/%d reformulations", reformulations.size(), k);
                }
                int[][] dfsCode; 
                for (Integer ref : reformulations) {
                    if (index.indexSearcher.indexTerms[ref].getExtension() == null) {
                        error("Houston we got a problem!");
                    }
                    dfsCode = extractFeatureDFSCode(index.indexSearcher.indexTerms[ref]);
                    //info(Utils.matrixToString(dfsCode));
                    refQueryGraph = dfsParser.parse(dfsCode, MyFactory.getGraphFactory());
                    refResults = Utils.intArrayToSet(index.getAnswerIDs(refQueryGraph)[0]);
                    intersection = Utils.intersect(queryResults, refResults);
                    if (refResults.size() > intersection.size()) {
                        warn("Graph %s is not a supegraph of the query", dfsParser.serialize(refQueryGraph));
                    }
                    refQuery = new ReformulatedQuery(refQueryGraph);
                    refQuery.addResults(intersection);
                    s.add(refQuery);
                }
            }
            algorithmTime = watch.getElapsedTimeMillis();
            info("Time to compute %d reformulations using index algorithm: %dms", k, watch.getElapsedTimeMillis());
            Set<Integer> finalResults = new HashSet<>(); 
            for (ReformulatedQuery q : s) {
                finalResults.addAll(q.getResults());
            }
            coverage = finalResults.size()/(double)queryResults.size();
            diversity = diversitySum(s);
            info("Coverage of the result set: %.2f%%", coverage*100);
            info("Diversity of the result set: %d", diversity);
            info("Number of call to extend: %d", callToExtend);
            info("Size of the final result set: %d", s.size());

        } catch (IOException ex) {
            Logger.getLogger(MinimalSupergraphs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private int[][] extractFeatureDFSCode(LindexTerm feature) {
        LindexTerm next = feature; 
        List<int[]> dfsCode = new ArrayList<>();
        int[][] extensions; 
        while (next != null && next.getId() != -1) {
            extensions = next.getExtension();
            dfsCode.addAll(Arrays.asList(extensions));
            next = next.getParent();
        }
        Collections.reverse(dfsCode);
        return dfsCode.toArray(new int[dfsCode.size()][]);
    }
    
    
}
