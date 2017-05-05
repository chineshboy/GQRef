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
package eu.unitn.disi.db.gref.algorithms.ged;

import de.parmol.graph.Graph;
import java.util.LinkedList;
/**
 * @author riesen
 *
 */
public class TreeNode implements Comparable<TreeNode> {

    /**
     * nodes of g1 are mapped to...
     */
    private int[] matching;

    /**
     * nodes of g2 are mapped to...
     */
    private int[] inverseMatching;

    /**
     * the current cost of this partial solution
     */
    private double cost;

    /**
     * the adjacency-matrix of the graphs
     */
//    private Edge[][] a1;
//    private Edge[][] a2;

    /**
     * the original graphs
     */
    private Graph originalGraph1;
    private Graph originalGraph2;

    /**
     * the graphs where the processed nodes are removed
     */
    //private Graph unusedNodes1;
    //private Graph unusedNodes2;

    private LinkedList<Integer> unusedNodes1;
    private LinkedList<Integer> unusedNodes2;
    
    /**
     * weighting factor for edge operations = 0.5 if undirected edges are used
     * (1.0 otherwise)
     *
     */
    private double factor;

    /**
     * constructor for the initial empty solution
     *
     * @param g2
     * @param g1
     * @param cf
     * @param factor
     */
    public TreeNode(Graph g1, Graph g2, double factor) {
        this.originalGraph1 = g1;
        this.originalGraph2 = g2;
        this.cost = 0;
        this.matching = new int[g1.getNodeCount()];
        this.inverseMatching = new int[g2.getNodeCount()];
        this.unusedNodes1 = new LinkedList<>();
        this.unusedNodes2 = new LinkedList<>();
        for (int i = 0; i < this.matching.length; i++) {
            unusedNodes1.add(i);
            this.matching[i] = -1;
        }
        for (int i = 0; i < this.inverseMatching.length; i++) {
            unusedNodes2.add(i);
            this.inverseMatching[i] = -1;
        }
        this.factor = factor;

    }

    /**
     * copy constructor in order to generate successors of treenode @param o
     */
    public TreeNode(TreeNode o) {
        this.unusedNodes1 = (LinkedList<Integer>) o.unusedNodes1.clone();
        this.unusedNodes2 = (LinkedList<Integer>) o.unusedNodes2.clone();
        this.cost = o.getCost();
        this.matching = o.matching.clone();
        this.inverseMatching = o.inverseMatching.clone();
        this.originalGraph1 = o.originalGraph1;
        this.originalGraph2 = o.originalGraph2;
        this.factor = o.factor;

    }

    /**
     * @return a list of successsors of this treenode (extended solutions to
     * *this* solution)
     */
    public LinkedList<TreeNode> generateSuccessors() {
        // list with successors
        LinkedList<TreeNode> successors = new LinkedList<>();

        // all nodes of g2 are processed, the remaining nodes of g1 are deleted
        if (this.unusedNodes2.isEmpty()) {
            TreeNode tn = new TreeNode(this);
            int n = tn.unusedNodes1.size();
            int e = 0;
            //Iterator<Node> nodeIter = tn.unusedNodes1.iterator();
            for (int node : unusedNodes1) {
                //int i = tn.originalGraph1.indexOf(node);
                // find number of edges adjacent to node i
                e += this.getNumberOfAdjacentEdges(tn.matching, originalGraph1, node);
                tn.matching[node] = -2; // -2 = deletion
            }

            tn.addCost(n * CostFunction.getNodeCosts());
            tn.addCost(e * CostFunction.getEdgeCosts() * factor);
            tn.unusedNodes1.clear();
            successors.add(tn);

        } else { // there are still nodes in g2 but no nodes in g1, the nodes of
            // g2 are inserted
            if (this.unusedNodes1.isEmpty()) {
                TreeNode tn = new TreeNode(this);
                int n = tn.unusedNodes2.size();
                int e = 0;
                for (int node : unusedNodes2) {
                    //int i = tn.originalGraph2.indexOf(node);
                    // find number of edges adjacent to node i
                    e += this.getNumberOfAdjacentEdges(tn.inverseMatching, originalGraph2, node);
                    tn.inverseMatching[node] = -2; // -2 = insertion
                }
                tn.addCost(n * CostFunction.getNodeCosts());
                tn.addCost(e * CostFunction.getEdgeCosts() * factor);
                tn.unusedNodes2.clear();
                successors.add(tn);

            } else { // there are nodes in both g1 and g2
                for (int i = 0; i < this.unusedNodes2.size(); i++) {
                    TreeNode tn = new TreeNode(this);
                    int start = tn.unusedNodes1.remove();
                    int end = tn.unusedNodes2.remove(i);
                    tn.addCost(CostFunction.getCost(originalGraph1.getNodeLabel(start), originalGraph2.getNodeLabel(end)));
                    tn.matching[start] = end;
                    tn.inverseMatching[end] = start;
                    // edge processing
                    this.processEdges(tn, start, end);
                    successors.add(tn);
                }
                // deletion of a node from g_1 is also a valid successor
                TreeNode tn = new TreeNode(this);
                int deleted = tn.unusedNodes1.remove();
                tn.matching[deleted] = -2; // deletion
                tn.addCost(CostFunction.getNodeCosts());
                // find number of edges adjacent to node i
                int e = this.getNumberOfAdjacentEdges(tn.matching, originalGraph1, deleted);
                tn.addCost(CostFunction.getEdgeCosts() * e
                        * factor);
                successors.add(tn);
            }
        }
        return successors;
    }

    /**
     * TODO needs massive refactoring!
     *
     * @param tn
     * @param start
     * @param startIndex
     * @param end
     * @param endIndex
     */
    private void processEdges(TreeNode tn, int n1 /*startIndex*/, int n2 /*endIndex*/) {
        int edge1, edge2;
        for (int e = 0; e < originalGraph1.getNodeCount(); e++) {
            edge1 = tn.originalGraph1.getEdge(n1,e);
            if (edge1 != -1) { // there is an edge between start and start2
                int start2Index = e;
                if (tn.matching[start2Index] != -1) { // other end has been handled
                    int end2Index = tn.matching[start2Index];
                    if (end2Index >= 0) {
                        edge2 = tn.originalGraph2.getEdge(n2,end2Index);
                        if (edge2 != -1) {
                            tn.addCost(CostFunction.getCost(tn.originalGraph1.getEdgeLabel(edge1),tn.originalGraph2.getEdgeLabel(edge2))
                                    * factor);
                        } else {
                            tn.addCost(CostFunction.getEdgeCosts() * factor);
                        }
                    } else { // deletion
                        tn.addCost(CostFunction.getEdgeCosts() * factor);
                    }
                }
            } else { // there is no edge between start and start2
                int start2Index = e;
                if (tn.matching[start2Index] != -1) { // other "end" has been handled
                    int end2Index = tn.matching[start2Index];
                    if (end2Index >= 0) {
                        if (tn.originalGraph2.getEdge(n2,end2Index) != -1) {
                            tn.addCost(CostFunction.getEdgeCosts() * factor);
                        }
                    }
                }
            }
            // DUPLICATED CODE REFACTOR
            edge1 = tn.originalGraph1.getEdge(e,n1);
            if (edge1 != -1) {
                int start2Index = e;
                if (tn.matching[start2Index] != -1) { // other end has been handled
                    int end2Index = tn.matching[start2Index];
                    if (end2Index >= 0) {
                        edge2 = tn.originalGraph2.getEdge(n2,end2Index);
                        if (edge2 != -1) {
                            tn.addCost(CostFunction.getCost(tn.originalGraph1.getEdgeLabel(edge1), tn.originalGraph2.getEdgeLabel(edge2))
                                    * factor);
                        } else {
                            tn.addCost(CostFunction.getEdgeCosts() * factor);
                        }
                    } else { // deletion
                        tn.addCost(CostFunction.getEdgeCosts() * factor);
                    }
                }
            } else {
                int start2Index = e;
                if (tn.matching[start2Index] != -1) { // other "end" has been handled
                    int end2Index = tn.matching[start2Index];
                    if (end2Index >= 0) {
                        if (tn.originalGraph2.getEdge(n2,end2Index) != -1) {
                            tn.addCost(CostFunction.getEdgeCosts() * factor);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return number of adjacent edges of node with index @param i NOTE: only
     * edges (i,j) are counted if j-th node hae been processed (deleted or
     * substituted)
     */
    private int getNumberOfAdjacentEdges(int[] m, Graph g, int i) {
        int e = 0;
        for (int j = 0; j < g.getNodeCount(); j++) {
            if (m[j] != -1) { // count edges only if other end has been processed
                if (g.getEdge(i,j) != -1) {
                    e += 1;
                }
                if (g.getEdge(j,i) != -1) {
                    e += 1;
                }
            }
        }
        return e;
    }

    /**
     * adds @param c to the current solution cost
     */
    private void addCost(double c) {
        this.cost += c;
    }

    /**
     * tree nodes are ordererd according to their past cost in the open list:
     * NOTE THAT CURRENTLY NO HEURISTIC IS IMPLEMENTED FOR ESTIMATING THE FUTURE
     * COSTS
     */
    public int compareTo(TreeNode other) {
        if ((this.getCost() - other.getCost()) < 0) {
            return -1;
        }
        if ((this.getCost() - other.getCost()) > 0) {
            return 1;
        }
        // we implement the open list as a TreeSet which does not allow 
        // two equal objects. That is, if two treenodes have equal cost, only one 
        // of them would be added to open, which would not be desirable
        return 1;
    }

    /**
     * @return true if all nodes are used in the current solution
     */
    public boolean allNodesUsed() {
        if (unusedNodes1.isEmpty() && unusedNodes2.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * some getters and setters
     */
//    public Graph getUnusedNodes1() {
//        return unusedNodes1;
//    }
//
//    public Graph getUnusedNodes2() {
//        return unusedNodes2;
//    }

    public double getCost() {
        return this.cost;
    }

}
