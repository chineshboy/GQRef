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
package eu.unitn.disi.db.gref.commands;

import de.parmol.graph.Graph;
import de.parmol.graph.GraphFactory;
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.algorithms.ged.EditDistance;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Use edit distance to find significative graphs.
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class SummarizeDataset extends Command {

    private String dbFile;
    private String parserClass;
    private String outputFile;
    private double editThreshold;
    private int numberOfThreads;

    private GraphFactory factory;
    private final static int PRINT_STEP = 20;

    private class SelectionTask implements Callable<List<Graph>> {

        private final List<Graph> toCheck;
        private final List<Graph> significantGraphs;
        private final int thread;
        private final int level;

        public SelectionTask(int thread, int level, List<Graph> toCheck, List<Graph> significantGraphs) {
            this.level = level;
            this.thread = thread;
            this.toCheck = toCheck;
            this.significantGraphs = significantGraphs;
        }

        @Override
        public List<Graph> call() throws Exception {
            boolean significant;
            int count = 0;
            double distance;

            for (Graph graph : toCheck) {
                count++;
                significant = true;
                for (Graph sigGraph : significantGraphs) {
                    distance = EditDistance.getApproximateEditDistance(graph, sigGraph, EditDistance.ApproximationType.VOLGENANT_JONKER);
                    if (distance / (double) (graph.getEdgeCount() + sigGraph.getEdgeCount()) < editThreshold) {
                        //System.out.println(distance);
                        significant = false;
                        break;
                    }
                }
                if (significant) {
                    significantGraphs.add(graph);
                    if (significantGraphs.size() % PRINT_STEP == 0) {
                        debug("[t=%d,l=%d] Found %d significant graphs over %d", thread, level, significantGraphs.size(), count);
                    }
                }
            }
            return significantGraphs;
        }

    }

    @Override
    protected void execute() throws ExecutionException {
        GraphParser parser;
        List<Graph> db;
        StopWatch watch = new StopWatch();
        List<Graph> significantDataset = new ArrayList<>();
        int mask;
        ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);
        int count;

        MyFactory.getDFSCoder();//To load the factories (insane, I know)

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(dbFile));
                BufferedWriter out = new BufferedWriter(new FileWriter(outputFile))) {
            parser = (GraphParser) Class.forName(parserClass).newInstance();
            mask = GraphFactory.UNDIRECTED_GRAPH;
            factory = GraphFactory.getFactory(mask);

            db = Arrays.asList(parser.parse(in, factory));
            Collections.shuffle(db);
// From experiments this seems not a good idea (too many clusters). 
//            Collections.sort(db, 
//                new Comparator<Graph>() {
//                    @Override
//                    public int compare(Graph o1, Graph o2) {
//                        int c1 = o1.getEdgeCount(), c2 = o2.getEdgeCount();
//                        if (c1 < c2) {
//                            return -1; 
//                        } else if (c1 > c2) {
//                            return 1;
//                        }
//                        return 0;
//                    }                    
//                }
//            );
            info("Loaded graph database %s in %dms", dbFile, watch.getElapsedTimeMillis());

            List<Future<List<Graph>>> futures = new ArrayList<>(), tmpFutures;
            int bucketSize = db.size() / numberOfThreads;
            int level;

            for (int i = 0; i < numberOfThreads; i++) {
                futures.add(
                        pool.submit(
                                new SelectionTask(i + 1, 0,
                                        db.subList(i * bucketSize, i == (numberOfThreads - 1) ? db.size() : (i + 1) * bucketSize),
                                        new ArrayList<Graph>())));
            }
            level = 1;
            while (futures.size() > 1) {
                tmpFutures = new ArrayList<>();
                for (int i = 0; i < futures.size() - 1; i += 2) {
                    tmpFutures.add(
                            pool.submit(
                                    new SelectionTask(i + 1, level, futures.get(i).get(), futures.get(i + 1).get())
                            )
                    );

                }
                if (futures.size() % 2 == 1) {
                    tmpFutures.add(futures.get(futures.size() - 1));
                }
                futures = tmpFutures;
                level++;
            }
            significantDataset = futures.get(0).get();

            info("Size of the dataset: %d ", significantDataset.size());
            info("Significance level: %f", editThreshold);
            info("Writing to file %s", outputFile);

            count = 0;
            for (Graph graph : significantDataset) {
                out.append(parser.serialize(graph).replaceFirst("t # [0-9]+", "t # " + count++));
            }
            pool.shutdown();
        } catch (IOException ex) {
            throw new ExecutionException("Error while reading the file %s", ex, dbFile);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            fatal("Graph parser %s cannot be instantiated", parserClass);
        } catch (ParseException ex) {
            throw new ExecutionException("Cannot parse the database file", ex);
        } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
            throw new ExecutionException("Error with multithread execution", ex);
        }

    }

    @Override
    protected String commandDescription() {
        return "Use graph edit distance to find a significant set of graphs";
    }

    @CommandInput(
            consoleFormat = "-db",
            defaultValue = "",
            mandatory = true,
            description = "graph database file",
            parameters = ParametersNumber.TWO)
    public void setDbFile(String dbFile) {
        this.dbFile = dbFile;
    }

    @CommandInput(
            consoleFormat = "-o",
            defaultValue = "graph",
            mandatory = false,
            description = "output graph file",
            parameters = ParametersNumber.TWO)
    public void setOutput(String output) {
        this.outputFile = output;
    }

    @CommandInput(
            consoleFormat = "-parser",
            defaultValue = "de.parmol.parsers.LineGraphParser",
            mandatory = false,
            description = "parser used for the input graph",
            parameters = ParametersNumber.TWO)
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }

    @CommandInput(
            consoleFormat = "-t",
            defaultValue = "8",
            mandatory = false,
            description = "number of threads",
            parameters = ParametersNumber.TWO)
    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    @CommandInput(
            consoleFormat = "-s",
            defaultValue = "0.1",
            mandatory = false,
            description = "minimumn edit distance to keep the graph",
            parameters = ParametersNumber.TWO)
    public void setEditThreshold(double editThreshold) {
        this.editThreshold = editThreshold;
    }
}
