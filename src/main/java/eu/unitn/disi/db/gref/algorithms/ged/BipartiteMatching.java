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

/**
 * @author riesen
 *
 */
public class BipartiteMatching {

    /**
     * the matching procedure defined via GUI or properties file possible
     * choices are 'Hungarian' or 'VJ' (VolgenantJonker)
     */
    private String matching;

    /**
     * whether (1) or not (0) the matching found is logged on the console
     */
    //private int outputMatching;

    /**
     * @param matching
     * @param outputMatching
     */
    public BipartiteMatching(String matching) {
        this.matching = matching;
    }

    /**
     * @return the optimal matching according to the @param costMatrix the
     * matching actually used is defined in the string "matching"
     */
    public int[][] getMatching(double[][] costMatrix) {
        int[][] assignment = null;;
        if (this.matching.equals("Hungarian")) {
            HungarianAlgorithm ha = new HungarianAlgorithm();
            assignment = ha.hgAlgorithm(costMatrix);
        }
        if (this.matching.equals("VJ")) {
            VolgenantJonker vj = new VolgenantJonker();
            vj.computeAssignment(costMatrix);
            int[] solution = vj.rowsol;
            assignment = new int[costMatrix.length][2];
            // format the assignment correctly
            for (int i = 0; i < solution.length; i++) {
                assignment[i][0] = i;
                assignment[i][1] = solution[i];
            }

        }
        // log the matching on the console
//        if (this.outputMatching == 1) {
//            System.out.println("\nThe Optimal Matching:");
//            for (int k = 0; k < assignment.length; k++) {
//                System.out.print(assignment[k][0] + " -> " + assignment[k][1] + " ");
//            }
//            System.out.println();
//        }

        return assignment;
    }

}
