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
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.iso.CanonicalDFS;
import eu.unitn.disi.db.command.util.LoggableObject;
import eu.unitn.disi.db.gref.utils.GraphUtilities;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Wrapper class for graphs, it encapsulate a graph and it computes the code. 
 * This class can be used in hashmaps, since it returns an hash and a unique
 * representation. 
 * 
 * The code is the DFS code [1] of the graph. 
 * This graph is immutable, the only way to change it is to create a copy. 
 * 
 * gSpan: Graph-Based Substructure Pattern Mining, by X. Yan and J. Han. 
 * Proc. 2002 of Int. Conf. on Data Mining (ICDM'02)
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class CodedGraph extends LoggableObject implements Graph, Serializable {
    protected final Graph graph; 
    private int[][] code; 
    /*
     * DFS code node to graph node
    */
    private final int[] nodeMapping; 
    /*
     * Graph node to DFS code node
    */
    private final int[] inverseMapping;
    private int hashCode; 
    private transient static final CanonicalDFS CODER = MyFactory.getDFSCoder();
    private final boolean tree; 
   
    
    public CodedGraph(Graph graph) {
        this.graph = graph;
        code = CODER.serializeToArray(this.graph);
        nodeMapping = CODER.getNodeMapping();
        if (nodeMapping != null) {
            inverseMapping = new int[nodeMapping.length];
            for (int i = 0; i < nodeMapping.length; i++) {
                inverseMapping[nodeMapping[i]] = i; 
            }
        } else {
            inverseMapping = null;
        }
        tree = GraphUtilities.isTree(graph);
        hashCode = 7;
        hashCode = 11 * hashCode + Arrays.deepHashCode(this.code);
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public int getNodeCount() {
        return graph.getNodeCount();
    }

    @Override
    public int getEdgeCount() {
        return graph.getEdgeCount();
    }

    @Override
    public String getName() {
        return graph.getName();
    }

    @Override
    public int getID() {
        return graph.getID();
    }

    @Override
    public int getEdge(int nodeA, int nodeB) {
        return graph.getEdge(nodeA, nodeB);
    }

    @Override
    public int getEdge(int index) {
        return graph.getEdge(index);
    }

    @Override
    public int getNode(int index) {
        return graph.getNode(index);
    }

    @Override
    public int getNodeLabel(int node) {
        return graph.getNodeLabel(node);
    }

    @Override
    public int getEdgeLabel(int edge) {
        return graph.getEdgeLabel(edge);
    }

    @Override
    public int getDegree(int node) {
        return graph.getDegree(node);
    }

    @Override
    public int getNodeEdge(int node, int index) {
        return graph.getNodeEdge(node, index);
    }

    @Override
    public int getNodeIndex(int node) {
        return graph.getNodeIndex(node);
    }

    @Override
    public int getEdgeIndex(int edge) {
        return graph.getEdgeIndex(edge);
    }

    @Override
    public int getNodeA(int edge) {
        return graph.getNodeA(edge);
    }

    @Override
    public int getNodeB(int edge) {
        return graph.getNodeB(edge);
    }

    @Override
    public int getOtherNode(int edge, int node) {
        return graph.getOtherNode(edge, node);
    }

    @Override
    public boolean isBridge(int edge) {
        return graph.isBridge(edge);
    }

    @Override
    public void setNodeObject(int node, Object o) {
        graph.setNodeObject(node, o);
    }

    @Override
    public Object getNodeObject(int node) {
        return graph.getNodeObject(node);
    }

    @Override
    public void setEdgeObject(int edge, Object o) {
        setEdgeObject(edge, o);
    }

    @Override
    public Object getEdgeObject(int edge) {
        return graph.getEdgeObject(edge);
    }

    @Override
    public void saveMemory() {
        graph.saveMemory();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CodedGraph other = (CodedGraph) obj;
        return Arrays.deepEquals(other.code, this.code);
    }

    @Override
    public String toString() {
        return CODER.writeArrayToText(code);
    }
    
    
    @Override
    public Object clone(){
        CodedGraph g = new CodedGraph((Graph) graph.clone());
        g.code = this.code;
        g.hashCode = this.hashCode;
        return g;
    }
    
    public void recomputeHash() {
        code = CODER.serializeToArray(this.graph);
        hashCode = 7;
        hashCode = 11 * hashCode + Arrays.deepHashCode(this.code);
    }
    
    public String serialize(GraphParser serializer) {
        return serializer.serialize(graph);
    }
    
    public void writeToFile(String filename, GraphParser serializer) throws IOException {
        try (BufferedWriter br = new BufferedWriter(new FileWriter(filename))) {
            br.write(serialize(serializer));
        } 
    }

    public int[] getNodeMapping() {
        return nodeMapping;
    }

    public int[] getInverseMapping() {
        return inverseMapping;
    }
    
    public boolean isTree() {
        return tree;
    }
}
