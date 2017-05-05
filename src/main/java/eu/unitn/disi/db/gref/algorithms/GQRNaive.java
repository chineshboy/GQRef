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
import java.util.LinkedHashSet;

/**
 * A naive implementation of Graph Query Reformulation which takes the top-k 
 * most frequent reformulated queries
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GQRNaive extends GQRExact {
    
    @Override
    public void compute() throws AlgorithmExecutionException {
        StopWatch watch = new StopWatch();
        ReformulatedQuery currentQuery;//, candidateQuery;
        queryCount = 0;
        callToExtend = 0;
        boolean expand = false; 
        
        watch.start();
        currentQuery = lattice.getRoot();
        BucketTreeSet<ReformulatedQuery> orderedReformulations;
        
        s = new LinkedHashSet<>();        
        extend(lattice, currentQuery);
        //Set<ReformulatedQuery> considered = new HashSet<>();
        orderedReformulations = new BucketTreeSet<>(); 
        for (ReformulatedQuery q : lattice.getIndex().values()) {
            q.setScore(q.resultsNumber());
            orderedReformulations.add(q);
        }        
        
        while (s.size() < k && !orderedReformulations.isEmpty()) {
            currentQuery = orderedReformulations.last();
            
            if (!s.contains(currentQuery)) {
                s.add(currentQuery);
                extend(lattice, currentQuery);
                orderedReformulations.remove(currentQuery);
                for (ReformulatedQuery child : currentQuery.getReformulations()) {
                    child.setScore(child.resultsNumber());
                    orderedReformulations.add(child);
                }
                info("Reformulated Query %s, relative frequency: %f", currentQuery, currentQuery.getScore()/lattice.getRoot().resultsNumber());
            }
        }
        algorithmTime = watch.getElapsedTimeMillis();
        diversity = diversitySum(s);
        info("Coverage of the result set: %.2f%%", ReformulationAlgorithm.coverage(s)/(double)lattice.getRoot().resultsNumber()*100.0);
        info("Number of call to extend: %d", callToExtend);
        info("Time to compute the reformulations using naive algorithm: %dms", algorithmTime);
        info("Size of the final result set: %d", s.size());
    }
    
    
    protected void extend(ReformulationLattice lattice, ReformulatedQuery currentQuery) {
        if (currentQuery.resultsNumber() > 1) {
            super.extend(lattice, currentQuery, null);
        }
        //info("Lattice size: %d", lattice.size());
    }
}
