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

import eu.unitn.disi.db.command.exceptions.AlgorithmExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.algorithms.index.FrequencyIndex;
import eu.unitn.disi.db.gref.lattice.BucketTreeSet;
import eu.unitn.disi.db.gref.lattice.Query;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GQRIndex extends GQRPruning {
    private double minSupport = -1; 
    private String indexFile; 
    private Query query;      
    
    
    public GQRIndex() {}

    @Override
    public void compute() throws AlgorithmExecutionException {
        StopWatch watch = new StopWatch();
        FrequencyIndex index;
        callToExtend = 0;
        
        watch.start();
        //1: Load index
        info("Loading index located in: %s", indexFile);
        try (ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile))) {
            lattice = (FrequencyIndex) reader.readObject();
            index = (FrequencyIndex)lattice;
            if (minSupport < 0) {
                minSupport = index.getMinSupport();             
            } 
            if (minSupport < 0) {
                throw new AlgorithmExecutionException("Minum support not set and not found in the index");
            }
        } catch (FileNotFoundException ex) {
            throw new AlgorithmExecutionException("Index file %s not found", ex, indexFile);
        } catch (IOException | ClassNotFoundException ex) {
            throw new AlgorithmExecutionException("The index file %s is not a valid index", ex, indexFile);
        } 
        info("Time to load index: %dms", watch.getElapsedTimeMillis());
        
        //2: Answer query on index
        watch.reset();
        if (index.containsReformulation(query)) {
            ReformulatedQuery currentQuery = index.findReformulation(query);
            info("Time to answer the query: %dms", watch.getElapsedTimeMillis());
            
            Map<Integer,Integer> multiplicity; 
            queryCount = 0;
            boolean expand = false; 

            watch.start();
            //Expand the first level. 
            //List<ReformulatedQuery> orderedReformulations = new ArrayList<>(lattice.size());
            BucketTreeSet<ReformulatedQuery> orderedReformulations;
            queryScores = new HashMap<>();
            multiplicity = new HashMap<>();
            for (Integer res : currentQuery.getResults()) {
                multiplicity.put(res, 0);
            }
            s = new LinkedHashSet<>();

            //extend(index, currentQuery);
            updateScores(multiplicity);
            GQRPruning.Score scores;
            Set<ReformulatedQuery> extended = new HashSet<>();
            ReformulatedQuery father; 
            //Set<ReformulatedQuery> considered = new HashSet<>();
            orderedReformulations = new BucketTreeSet<>(); 
            for (ReformulatedQuery q : lattice.getIndex().values()) {
                orderedReformulations.add(q);
            }        
            //Add lb reasoning? 
            while (s.size() < k) {
                //Optimization based on the assumption that the first with the highest 
                //score and same ub and actual marginal will is likely to prevent
                //node expansion
                currentQuery = orderedReformulations.last();
                for (ReformulatedQuery q : orderedReformulations.getBucket(currentQuery)) {
                    scores = queryScores.get(q);
                    if (scores.upper <= scores.actual) {
                        currentQuery = q; 
                        break;
                    }
                }  
                scores = queryScores.get(currentQuery);
                expand = false; 

                if (scores.upper <= scores.actual && !s.contains(currentQuery)) {
                    s.add(currentQuery);
                    updateMultiplicity(multiplicity, currentQuery);
                    //extended = new HashSet<>();
                    updateScores(multiplicity);
                    orderedReformulations = new BucketTreeSet<>();            

                    //Optimize to update if needed. 
                    for (ReformulatedQuery q : index.getIndex().values()) {
                        orderedReformulations.add(q);
                    }
                    info("Reformulated Query %s obj marginal gain: %f, size: %d", currentQuery, scores.actual, currentQuery.resultsNumber());
                } else {
                    expand = true; 
                }
                if (expand) {
                    //info("Scores: %f, %f, %f", scores.actual, scores.upper, scores.lower);
                    if (!extended.contains(currentQuery) && currentQuery.resultsNumber() > 1) {
                        extend(index, currentQuery);
                        father = currentQuery;
                        while (father != null && father != index.getRoot()) {
                            orderedReformulations.remove(father);
                            father = father.getFather();
                        }
                        extended.add(currentQuery);
                        updateScores(multiplicity, currentQuery);
                        currentQuery.clear();
                        for (ReformulatedQuery q : currentQuery.getReformulations()) {
                            orderedReformulations.add(q);
                        }
                        father = currentQuery;
                        while (father != null && father != index.getRoot()) {
                            orderedReformulations.add(father);
                            father = father.getFather();
                        }
                    } else {
                        orderedReformulations.remove(currentQuery);
                    }
                }
                if (orderedReformulations.size() == 1) {
                    break;//Optimization check
                }
            }
            algorithmTime = watch.getElapsedTimeMillis();
            info("Total number of reformulations generated: %d", index.size());
            int results = 0; 
            for (Integer mult : multiplicity.values()) {
                if (mult > 0) {
                    results++;
                }
            }
            coverage = results/(double)lattice.getRoot().resultsNumber();
            diversity = diversitySum(s);
            info("Coverage of the result set: %.2f%%", coverage*100);
            info("Diversity of the result set: %d", diversity);
            info("Number of call to extend: %d", callToExtend);
            info("Time to compute the reformulations using greedy algorithm: %dms", algorithmTime);
            info("Size of the final result set: %d", s.size());
        } else {
            error("The index does not contain the query %s, use the normal pruning algorithm to get the results");
            throw new AlgorithmExecutionException("Index does not contain query", new NullPointerException());
        }
        
    }
    
    public void setMinSupport(double minSupport) {
        this.minSupport = minSupport;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    public void setQuery(Query query) {
        this.query = query;
    }
    
}
