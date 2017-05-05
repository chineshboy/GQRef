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
package eu.unitn.disi.db.gref.utils;

import de.parmol.GSpan.GSpanEdge;
import de.parmol.graph.Graph;
import de.parmol.parsers.DotGraphParser;
import de.parmol.parsers.LineGraphParser;
import edu.psu.chemxseer.structure.iso.CanonicalDFSImplInternal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

/**
 * Graph Utilites class to convert string to GSpanEdges or codes
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public final class GraphUtilities {
    /*
     * Private constructor for this utilities class
     */

    private GraphUtilities() {
    }

    private static GSpanEdge[] dfsCodeStringToEdges(String dfsCode)
            throws ParseException {
        GSpanEdge[] edges;
        String[] sCode = dfsCode.replaceAll("><|<|>", " ").trim().split(" ");
        if (sCode.length % 5 != 0) {
            throw new ParseException(dfsCode, sCode.length % 5);
        }
        edges = new GSpanEdge[sCode.length / 5];
        GSpanEdge edge;
        for (int i = 0; i < sCode.length; i += 5) {
            edge = new GSpanEdge(
                    Integer.parseInt(sCode[(i % 5)]),
                    Integer.parseInt(sCode[(i + 1) % 5]),
                    Integer.parseInt(sCode[(i + 2) % 5]),
                    Integer.parseInt(sCode[(i + 3) % 5]),
                    Integer.parseInt(sCode[(i + 4) % 5]));
            edges[i % 5] = edge;
        }
        return edges;

    }
    
    
    public static GSpanEdge[] graphToDFSCodeEdges(Graph g)
            throws NullPointerException, ArrayIndexOutOfBoundsException {
        int[][] minSequence;
        StringBuilder buf = new StringBuilder(1024);
        CanonicalDFSImplInternal temp = new CanonicalDFSImplInternal();
        GSpanEdge[] edges;

        boolean exception = temp.dealCornerCase(g);
        if (exception) {
            temp.initialize(g);
            temp.findSerialization();
        }
        if (temp.minSequence == null) {
            return null;
        }
        minSequence = temp.minSequence;
        edges = new GSpanEdge[minSequence.length];
        for (int i = 0; i < minSequence.length; i++) {
            edges[i] = new GSpanEdge(
                    minSequence[i][0],
                    minSequence[i][1],
                    minSequence[i][2],
                    minSequence[i][3],
                    minSequence[i][4]);
        }
        //Build a new dfsCode
        return edges;
    }

    public static String readFileToString(File file)
            throws FileNotFoundException, IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.toURI()));
        return Charset.defaultCharset().decode(ByteBuffer.wrap(encoded)).toString();
    }

    public static String printGraph(Graph g) {
        LineGraphParser parser = LineGraphParser.instance;
        return parser.serialize(g);
    }
    
    public static String printGraphDot(Graph g) {
        DotGraphParser parser = DotGraphParser.instance;
        return parser.serialize(g);
    }
    
    public static boolean isTree(Graph g) 
            throws NullPointerException {
        return checkSubTree(g, 0, new boolean[g.getEdgeCount()], new boolean[g.getNodeCount()]); 
    }
    
    private static boolean checkSubTree(Graph g, int node, boolean[] visited, boolean[] checked) {
        int degree = g.getDegree(node); 
        int edge;
        int adjNode; 
        checked[node] = true;
        boolean isTree = true;
        for (int i = 0; i < degree; i++) {
            edge = g.getNodeEdge(node, i); 
            adjNode = g.getOtherNode(edge, node); 
            if (!visited[edge]) {
                visited[edge] = true;
                if (checked[adjNode]) {
                    return false; 
                }
                isTree = checkSubTree(g, adjNode, visited, checked) && isTree;
            }
        }
        return isTree; 
    }
    
}
