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

import de.parmol.AbstractMiner;
import de.parmol.Settings;
import de.parmol.graph.Graph;
import de.parmol.graph.GraphFactory;
import de.parmol.graph.MutableGraph;
import de.parmol.parsers.GraphParser;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class BFSIndexBuild extends AbstractMiner {
    private FrequencyIndex index; //Create the index. 
    private float[] minFrequency; 

    public BFSIndexBuild(Settings settings) {
        super(settings);
        index = new FrequencyIndex();
    }
    
    
    private void initIndex() {
        Collection<Graph> graphs = m_graphs;
        int edge, oldEdge;
        Map<Integer,ReformulatedQuery> edgeReformulations = new HashMap<>(); 
        ReformulatedQuery query;
        GraphParser parser = m_settings.parser;
        m_settings.directedSearch = parser.directed();
        Graph graph;
        Set<Integer> candidates; 
        Map<Integer, Integer> mapping; 
        
        int nodeA, nodeB; 
        int mappedNodeA, mappedNodeB; 
        for (Graph g: graphs) {
            for (edge = 0; edge < g.getEdgeCount(); edge++) {
                query = edgeReformulations.get(edge);
                nodeA = g.getNodeA(edge);
                nodeB = g.getNodeB(edge);
                if (query == null) {    
                    graph = GraphFactory.getFactory(parser.getDesiredGraphFactoryProperties()).createGraph();
                    mappedNodeA = ((MutableGraph)graph).addNode(g.getNodeLabel(nodeA));
                    mappedNodeB = ((MutableGraph) graph).addNode(g.getNodeLabel(nodeB));
                    ((MutableGraph)graph).addEdge(
                            mappedNodeA, 
                            mappedNodeB, 
                            g.getEdgeLabel(edge));
                    query = new ReformulatedQuery(graph);
                } else {
                    graph = query.getGraph();
                    oldEdge = graph.getEdge(0); //Since it is a single edge
                    mappedNodeA = graph.getNodeA(oldEdge);
                    if (graph.getNodeLabel(mappedNodeA) == graph.getNodeLabel(nodeA)) {
                        mappedNodeB = graph.getNodeB(oldEdge);
                    } else {
                        mappedNodeA = graph.getNodeB(oldEdge);
                        mappedNodeB = graph.getNodeA(oldEdge);
                    }
                }
                query.addResult(graph.getID());
                candidates = new HashSet<>();
                candidates.add(nodeA);
                candidates.add(nodeB);
                query.addCandidates(candidates);
                query.addMappedEdges(new HashSet<>(Collections.singleton(edge)));
                query.addMapping(null);
                mapping = new HashMap<>();
                mapping.put(nodeA, mappedNodeA);
                mapping.put(nodeB, mappedNodeB);
                query.addMapping(mapping);
            }
        }
    }
    

    @Override
    protected void startRealMining() {
        //TODO: implement this. 
    }

    public FrequencyIndex getIndex() {
        return index;
    }
}
