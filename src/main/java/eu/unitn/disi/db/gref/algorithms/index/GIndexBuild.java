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

import de.parmol.GSpan.DFSCode;
import de.parmol.GSpan.DataBase;
import de.parmol.GSpan.GSpanEdge;
import de.parmol.Settings;
import de.parmol.graph.Graph;
import de.parmol.util.Debug;
import de.parmol.util.FrequentFragment;
import edu.psu.chemxseer.structure.parmolExtension.GindexMiner;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import java.util.Iterator;

/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GIndexBuild extends GindexMiner {
    private FrequencyIndex index; //Create the index. 
    private float[] minFrequency; 
    
    public GIndexBuild(Settings settings) {
        super(settings);
        index = new FrequencyIndex();
        minMustSelectSize = 0;
    }    

    @Override
    protected void graphSet_Projection(DataBase gs) {
        ReformulatedQuery root = index.getRoot();
        ReformulatedQuery currentQuery; 
        minFrequency = new float[m_settings.minimumClassFrequencies.length];
        for (int i = 0; i < m_settings.minimumClassFrequencies.length; i++) {
            minFrequency[i] = m_settings.minimumClassFrequencies[i] * gs.size();
            index.setMinSupport(minFrequency[i]);
        }
        for (Iterator eit = gs.frequentEdges(); eit.hasNext();) {
            GSpanEdge edge = (GSpanEdge) eit.next();
            DFSCode code = new DFSCode(edge, gs); // create DFSCode for the
            this.numberOfPatterns++;
            currentQuery = graphSetToReformulatedQuery(code.toFragment());
            index.addReformulation(root, currentQuery);
            // current edge
            long time = System.currentTimeMillis();
            // Debug.print(1, "doing seed " +
            // m_settings.serializer.serialize(code.toFragment().getFragment())
            // + " ...");
            // Debug.println(2,"");
            subgraph_Mining(code, currentQuery); // recursive search
            // eit.remove(); //shrink database
            Debug.println(1, "\tdone (" + (System.currentTimeMillis() - time)
                    + " ms)");
            if (gs.size() < m_settings.minimumClassFrequencies[0]
                    && gs.size() != 0) { // not needed
                Debug.println("remaining Graphs: " + gs.size());
                Debug.println("May not happen!!!");
                return;
            }
        }
        Debug.println(2, "remaining Graphs: " + gs.size()); //To change body of generated methods, choose Tools | Templates.
    }

    
    private ReformulatedQuery graphSetToReformulatedQuery(FrequentFragment fg) {
        ReformulatedQuery query = new ReformulatedQuery(fg.getFragment());
        for (Graph g : fg.getSupportedGraphs()) {
            query.addResult(g.getID());
        }
        return query;
    }
    
    

    protected float[] subgraph_Mining(DFSCode code, ReformulatedQuery currentQuery) {
        ReformulatedQuery child; 
        if (!code.isMin()) {
            Debug.println(1, code.toString(m_settings.serializer) + " not min");
            m_settings.stats.duplicateFragments++;
            return empty;
        }
        float[] max = empty;

        float[] my = code.getFrequencies();
        
        Debug.println(1,"   found graph " + code.toString(m_settings.serializer));
        
        if (code.getSubgraph().getEdgeCount() < m_settings.maximumFragmentSize) {
            Iterator it = code.childIterator(false, false);
            for (; it.hasNext();) {
                DFSCode next = (DFSCode) it.next();
                this.numberOfPatterns++;
                // This is edited by Dayu, affect efficiency
                // Calculate new minimum Class Frequency
                // Decreasing frequency by levelß
//                float[] dayuFrequency  = new float[m_settings.minimumClassFrequencies.length];
//                for (int i = 0; i < dayuFrequency.length; i++) {
//                    dayuFrequency[i] = (float) 
//                            java.lang.Math
//                            .sqrt((double) next.getSubgraph().getEdgeCount()
//                            / (double) m_settings.maximumFragmentSize)
//                            * m_settings.minimumClassFrequencies[i];
//                }
                if (next.getSubgraph().getEdgeCount() < this.minMustSelectSize
                        || (next.isFrequent(minFrequency))) { //Its a frequent child
                    child = graphSetToReformulatedQuery(next.toFragment());
                    index.addReformulation(currentQuery, child);
                    float[] a = subgraph_Mining(next, child);
                    max = getMax(max, a);
                } else {
                    Debug.println(1, "graph " + code.toString(m_settings.serializer) + "is not frequent");
                }
            }
        } else {
            Debug.println(1,"Code " + code.toString(m_settings.serializer) + " discharded because too big");
        }
        //Closure
        if ((/*!m_settings.closedFragmentsOnly || */max == empty || unequal(my, max))
                && m_settings.checkReportingConstraints(code.getSubgraph(),
                code.getFrequencies())) {
            //code.toFragment().
            m_frequentSubgraphs.add(code.toFragment());
        } else {
            m_settings.stats.earlyFilteredNonClosedFragments++;
        }
        return my;
    }

    public FrequencyIndex getIndex() {
        return index;
    }
    
    
    
}
