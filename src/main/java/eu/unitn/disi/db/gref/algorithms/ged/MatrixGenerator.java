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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author riesen
 *
 */
public class MatrixGenerator {

    /**
     * the cource and target graph whereon the cost matrix is built
     */
    private Graph source, target;

    /**
     * the cost function actually employed
     */
    //private CostFunction cf;

    /**
     * the matching algorithm for recursive edge matchings (hungarian is used in
     * any case!)
     */
    private HungarianAlgorithm ha;

    /**
     * whether or not the cost matrix is logged on the console
     */
    //private int outputCostMatrix;

    /**
     * the decimal format for the distances found
     */
    private DecimalFormat decFormat;

    /**
     * constructs a MatrixGenerator
     *
     * @param costFunction
     * @param outputCostMatrix
     */
    public MatrixGenerator() {
        this.ha = new HungarianAlgorithm();
        this.decFormat = (DecimalFormat) NumberFormat
                .getInstance(Locale.ENGLISH);
        this.decFormat.applyPattern("0.00000");
    }

    /**
     * @return the cost matrix for two graphs @param sourceGraph and @param
     * targetGraph | | | c_i,j | del |_________|______ | | | ins |	0 | |
     *
     */
    public double[][] getMatrix(Graph sourceGraph, Graph targetGraph) {
        this.source = sourceGraph;
        this.target = targetGraph;
        int sSize = sourceGraph.getNodeCount();
        int tSize = targetGraph.getNodeCount();
        int dim = sSize + tSize;
        double[][] matrix = new double[dim][dim];
        double[][] edgeMatrix;
        //int u;
        //int v;
        
        for (int i = 0; i < sSize; i++) {
            //u = this.source.getNode(i);
            for (int j = 0; j < tSize; j++) {
                //v = this.target.getNode(j);
                double costs = CostFunction.getCost(source.getNodeLabel(i), target.getNodeLabel(j));
                // adjacency information is added to the node costs
                edgeMatrix = this.getEdgeMatrix(i, j);
                costs += this.ha.hgAlgorithmOnlyCost(edgeMatrix);
                matrix[i][j] = costs;
            }
        }
        for (int i = sSize; i < dim; i++) {
            for (int j = 0; j < tSize; j++) {
                if ((i - sSize) == j) {
                    //v = (Node) this.target.get(j);
                    double costs = CostFunction.getNodeCosts();
                    double f = target.getDegree(j);//v.getEdges().size();
                    costs += (f * CostFunction.getEdgeCosts());
                    matrix[i][j] = costs;
                } else {
                    matrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = 0; i < sSize; i++) {
            //u = (Node) this.source.get(i);
            for (int j = tSize; j < dim; j++) {
                if ((j - tSize) == i) {
                    double costs = CostFunction.getNodeCosts();;
                    double f = source.getDegree(i);//u.getEdges().size();
                    costs += (f * CostFunction.getEdgeCosts());
                    matrix[i][j] = costs;
                } else {
                    matrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = sSize; i < dim; i++) {
            for (int j = tSize; j < dim; j++) {
                matrix[i][j] = 0.0;
            }
        }
//        if (this.outputCostMatrix == 1) {
//            System.out.println("\nThe Cost Matrix:");
//            for (int k = 0; k < matrix.length; k++) {
//                for (int l = 0; l < matrix[0].length; l++) {
//                    if (matrix[k][l] < Double.POSITIVE_INFINITY) {
//                        System.out.print(decFormat.format(matrix[k][l]) + "\t");
//
//                    } else {
//                        System.out.print("infty\t");
//                    }
//
//                }
//                System.out.println();
//            }
//        }
        return matrix;
    }

    /**
     * @return the cost matrix for the edge operations between the nodes @param
     * u
     * @param v
     */
    private double[][] getEdgeMatrix(int u, int v) {
        int uSize = source.getDegree(u);
        int vSize = target.getDegree(v);
        int dim = uSize + vSize;
        double[][] edgeMatrix = new double[dim][dim];
        int e_u;
        int e_v;
        for (int i = 0; i < uSize; i++) {
            e_u = source.getNodeEdge(u, i);
            //(Edge) u.getEdges().get(i);
            for (int j = 0; j < vSize; j++) {
                e_v = target.getNodeEdge(v, j);
                //(Edge) v.getEdges().get(j);
                double costs = CostFunction.getCost(e_u, e_v);
                edgeMatrix[i][j] = costs;
            }
        }
        for (int i = uSize; i < dim; i++) {
            for (int j = 0; j < vSize; j++) {
                // diagonal
                if ((i - uSize) == j) {
                    e_v = target.getNodeEdge(v,j);
                    double costs = CostFunction.getEdgeCosts();
                    edgeMatrix[i][j] = costs;
                } else {
                    edgeMatrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = 0; i < uSize; i++) {
            e_u = source.getNodeEdge(u, i);
            //(Edge) u.getEdges().get(i);
            for (int j = vSize; j < dim; j++) {
                // diagonal
                if ((j - vSize) == i) {
                    double costs = CostFunction.getEdgeCosts();
                    edgeMatrix[i][j] = costs;
                } else {
                    edgeMatrix[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }
        for (int i = uSize; i < dim; i++) {
            for (int j = vSize; j < dim; j++) {
                edgeMatrix[i][j] = 0.0;
            }
        }
        return edgeMatrix;
    }
}
