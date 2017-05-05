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

package eu.unitn.disi.db.gref.lattice;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A {@link TreeSet} consistent with equals. The consistency with equals implies
 * that for each duplicate value it contains a {@link HashSet} of clashing values
 * 
 * Note: this implementation is not synchronized
 * 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class BucketTreeSet<E>  
    extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable 
{   
    /**
     * The backing map.
     */
    private transient NavigableMap<E,Set<E>> m;

    private int size = 0;
    
    
    BucketTreeSet(NavigableMap<E,Set<E>> m) {
        this.m = m;
    }
    
    /**
     * Constructs a new, empty tree set, sorted according to the
     * natural ordering of its elements.  All elements inserted into
     * the set must implement the {@link Comparable} interface.
     * Furthermore, all such elements must be <i>mutually
     * comparable</i>: {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and
     * {@code e2} in the set.  If the user attempts to add an element
     * to the set that violates this constraint (for example, the user
     * attempts to add a string element to a set whose elements are
     * integers), the {@code add} call will throw a
     * {@code ClassCastException}.
     */
    public BucketTreeSet() {
        m = new TreeMap<>();
    }

    /**
     * Constructs a new, empty tree set, sorted according to the specified
     * comparator.  All elements inserted into the set must be <i>mutually
     * comparable</i> by the specified comparator: {@code comparator.compare(e1,
     * e2)} must not throw a {@code ClassCastException} for any elements
     * {@code e1} and {@code e2} in the set.  If the user attempts to add
     * an element to the set that violates this constraint, the
     * {@code add} call will throw a {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this set.
     *        If {@code null}, the {@linkplain Comparable natural
     *        ordering} of the elements will be used.
     */
    public BucketTreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<E,Set<E>>(comparator));
    }
    
    /**
     * Constructs a new tree set containing the elements in the specified
     * collection, sorted according to the <i>natural ordering</i> of its
     * elements.  All elements inserted into the set must implement the
     * {@link Comparable} interface.  Furthermore, all such elements must be
     * <i>mutually comparable</i>: {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and
     * {@code e2} in the set.
     *
     * @param c collection whose elements will comprise the new set
     * @throws ClassCastException if the elements in {@code c} are
     *         not {@link Comparable}, or are not mutually comparable
     * @throws NullPointerException if the specified collection is null
     */
    public BucketTreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * Constructs a new tree set containing the same elements and
     * using the same ordering as the specified sorted set.
     *
     * @param s sorted set whose elements will comprise the new set
     * @throws NullPointerException if the specified sorted set is null
     */
    public BucketTreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }

    @Override
    public String toString() {
        return super.toString(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        size = 0; 
        m.clear();
        super.clear(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean remove(Object o) {
        Set<E> values = (Set<E>)m.remove(o);
        if (values != null) {
            if (!values.remove(o) || !values.isEmpty()) {
                m.put(values.iterator().next(), values);
            }
            size--;
            return true;
        }
        return false; 
    }

    @Override
    public boolean add(E e) {
        size++;
        Set<E> values = m.get(e);
        if (values == null) {
            values = new HashSet<>();
        }
        values.add(e);
        return m.put(e, values) != null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return super.toArray(a); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object[] toArray() {
        return super.toArray(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean contains(Object o) {
        Set<E> values = m.get(o);
        return values != null && !values.isEmpty() && values.contains(o);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    public Iterator<E> iterator() {
        return new BucketIterator(true);
    }
    
    private class BucketIterator implements Iterator<E> {
        private final Iterator<Set<E>> externalIterator; 
        private Iterator<E> internalIterator; 
        
        public BucketIterator(boolean ascending) {
            if (ascending) {
                externalIterator = m.values().iterator();
            } else {
                externalIterator = m.descendingMap().values().iterator();
            }
        }
        
        @Override
        public boolean hasNext() {
            return externalIterator.hasNext() || internalIterator.hasNext();
        }

        @Override
        public E next() {
            E next = null;
            if (internalIterator.hasNext()) {
                next = internalIterator.next();
            } else if (externalIterator.hasNext()) {
                internalIterator = externalIterator.next().iterator();
                next = internalIterator.next();
            } else {
                throw new NoSuchElementException();
            }
            return next; 
        }

        @Override
        public void remove() {
            internalIterator.remove();
            if (!internalIterator.hasNext()) {
                externalIterator.remove();
            }
            size--;
        }
    }
    

    @Override
    public int size() {
        return size; 
    }

    @Override
    public E lower(E e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E floor(E e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E ceiling(E e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E higher(E e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new BucketTreeSet<>(m.descendingMap());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new BucketIterator(false);
    }
    
    public Set<E> getBucket(E el) {
        return m.get(el);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Comparator<? super E> comparator() {
        return m.comparator();
    }

    @Override
    public E first() {
        Set<E> values = m.firstEntry().getValue();
        if (values == null) {
            throw new NoSuchElementException();
        }
        return values.iterator().next();
    }

    @Override
    public E last() {
        if (m.isEmpty())
            throw new NoSuchElementException();
        Set<E> values = m.lastEntry().getValue();
        if (values == null) {
            throw new NoSuchElementException();
        }
        return values.iterator().next();
    }    
}
