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
package eu.unitn.disi.db.gref.algorithms;

import de.parmol.graph.Graph;
import eu.unitn.disi.db.command.algorithmic.AlgorithmInput;
import eu.unitn.disi.db.command.exceptions.AlgorithmExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.algorithms.ged.EditDistance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple k-means algorithm for graph databases
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GraphClustering extends LatticeAlgorithm {

    @AlgorithmInput(
            description = "Number of clusters for simple k-means",
            mandatory = false,
            defaultValue = "2"
    )
    private int k;

    @AlgorithmInput(
            description = "Number of prototypes used in the embedding",
            mandatory = false,
            defaultValue = "-1"
    )
    private int n;

    private Collection<Integer>[] clusters;
    private Collection<Graph> medoids; 

    
    @Override
    public void compute() throws AlgorithmExecutionException {
        //DECLARATIONS
        StopWatch watch = new StopWatch();
        
        watch.start();

        if (n == -1)//i.e., parameter not set
        {
            //set it to a default value equal to 10% of the size of the database
            n = (int) Math.round(((double) gdb.length) / 10);
        }

        //compute all pairwise distances
        watch.reset();
        info("Computing pairwise distances...");
        double[][] distances = getPairwiseDistances(gdb);
        info("Pairwise distances computed (time: %dms", watch.getElapsedTimeMillis());

        //compute prototypes
        watch.reset();
        info("Computing prototypes...");
        int[] prototypes = getPrototypes(distances);
        info("Prototypes computed (time: %dms", watch.getElapsedTimeMillis());

        /*
         //compute embeddings
         watch.reset();
         info("Computing embeddings...");
         double[][] embeddings = getEmbeddings(distances,prototypes);
         info("Embeddings computed (time: %dms", watch.getElapsedTimeMillis());
         */
        //run kmeans
        watch.reset();
        info("K-means started...");
        //Collection<Integer>[] clusters = kmeans(embeddings);
        clusters = kmeans(distances, prototypes);
        StringBuilder sb = new StringBuilder();

        info("K-means finished (time: %dms", watch.getElapsedTimeMillis());

        //printing clustering
        info("Clustering:");

        medoids = new ArrayList<Graph>();
        for (int i = 0; i < clusters.length; i++) {
            sb = new StringBuilder();
            int m = getMedoid(clusters[i], distances);
            sb.append(String.format("Cluster %d:\t", i));
            sb.append(String.format("[Medoid: %d]\t", m));
            medoids.add(gdb[m]);
            for (int x : clusters[i]) {
                sb.append(x + "\t");
            }
            info(sb.toString());
        }

    }


    private double[][] getPairwiseDistances(Graph[] gdb) {
        int tick = 0;
        StopWatch watch = new StopWatch();
        watch.start();
        double[][] distances = new double[gdb.length][gdb.length];
        for (int i = 0; i < gdb.length - 1; i++) {
            Graph g1 = gdb[i];
            for (int j = i + 1; j < gdb.length; j++) {
                Graph g2 = gdb[j];
                //dist = EditDistance.getExactEditDistance(g1, g2);
                //info("Graph edit distance: %f", dist);

                double dist = EditDistance.getApproximateEditDistance(g1, g2, EditDistance.ApproximationType.HUNGARIAN);
                //info("Approximate Graph edit distance: %f", dist);

                if (tick % 10000 == 0) {
                    info("Approximate Graph edit distance: %f, Time: %dms", dist, watch.getElapsedTimeMillis());
                    watch.reset();
                }
                tick++;

                distances[i][j] = dist;
                distances[j][i] = dist;
            }
        }

        return distances;
    }

    private double[][] getEmbeddings(double[][] distances, int[] prototypes) {
        double[][] embeddings = new double[distances.length][prototypes.length];

        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < prototypes.length; j++) {
                embeddings[i][j] = distances[i][prototypes[j]];
            }
        }

        return embeddings;
    }

    private Collection<Integer>[] kmeans(double[][] distances, int[] prototypes) {
        Collection<Integer>[] clusters = new Collection[k];
        for (int i = 0; i < clusters.length; i++) {
            clusters[i] = new ArrayList<Integer>();
        }

        double[][] centroids = getInitialCentroids(distances, prototypes);
        int[] a = computeClusterAssignments(centroids, distances, prototypes);

        boolean convergence = false;
        while (!convergence) {
            double[][] new_centroids = computeCentroids(a, distances, prototypes);
            int[] new_a = computeClusterAssignments(new_centroids, distances, prototypes);

            if (!hasChanged(a, new_a)) {
                convergence = true;
            }

            centroids = new_centroids;
            a = new_a;
        }

        //build clusters
        for (int i = 0; i < a.length; i++) {
            int c = a[i];
            clusters[c].add(i);
        }

        return clusters;
    }

    private int[] getPrototypes(double[][] distances) {
        Set<Integer> prot = new HashSet<Integer>(); //prototypes

        //select the first prototype, i.e., the median graph
        double min_score = Double.POSITIVE_INFINITY;
        int median = -1;
        for (int i = 0; i < distances.length; i++) {
            double score_i = 0.0;
            for (int j = 0; j < distances[i].length && score_i <= min_score; j++) {
                score_i += distances[i][j];
            }
            if (score_i < min_score) {
                min_score = score_i;
                median = i;
            }
        }
        prot.add(median);

        //for each graph in the database, it stores the minimum distance from the current set of prototypes
        double[] mindist = new double[distances.length];
        for (int i = 0; i < mindist.length; i++) {
            mindist[i] = distances[i][median];
        }

        //select the remaining n-1 prototypes based on how much they differ from the already selected ones
        while (prot.size() < n) {
            double maxd = Double.NEGATIVE_INFINITY;
            int p = -1;
            for (int i = 0; i < mindist.length; i++) {
                if (!prot.contains(i) && mindist[i] > maxd) {
                    maxd = mindist[i];
                    p = i;
                }
            }

            prot.add(p);

            //update min distances
            for (int i = 0; i < mindist.length; i++) {
                double d = distances[i][p];
                if (d < mindist[i]) {
                    mindist[i] = d;
                }
            }
        }

        //build prototype array and return it 
        int[] vprot = new int[prot.size()];
        int i = 0;
        for (int x : prot) {
            vprot[i] = x;
            i++;
        }
        return vprot;
    }

    private double[][] getInitialCentroids(double[][] distances, int[] prototypes) {
        int[] shuffle = new int[distances.length];
        for (int i = 0; i < shuffle.length; i++) {
            shuffle[i] = i;
        }

        //apply the Knuth-Shuffle algorithm to randomly shuffle the 'shuffle' array and pick the first k elements as randomly-selected centroids
        for (int i = 1; i < shuffle.length; i++) {
            int tmp = shuffle[i];
            int j = (int) Math.round(Math.random() * i);
            /*
             if(j > i)
             {
             throw new RuntimeException("ERROR: j cannot be greater than i---j="+j+" ,"+i);
             }
             */
            shuffle[i] = shuffle[j];
            shuffle[j] = tmp;
        }

        double[][] centroids = new double[k][prototypes.length];
        for (int i = 0; i < centroids.length; i++) {
            int a = shuffle[i];
            for (int j = 0; j < centroids[i].length; j++) {
                int b = prototypes[j];
                centroids[i][j] = distances[a][b];
            }
        }

        return centroids;
    }

    private int[] computeClusterAssignments(double[][] centroids, double[][] distances, int[] prototypes) {
        int[] a = new int[distances.length]; //cluster assignments for each graph in the database

        for (int i = 0; i < a.length; i++) {
            double min_d = Double.POSITIVE_INFINITY;
            a[i] = -1;
            for (int j = 0; j < centroids.length; j++) {
                double d = getSquaredEuclidianDistance(distances, prototypes, i, centroids[j]);
                if (d < min_d) {
                    min_d = d;
                    a[i] = j;
                }
            }
        }

        return a;
    }

    private double getSquaredEuclidianDistance(double[][] distances, int[] prototypes, int i, double[] centroid) {
        double d = 0.0;
        for (int j = 0; j < prototypes.length; j++) {
            int pj = prototypes[j];
            double x = distances[i][pj];
            double y = centroid[j];
            d += (x - y) * (x - y);
        }

        return d;
    }

    private boolean hasChanged(int[] a, int[] new_a) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != new_a[i]) {
                return true;
            }
        }

        return false;
    }

    private double[][] computeCentroids(int[] a, double[][] distances, int[] prototypes) {
        double[][] centroids = new double[k][prototypes.length];
        int[] cluster_size = new int[k];

        for (int i = 0; i < a.length; i++) {
            int c = a[i];
            cluster_size[c]++;

            for (int j = 0; j < prototypes.length; j++) {
                int pj = prototypes[j];
                centroids[c][j] += distances[i][pj];
            }
        }

        for (int i = 0; i < centroids.length; i++) {
            if (cluster_size[i] > 0) {
                for (int j = 0; j < centroids[i].length; j++) {
                    centroids[i][j] /= cluster_size[i];
                }
            }
        }

        return centroids;
    }

    public int getMedoid(Collection<Integer> collection, double[][] distances) {
        int m = -1;
        double dmax = Double.POSITIVE_INFINITY;

        for (int x : collection) {
            double sum = 0.0;
            for (int y : collection) {
                if (x != y) {
                    sum += distances[x][y];
                }
            }
            if (sum < dmax) {
                dmax = sum;
                m = x;
            }
        }
        return m;
    }

    public Collection<Graph> getMedoids() {
        return medoids;   
    }
    
    public Collection<Integer>[] getClusters() {
        return clusters;
    }

    @Override
    public int getNumberOfReformulations() {
        return gdb.length;
    }

    @Override
    public int getNumberOfExpansions() {
        return 0;
    }
}
