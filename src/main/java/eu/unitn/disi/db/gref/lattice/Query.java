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
import java.io.Serializable;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a query as a {@link Graph}. The query is immutable and is represented
 * as a DFS code. 
 * 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class Query extends CodedGraph implements Serializable {
    protected Set<Integer> results;    
    
    public Query(Graph graph) {
        super(graph);
        results = new HashSet<>();
    }
    
    //TODO: implement this. 
    public void serialize(Serializer serializer, Writer w) {
        
    }

    public Set<Integer> getResults() {
        return results;
    }
    
    public boolean addResult(int gId)
            throws NullPointerException
    {
        return results.add(gId);
    }

    public boolean removeResult(int gId)
            throws NullPointerException
    {
        return results.remove(gId);
    }
    
    public boolean containsResult(int gId)
            throws NullPointerException
    {
        return results.contains(gId);
    }
    
    public int resultsNumber() {
        return results.size();
    }

    
    public int getFrequency()
    {
        return results.size();
    }

    public boolean isEmpty() 
    {
        return results.isEmpty();
    }
}
