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
import de.parmol.graph.UndirectedGraph;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Computes the graph edit distance either approximate or exact. 
 * @author riesen
 */
public class EditDistance {
    
    private EditDistance() {
    }

    public enum ApproximationType {
        HUNGARIAN("Hungarian"),
        VOLGENANT_JONKER("VJ");
        
        private String name;
        ApproximationType(String name) {
            this.name = name; 
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    
    public static double getExactEditDistance(Graph g1, Graph g2) {
        return getSuboptEditDistance(g1, g2, Integer.MAX_VALUE);
    }
    
    
    public static double getApproximateEditDistance(Graph g1, Graph g2, ApproximationType type) {
        Graph sourceGraph = g1;
        Graph targetGraph = g2;
        MatrixGenerator matrixGenerator = new MatrixGenerator();
        BipartiteMatching bipartiteMatching = new BipartiteMatching(type.toString());
        // in order to get determinant edit costs between two graphs
        if (g1.getNodeCount() < g2.getNodeCount()) {
            sourceGraph = g2; 
            targetGraph = g1; 
        }
        
          // generate the cost-matrix between the local substructures of the source and target graphs
        double[][] costMatrix = matrixGenerator.getMatrix(sourceGraph, targetGraph);
        // compute the matching using Hungarian or VolgenantJonker (defined in String matching)
        int[][] matching = bipartiteMatching.getMatching(costMatrix);
          // calculate the approximated edit-distance according to the bipartite matching 
        return getEditDistance(sourceGraph, targetGraph, matching);
    }
    
    
    /**
     *
     * @param g1 first graph
     * @param g2 second graph
     * @param matching  matching using the cost function 
     * @return the approximated edit distance between graph 
     */
    private static double getEditDistance(Graph g1, Graph g2, int[][] matching) {
        // if the edges are undirected
        // all of the edge operations have to be multiplied by 0.5
        // since all edge operations are performed twice 
        double factor = 1.0;
        if (g1 instanceof UndirectedGraph) {
            factor = 0.5;
        }
        double ed = 0.;
        // the edges of graph g1 and graph g2
        //Edge[][] edgesOfG1 = g1.getAdjacenyMatrix();
        //Edge[][] edgesOfG2 = g2.getAdjacenyMatrix();

        
        for (int i = 0; i < matching.length; i++) {
            if (matching[i][0] < g1.getNodeCount()) {
                if (matching[i][1] < g2.getNodeCount()) {
                    // i-th node substitution with node from g2 with index matching[i][1]
                    ed += CostFunction.getCost(g1.getNodeLabel(matching[i][0]), g2.getNodeLabel(matching[i][1]));
                    // edge handling when i-th node is substituted with node with index matching[i][i];
                    // iterating through all possible edges e_ij of node i
                    for (int j = 0; j < matching.length; j++) {
                        if (j < g1.getNodeCount()) {
                            int e_ij = g1.getEdge(matching[i][0], j);
                            if (matching[j][1] < g2.getNodeCount()) {
                                // node with index j is NOT deleted but subtituted
                                int e_ij_mapped = g2.getEdge(matching[i][1],matching[j][1]);
                                if (e_ij != -1) {
                                    if (e_ij_mapped != -1) {
                                        // case 1:
                                        // there is an edge between the i-th and j-th node (e_ij)
                                        // AND there is an edge between the mappings of the i-th and j-th node (e_ij_mapped)
                                        ed += factor * CostFunction.getCost(g1.getEdgeLabel(e_ij), g2.getEdgeLabel(e_ij_mapped)); // substitute the edge
                                    } else {
                                        // case 2:
                                        // there is an edge between the i-th and j-th node (e_ij)
                                        // BUT there is NO edge between the mappings of the i-th and j-th node (e_ij_mapped=null)
                                        ed += factor * CostFunction.getEdgeCosts(); // delete the edge
                                    }
                                } else {
                                    if (e_ij_mapped != -1) {
                                        // case 3:
                                        // there is NO edge between the i-th and j-th node
                                        // BUT there is an edge between the mappings
                                        ed += factor * CostFunction.getEdgeCosts(); // insert the edge
                                    }
                                }

                            } else {
                                // node with index j is deleted
                                if (e_ij != -1) {
                                    // case 4:
                                    // there is an edge between the i-th and j-th node and the j-th node is deleted
                                    ed += factor * CostFunction.getEdgeCosts(); // delete the edge
                                }
                            }
                        } else {
                            // node with index j is inserted
                            if (matching[j][1] < g2.getNodeCount()) {
                                int e_ij_mapped = g2.getEdge(matching[i][1],matching[j][1]);
                                if (e_ij_mapped != -1) {
                                    // case 5:
                                    // there is an edge between the mappings of i and j
                                    ed += factor * CostFunction.getEdgeCosts(); // insert the edge
                                }
                            }
                        }
                    }
                } else {
                    // i-th node deletion
                    ed += CostFunction.getNodeCosts();
                    // edge handling
                    for (int j = 0; j < g1.getNodeCount(); j++) {
                        int e_ij = g1.getEdge(matching[i][0],j);
                        if (e_ij != -1) {
                            // case 6:
                            // there is an edge between the i-th and j-th node
                            ed += factor * CostFunction.getEdgeCosts(); // delete the edge
                        }
                    }
                }
            } else {
                if (matching[i][1] < g2.getNodeCount()) {
                    // i-th node insertion
                    ed += CostFunction.getNodeCosts();
                    // edge handling
                    for (int j = 0; j < g2.getNodeCount(); j++) {
                        int e_ij_mapped = g2.getEdge(matching[i][1],j);
                        if (e_ij_mapped != -1) {
                            // case 7:
                            // there is an edge between the mapping of the i-th and j-th node
                            ed += factor * CostFunction.getEdgeCosts(); // insert the edge
                        }
                    }
                }
            }
        }
        return ed;
    }

    /**
     * Edit distance using beam search
     * @param g1
     * @param g2
     * @param cf
     * @param s
     * @return the exact edit distance between graph @param g1 and graph @param
     * g2 using the cost function @param cf s is the maximum number of open
     * paths used in beam-search
     */
    public static double getSuboptEditDistance(Graph g1, Graph g2, int s) {
        // list of partial edit paths (open) organized as TreeSet
        TreeSet<TreeNode> open = new TreeSet<>();

        // if the edges are undirected
        // all of the edge operations have to be multiplied by 0.5
        // since all edge operations are performed twice 
        double factor = 1.0;
        if (g1 instanceof UndirectedGraph) {
            factor = 0.5;
        }

        // each treenode represents a (partial) solution (i.e. edit path)
        // start is the first (empty) partial solution
        TreeNode start = new TreeNode(g1, g2, factor);
        open.add(start);

        // main loop of the tree search
        while (!open.isEmpty()) {
            TreeNode u = open.pollFirst();
            if (u.allNodesUsed()) {
                return u.getCost();
            }
            // generates all successors of node u in the search tree
            // and add them to open
            LinkedList<TreeNode> successors = u.generateSuccessors();
            open.addAll(successors);

            // in beam search the maximum number of open paths
            // is limited to s
            while (open.size() > s) {
                open.pollLast();
            }
        }
        // error case
        return -1;
    }

}
