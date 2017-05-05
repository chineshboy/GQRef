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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Reformulation lattice contains expansions of the input query, the lattice
 * has a double searching system (via iterator and 
 * 
 * @see ReformulatedQuery
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class ReformulationLattice implements Iterable<ReformulatedQuery>, Serializable {
    protected ReformulatedQuery root;
    protected Map<Query, ReformulatedQuery> index; 
    
    public ReformulationLattice(Query query) {
        root = new ReformulatedQuery(query.getGraph());
        index = new HashMap<>();
    }
    
    public ReformulatedQuery findReformulation(Query query) 
            throws NullPointerException 
    {
        return index.get(query);
    }
    
    public void addReformulation(ReformulatedQuery query) 
            throws NullPointerException
    {
        index.put(query, query);
    }

    public boolean containsReformulation(Query query)
            throws NullPointerException
    {
        return index.containsKey(query); 
    }
    
    
    private class RefIterator implements Iterator<ReformulatedQuery>  {
        private final LinkedList<ReformulatedQuery> queue; 
        private final Set<ReformulatedQuery> visited; 
        
        public RefIterator() {
            queue = new LinkedList<>();
            visited = new HashSet<>();
            queue.add(root);
        }
    
        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public ReformulatedQuery next() {
            if (queue.isEmpty())
                return null; 
            ReformulatedQuery q = queue.poll();
            for (ReformulatedQuery child : q.reformulations) {
                if (!visited.contains(child)) {
                    visited.add(child);
                    queue.add(child);
                }
            }
            return q;
        }

        @Override
        public void remove() {
            if (!queue.isEmpty()) {
                queue.removeFirst();
            }
        }
    }

    public ReformulatedQuery getRoot() {
        return root;
    }

    public Map<Query, ReformulatedQuery> getIndex() {
        return index;
    }
    
    public int size() {
        return index.size();
    }
    
    @Override
    public Iterator<ReformulatedQuery> iterator() {
        return new RefIterator();
    }    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<ReformulatedQuery> visited = new HashSet<>();
        LinkedList<ReformulatedQuery> queue = new LinkedList<>();
        ReformulatedQuery currentQuery; 
        int edgeCount = root.getEdgeCount();
        int level = 0;
        queue.add(root);
        
        sb.append(String.format("Level[%d]\n", level));
        while (!queue.isEmpty()) {
            currentQuery = queue.poll();
            if (currentQuery.getEdgeCount() == edgeCount + 1) {
                level++;
                edgeCount++;
                sb.append(String.format("Level[%d]\n", level));
            }
            sb.append(currentQuery.toString()).append("\n");
            for (ReformulatedQuery q : currentQuery.reformulations) {
                if (!visited.contains(q)) {
                    queue.add(q);
                    visited.add(q);
                }
            }
        }
        return sb.toString();
    }
    
}
