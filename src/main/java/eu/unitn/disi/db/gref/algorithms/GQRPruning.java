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
import eu.unitn.disi.db.gref.lattice.BucketTreeSet;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Graph Query Reformulation based on the best-first branch-and-bound algorithm 
 * presented in the paper
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GQRPruning extends GQRExact {  
    protected Map<ReformulatedQuery, Score> queryScores; 
    private int sumMultiplicity; 
    
    
    protected class Score {
        double lower; 
        double upper;
        double actual; 
        
        public Score() {}

        public Score(double lower, double upper, double actual) {
            this.lower = lower;
            this.upper = upper;
            this.actual = actual;
        }

        @Override
        public String toString() {
            return "(" + lower + "," + upper + "," + actual + ')';
        }
    }

    @Override
    public void compute() throws AlgorithmExecutionException {
        StopWatch watch = new StopWatch();
        ReformulatedQuery currentQuery;//, candidateQuery;
        Map<Integer,Integer> multiplicity; 
        queryCount = 0;
        callToExtend = 0;
        boolean expand = false; 
        double maxAct;
        ReformulatedQuery maxQuery; 
        
        
        watch.start();
        currentQuery = lattice.getRoot();
        //Expand the first level. 
        BucketTreeSet<ReformulatedQuery> orderedReformulations;
        queryScores = new HashMap<>();
        multiplicity = new HashMap<>();
        for (Integer res : currentQuery.getResults()) {
            multiplicity.put(res, 0);
        }
        s = new LinkedHashSet<>();
        
        extend(lattice, currentQuery);
        updateScores(multiplicity);
        Score scores;
        Set<ReformulatedQuery> extended = new HashSet<>();
        ReformulatedQuery father; 
        orderedReformulations = new BucketTreeSet<>(); 
        for (ReformulatedQuery q : lattice.getIndex().values()) {
            orderedReformulations.add(q);
        }        
        //Add lb reasoning? 
        while (s.size() < k && !orderedReformulations.isEmpty()) {
            //Optimization based on the assumption that the first with the highest 
            //score and same ub and actual marginal is likely to prevent
            //node expansion
            currentQuery = orderedReformulations.last();
            maxAct = queryScores.get(currentQuery).actual;
            maxQuery = currentQuery;
            for (ReformulatedQuery q : orderedReformulations.getBucket(currentQuery)) {
                scores = queryScores.get(q);
                if (scores.actual > maxAct) {
                    maxAct = scores.actual; 
                    maxQuery = q; 
                }
                if (scores.upper <= scores.actual) {
                    currentQuery = q; 
                    maxQuery = q; 
                    break;
                }
            }  
            currentQuery = maxQuery; 
            scores = queryScores.get(currentQuery);
            expand = false; 

            if (scores.upper <= scores.actual && !s.contains(currentQuery)) {
                s.add(currentQuery);
                updateMultiplicity(multiplicity, currentQuery);
                updateScores(multiplicity);
                orderedReformulations = new BucketTreeSet<>();            

                //Optimize to update if needed. 
                for (ReformulatedQuery q : lattice.getIndex().values()) {
                    orderedReformulations.add(q);
                }
                info("Reformulated Query %s obj marginal gain: %f, size: %d", currentQuery, scores.actual, currentQuery.resultsNumber());
            } else {
                expand = true; 
            }
            if (expand) {
                //info("Scores: %f, %f, %f", scores.actual, scores.upper, scores.lower);
                if (!extended.contains(currentQuery) && currentQuery.resultsNumber() > 1) {
                    extend(lattice, currentQuery);
                    father = currentQuery;
                    while (father != null && father != lattice.getRoot()) {
                        orderedReformulations.remove(father);
                        father = father.getFather();
                    }
                    extended.add(currentQuery);
                    updateScores(multiplicity, currentQuery);
                    //orderedReformulations.add(currentQuery);
                    currentQuery.clear();
                    for (ReformulatedQuery q : currentQuery.getReformulations()) {
                        orderedReformulations.add(q);
                    }
                    father = currentQuery;
                    while (father != null && father != lattice.getRoot()) {
                        orderedReformulations.add(father);
                        father = father.getFather();
                    }
//                    System.out.println(printLattice());
                } else {
                    orderedReformulations.remove(currentQuery);
                }
                //considered.add(currentQuery);
            }
        }
        algorithmTime = watch.getElapsedTimeMillis();
        info("Total number of reformulations generated: %d", lattice.size());
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
    }
    
    
    /**
     * 
     * @param multiplicity
     * @param currentQuery 
     */
    protected void updateMultiplicity(Map<Integer, Integer> multiplicity, ReformulatedQuery currentQuery) {
        Set<Integer> results = currentQuery.getResults(); 
        for (Integer res : results) {
            assert multiplicity.containsKey(res);
            multiplicity.put(res, multiplicity.get(res) + 1);
            sumMultiplicity++;
        }
    }
    
    /**
     * Update upper, lower and actual score for the whole lattice/tree. 
     * The multiplicity have been updated because we have added and extra query
     * to the result set <i>s</i> so the scores change accordingly
     * @param multiplicity The multiplicity of each result
     */
    protected void updateScores(Map<Integer, Integer> multiplicity) {
        //IT's another story ... maybe.
        //We can optimize taking into account only the queries that are affected
        drillDown(multiplicity, lattice.getRoot());
    }
    
    
    private Score drillDown(Map<Integer, Integer> multiplicity, ReformulatedQuery qPrime) {
        Score s;
        double lbmin = Double.MAX_VALUE, ubmax = 0; 
        for (ReformulatedQuery q : qPrime.getReformulations()) {
            if (q.isLeaf()) {
                s = scores(multiplicity, q);
                q.setScore(s.upper > s.actual? s.upper : s.actual);
                queryScores.put(q, s);
            } else {
                s = drillDown(multiplicity, q);
            }
            if (lbmin > s.lower) {
                lbmin = s.lower;
            } 
            if (ubmax < s.upper) {
                ubmax = s.upper;
            }
        }
        s = scores(multiplicity, qPrime);
        //s.upper = ubmax > s.actual ? ubmax : s.actual;
        s.upper = ubmax; 
        s.lower = lbmin;
        if (qPrime != lattice.getRoot()) {
            queryScores.put(qPrime, s);
            qPrime.setScore(s.upper > s.actual? s.upper : s.actual);
        }
        return s; 
    }
    
    /**
     * Update upper, lower and actual scores when we have just extended the 
     * current query. 
     * @param multiplicity The multiplicity of each result
     * @param currentQuery The query that have just been extended. 
     */
    protected void updateScores(Map<Integer, Integer> multiplicity, ReformulatedQuery currentQuery) {
        Collection<ReformulatedQuery> reformulations = currentQuery.getReformulations();
        ReformulatedQuery father;
        double maxub = 0, minlb;
        Score fatherScores, scores;
        minlb = queryScores.get(currentQuery).upper; 
        
        
        for (ReformulatedQuery q : reformulations) {
            scores = scores(multiplicity, q);
            queryScores.put(q, scores);
            if (scores.lower < minlb) {
                minlb = scores.lower;
            }
            if (scores.upper > maxub) {
                maxub = scores.upper;
            }
            q.setScore(scores.upper > scores.actual? scores.upper : scores.actual);
        }
        scores = queryScores.get(currentQuery);
        //scores.upper = maxub > scores.actual? maxub : scores.actual; 
        scores.upper = maxub; 
        scores.lower = minlb; //TO CHECK
        currentQuery.setScore(scores.upper > scores.actual? scores.upper : scores.actual);
        //roll-up (propagate up lower and upper bounds)
        father = currentQuery.getFather();
        while (father != null && father != lattice.getRoot()) {
            fatherScores = queryScores.get(father);
            if (father.getReformulations().size() == 1 || fatherScores.upper < scores.upper) {
                fatherScores.upper = scores.upper;
            } 
            if (father.getReformulations().size() == 1 || fatherScores.lower > scores.lower) {
                fatherScores.lower = scores.lower;
            }
            scores = fatherScores;
            father.setScore(scores.upper > scores.actual? scores.upper : scores.actual);
            father = father.getFather();
        }
    }
    
    
    /*
     * The upper bound is the value of the marginals if we take the elements that
     * inrease the objective function
    */
    private Score scores(Map<Integer, Integer> multiplicity, ReformulatedQuery qPrime) {
        int lbUnionSize = 0, ubUnionSize = 0, unionSize = 0;
        int ubSize = 0, lbSize = 0; 
        int lbMultiplicity = 0, ubMultiplicity = 0, actualMultiplicity = 0;
        int mult;
        float halfSSize = s.size()/2.0f;
        int rqPrime = qPrime.resultsNumber();
        double ub, lb, ac; 
        
        for (Integer res : qPrime.getResults()) {
            mult = multiplicity.get(res);
            if (mult < halfSSize) {
                ubSize++;
                if (mult > 0)
                    ubUnionSize++;
                ubMultiplicity += mult;
            } else if (mult > halfSSize) {
                lbSize++;
                if (mult > 0)
                    lbUnionSize++;
                lbMultiplicity += mult;
            }
            if (mult > 0) {
                unionSize++;
            }
            actualMultiplicity += mult;
        }
        ac = (rqPrime - unionSize)/2.0 + lambda * (sumMultiplicity + s.size() * rqPrime - 2 * actualMultiplicity);
        ub = ubUnionSize == 0? ac : (ubSize - ubUnionSize)/2.0 + lambda * (sumMultiplicity + s.size() * ubSize - 2 * ubMultiplicity);
        lb = lbUnionSize == 0? ac : (lbSize - lbUnionSize)/2.0 + lambda * (sumMultiplicity + s.size() * lbSize - 2 * lbMultiplicity);
        
        return new Score(
                lb,//LB 
                ub,//UB
                ac//OBJ FUNCT
        ); 
    }    
    
    protected void extend(ReformulationLattice lattice, ReformulatedQuery currentQuery) {
        if (currentQuery.resultsNumber() > 1) {
            super.extend(lattice, currentQuery, null);
        }
        //info("Lattice size: %d", lattice.size());
    }
    
    public static int findAndInsert(ReformulatedQuery[] queries, ReformulatedQuery toInsert) {
        ReformulatedQuery q, tmp; 
        int i, size = queries.length; 
        for (i = queries.length - 1; i > -1; i--) {
            q = queries[i];
            if (q == null) {
                size = i + 2;
                break;
            }
            if (toInsert.getScore() > q.getScore()) {
                break;
            }
        }
        q = toInsert;
        for (; i > -1; i--) {
            tmp = queries[i];
            queries[i] = q; 
            q = tmp;
        }
        return size;
    }
    
    private String printLattice() {
        StringBuilder sb = new StringBuilder();
        Set<ReformulatedQuery> visited = new HashSet<>();
        LinkedList<ReformulatedQuery> queue = new LinkedList<>();
        ReformulatedQuery currentQuery; 
        int edgeCount = lattice.getRoot().getEdgeCount();
        int level = 0;
        queue.add(lattice.getRoot());
        
        sb.append(String.format("Level[%d]\n", level));
        while (!queue.isEmpty()) {
            currentQuery = queue.poll();
            if (currentQuery.getEdgeCount() == edgeCount + 1) {
                level++;
                edgeCount++;
                sb.append(String.format("Level[%d]\n", level));
            }
            sb.append(currentQuery.isLeaf() ? "" : "*").append(s.contains(currentQuery)?"S":"").append(queryScores.get(currentQuery)).append(":" + currentQuery.resultsNumber() + ":").append(currentQuery.toString()).append("\n");
            for (ReformulatedQuery q : currentQuery.getReformulations()) {
                if (!visited.contains(q)) {
                    queue.add(q);
                    visited.add(q);
                }
            }
        }
        return sb.toString();
    } 
}


