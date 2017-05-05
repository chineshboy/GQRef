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

import eu.unitn.disi.db.command.algorithmic.Algorithm;
import eu.unitn.disi.db.command.algorithmic.AlgorithmInput;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.utils.Utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A reformulation algorithm can take a graph database and a k and returns the top-k
 * reformulations
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public abstract class ReformulationAlgorithm extends Algorithm {    
    
    @AlgorithmInput(
        description = "Max number of reformulations",
        mandatory = false,
        defaultValue = "10"
    )
    protected int k;
    
    protected Set<ReformulatedQuery> s;

    
    protected long algorithmTime; 
    protected double coverage; 
    protected int diversity; 

    
    public Set<ReformulatedQuery> getS() {
        return s;
    }

    public void setK(int k) {
        this.k = k;
    }
    
    public long getAlgorithmTime() {
        return algorithmTime;
    }
    
    public abstract int getNumberOfReformulations();
    
    public double getCoverage() {
        return coverage; 
    }
    
    public int getDiversity() {
        return diversity; 
    }
    
    public abstract int getNumberOfExpansions();
    
    public static int ovelap(Set<ReformulatedQuery> s) {
        int overlap = 0;
        Set<Integer> union = new HashSet<>(); 
        for (ReformulatedQuery ref : s) {
            overlap += Utils.setIntersection(union, ref.getResults());
            union.addAll(ref.getResults());
        }
        return overlap; 
    }
    
    public static int coverage(Set<ReformulatedQuery> s) {
        Set<Integer> union = new HashSet<>();
        for (ReformulatedQuery ref : s) {
            union.addAll(ref.getResults());
        }
        return union.size();
    }
    
    public static int diversity(ReformulatedQuery q1, ReformulatedQuery q2) {
        return q1.resultsNumber() + q2.resultsNumber() - 2*(Utils.setIntersection(q1.getResults(), q2.getResults()));
    }
    
    public static int diversityDiff(Set<ReformulatedQuery> s, ReformulatedQuery qPrime) {
        int div = 0, rqPrime = qPrime.resultsNumber();
        for (ReformulatedQuery q : s) {
            div += (rqPrime + q.resultsNumber() - 2 * Utils.setIntersection(qPrime.getResults(), q.getResults()));
        }
        return div;
    }

    public static int coverageDiff(Set<ReformulatedQuery> s, ReformulatedQuery qPrime) {
        Set<Integer> results = new HashSet<>();
        for (ReformulatedQuery q : s) {
            results.addAll(Utils.intersect(q.getResults(), qPrime.getResults()));
        }
        return qPrime.resultsNumber() - results.size();
    }
    
    public int diversitySum(Set<ReformulatedQuery> s) {
        List<ReformulatedQuery> sList = new ArrayList<>(s);
        int div = 0;
        for (int i = 0; i < sList.size(); i++) {
            for (int j = 0; j < sList.size(); j++) {
                if (i != j) {
                    div += diversity(sList.get(i), sList.get(j));
                }
            }
        }
        return div;
    }

}
