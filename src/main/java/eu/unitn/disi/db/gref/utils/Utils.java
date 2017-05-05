package eu.unitn.disi.db.gref.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class Utils {
    private Utils() {
    }
    
    public static <T> int setIntersection(Set<T> set1, Set<T> set2) {
        Set<T> a;
        Set<T> b;
        if (set1.size() <= set2.size()) {
            a = set1;
            b = set2;
        } else {
            a = set2;
            b = set1;
        }
        int count = 0;
        for (T e : a) {
            if (b.contains(e)) {
                count++;
            }
        }
        return count;
    }
    
    public static <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
        Set<T> a;
        Set<T> b;
        Set<T> intersection = new HashSet<>();
        if (set1.size() <= set2.size()) {
            a = set1;
            b = set2;
        } else {
            a = set2;
            b = set1;
        }
        for (T e : a) {
            if (b.contains(e)) {
                intersection.add(e);
            }
        }
        return intersection;
    }
    
    
    public static Set<Integer> intArrayToSet(int[] array) {
        Set<Integer> set = new HashSet<>(); 
        for (int el : array) {
            set.add(el);
        }
        return set; 
    }
    
    public static <T>String matrixToString(T[][] matrix) {
        StringBuilder sb = new StringBuilder(); 
        for (T[] matrix1 : matrix) {
            sb.append(Arrays.toString(matrix1)).append("\n");
        }
        return sb.toString();
    }
    
    public static String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder(); 
        for (int[] matrix1 : matrix) {
            sb.append(Arrays.toString(matrix1)).append("\n");
        }
        return sb.toString();
    }

    
    public static String readFileToString(String inputFile) 
            throws IOException, FileNotFoundException
    {
        String line; 
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(inputFile))) {
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } 
        return sb.toString();
    }
    
    public static void writeStringToFile(String s, String outputFile) throws IOException {
        writeStringToFile(s, outputFile, false);
    }

    
    public static void writeStringToFile(String s, String outputFile, boolean append) throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outputFile, append))) {
            out.append(s);
        }
    }

    
    public static String join (String joinString, String... array) {
        if ( array == null || array.length == 0 ) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        out.append( array[0] );
        for ( int i = 1; i < array.length; i++ ) {
          out.append(joinString).append(array[i]);
        }
        return out.toString();
    }

}
