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
public class CostFunction {


    /**
     * initializes the costfunction according to the properties read in the
     * properties file
     */
    private CostFunction() {
    }

    /**
     * @return the substitution cost between node @param u and node @param v
     * according to their attribute values and the cost functions. The
     * individual costs are softened by the importance factors and finally added
     * or multiplied (and possibly the result is "square rooted") The final
     * result is multiplied by alpha
     */
    public static double getCost(int label1, int label2) {
        if (label1 == label2) {
            return 0;
        }
        return 0.5;
    }

    /**
     * @return the substitution cost between edge @param u and edge @param v
     * according to their attribute values and the cost functions. The
     * individual costs are softened by the importance factors and finally added
     * or multiplied (and possibly the result is "square rooted") The final
     * result is multiplied by (1-alpha)
     */
    
    /**
     * @return the constant cost for node deletion/insertion multiplied by alpha
     */
    public static double getNodeCosts() {
        return .5;
    }

    /**
     * @return the constant cost for edge deletion/insertion multiplied by
     * (1-alpha)
     */
    public static double getEdgeCosts() {
        return .5;
    }

}
