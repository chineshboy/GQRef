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

package eu.unitn.disi.db.gref.algorithms.index;

import de.parmol.graph.Graph;
import edu.psu.chemxseer.structure.factory.MyFactory;
import eu.unitn.disi.db.gref.lattice.Query;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GIndex is a reformulation lattice with an inverted index on graphs. 
 */
public class FrequencyIndex extends ReformulationLattice implements Serializable {
    private float minSupport; 
    private Map<Integer,Set<ReformulatedQuery>> invertedIndex;      
    private static final Graph EMPTY = MyFactory.getDFSCoder().parse("<0 -1 -1 -1 -1>", MyFactory.getGraphFactory());
    
    private FrequencyIndex(Query query) {
        super(query);
    }
    
    public FrequencyIndex() {
        this(new ReformulatedQuery(EMPTY));
        invertedIndex = new HashMap<>();
    }

    @Override
    public void addReformulation(ReformulatedQuery query) throws NullPointerException {
        super.addReformulation(query);
        Set<ReformulatedQuery> reformulations; 
        for (Integer res : query.getResults()) { //Update the inverted index
            reformulations = invertedIndex.get(res);
            if (reformulations == null) {
                reformulations = new HashSet<>();
            }
            reformulations.add(query);
            invertedIndex.put(res, reformulations);
        }
    }
    
    public void addReformulation(ReformulatedQuery father, ReformulatedQuery query) {
        father.addQuery(query);
        addReformulation(query);
    }

    public float getMinSupport() {
        return minSupport;
    }

    public void setMinSupport(float minSupport) {
        this.minSupport = minSupport;
    }
    
}
