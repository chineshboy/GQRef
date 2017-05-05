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
import de.parmol.graph.MutableGraph;
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.iso.FastSUCompleteEmbedding;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Geenerate queries used in the experiment using uniform distribution
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GenerateQueries extends Command {
    private int numPerSize; 
    private int fromSize;
    private int toSize;
    private int minResults; 
    private float maxMultiple; 
    private String dbFile;
    private String parserClass;
    private String outputFile;
    private boolean directed; 
    private int numberOfGraphs;
    private int step; 
    
    private static final int MAX_ITERATIONS = 2000; 
    private long seed = 123623456L; //A random fixed seed. 
    private GraphFactory factory; 
    
    //TODO: Insert a timeout time
    @Override
    protected void execute() throws ExecutionException {
        GraphParser parser;
        int mask;
        List<Graph> db;
        boolean success = false;
        int maxSize = 0, results, multipleResults;
        Graph g; 
        Set<String> queries;
        MutableGraph query;
        String queryCode; 
        queries = new LinkedHashSet<>();
        StopWatch watch = new StopWatch();
        FastSUCompleteEmbedding isoTest;
        int numIterations;
        
        MyFactory.getDFSCoder();//To load the factories (insane, I know)
        
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(dbFile))) {
            parser = (GraphParser) Class.forName(parserClass).newInstance();

            mask = directed? GraphFactory.DIRECTED_GRAPH: GraphFactory.UNDIRECTED_GRAPH;
            factory = GraphFactory.getFactory(mask);
            watch.start();
            db = Arrays.asList(parser.parse(in, factory));
            if (numberOfGraphs > 0 && numberOfGraphs < db.size()) {
                db = db.subList(0, numberOfGraphs); 
            }
            info("Loaded graph database %s in %dms", dbFile, watch.getElapsedTimeMillis());
            
            for (Graph graph : db) {
                if (graph.getEdgeCount() > maxSize) {
                    maxSize = graph.getEdgeCount();
                }
            }
//            query = dfs(db.get(13534),3);
//                            query = dfs(g, size);
//                            try {
//                                queryCode = MyFactory.getDFSCoder().serialize(query);
//                                if (!queries.contains(queryCode)) {
//                                    debug("Generated query %s", queryCode);
//                                    queries.add(queryCode);
//                                    success = true;
//                                }
//                            } catch (Exception ex) {
//                                System.out.println(GraphUtilities.printGraphDot(db.get(1826)));
//                                System.out.println(GraphUtilities.printGraphDot(query));
//                                //System.out.println(query.getEdgeCount());
//                                //System.out.println(GraphUtilities.printGraphDot(query));
//                                ex.printStackTrace();
//                                warn("Failing to convert a graph into a DFS code");
//                                //queryCode = MyFactory.getDFSCoder().serialize(query);
//                            }
            
            
            
            Random rand = new Random(seed);
            for (int size = fromSize; size <= toSize && size <= maxSize; size += step) {
                info("Generating queries of size %d", size);
                for (int num = 0; num < numPerSize; num++) {
                    success = false;
                    numIterations = 0; 
                    while (!success && numIterations < MAX_ITERATIONS) {
                        g = db.get(rand.nextInt(db.size()));
                        numIterations++;
                        if (g.getEdgeCount() > size) {
                            query = dfs(g, size);
                            try {
                                queryCode = MyFactory.getDFSCoder().serialize(query);
                                if (!queries.contains(queryCode)) {
                                    results = 1; 
                                    multipleResults = 0; 
                                    if(minResults > 1 || maxMultiple < 1f) {
                                        results = 0; 
                                        for (Graph gr : db) {
                                            isoTest = new FastSUCompleteEmbedding(query, gr);
                                            if (isoTest.issubIsomorphic()) {
                                                results++; 
                                                if (isoTest.getMapNum() > 1) {
                                                    multipleResults++;
                                                }
                                            }
                                        }
                                    }
                                    if (results >= minResults && multipleResults/(float)results <= maxMultiple) {
                                        debug("Generated query %s", queryCode);
                                        queries.add(queryCode);
                                        success = true;
                                    } else {
                                        debug("Query %s not inserted: results %d, multiples: %d",queryCode, results, multipleResults);
                                    }
                                }
                            } catch (Exception ex) {
                                //System.out.println(GraphUtilities.printGraphDot(g));
                                //System.out.println(GraphUtilities.printGraphDot(query));
                                //System.out.println(query.getEdgeCount());
                                //System.out.println(GraphUtilities.printGraphDot(query));
                                //ex.printStackTrace();
                                error("Failing to convert a graph into a DFS code");
                                //queryCode = MyFactory.getDFSCoder().serialize(query);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new ExecutionException("Error while reading the file %s", ex, dbFile);
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            fatal("Graph parser %s cannot be instantiated", parserClass);
        } catch (ParseException ex) {
            throw new ExecutionException("Cannot parse the database file", ex);
        }
        
        info("Writing to file %s", outputFile);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outputFile))) {
            out.append(String.format("# number of queries per size: %d, min size: %d, max size: %d\n", numPerSize, fromSize, toSize));
            for (String q : queries) {
                out.append(q).append("\n");
            }
        } catch (IOException ex) {
            throw new ExecutionException("Error while writing the file %s", ex, outputFile);
        }
        
    }
    
    private MutableGraph dfs(Graph g, int size) {
        MutableGraph query = factory.createGraph();
        LinkedList<Integer> queue = new LinkedList<>();
        //Select a random starting node. 
        Random r = new Random(seed); 
        Set<Integer> visitedEdges = new HashSet<>(), visitedNodes = new HashSet<>();
        Map<Integer, Integer> nodeMapping = new HashMap<>(); 
        LinkedList<Integer> edges = new LinkedList<>();
        
        int count = 0; 
        int node, adjNode, edge;  
        
//        queue.add(6);
        queue.add(r.nextInt(g.getNodeCount()));
        while (count < size && !queue.isEmpty()) {
            node = queue.removeFirst();
            if (!visitedNodes.contains(node)) {
                if (!edges.isEmpty()) {
                    edge = edges.removeFirst();
                    addEdge(edge, query, g, nodeMapping);
                    visitedEdges.add(edge);
                    count++;
                }
                for (int i = 0; i < g.getDegree(node); i++) {
                    edge = g.getNodeEdge(node, i);
                    adjNode = g.getOtherNode(edge, node);
                    if (!visitedNodes.contains(adjNode)) {
                        queue.push(adjNode);
                    } else {
                        //A Cycle
                        if (!visitedEdges.contains(edge)) {                        
                            if (count == size) {
                                return query; 
                            }
                            //edge = edges.removeFirst();
                            addEdge(edge, query, g, nodeMapping);
                            visitedEdges.add(edge);
                            count++;                            
                        }    
                    }
                }
                visitedNodes.add(node);
            }
        }
        
        return query;
    }
        
    
    private void addEdge(int edge, MutableGraph query, Graph g, Map<Integer, Integer> nodeMapping) {
        int nodeAMapped,nodeBMapped; 
        nodeAMapped = getMappedNode(g.getNodeA(edge), query, g, nodeMapping);
        nodeBMapped = getMappedNode(g.getNodeB(edge), query, g, nodeMapping);
        query.addEdge(nodeAMapped, nodeBMapped, g.getEdgeLabel(edge));
    }
    
    private int getMappedNode(int node, MutableGraph query, Graph g, Map<Integer, Integer> nodeMapping) {
        Integer mappedNode = nodeMapping.get(node);
        if (mappedNode == null) {
            mappedNode = query.addNode(g.getNodeLabel(node));
            nodeMapping.put(node, mappedNode);
        }
        return mappedNode; 
    }
    
    @Override
    protected String commandDescription() {
        return "Generate queries used in the experiments";
    }
    
    
    @CommandInput(
        consoleFormat = "-s",
        defaultValue = "5",
        mandatory = false,
        description = "number of queries for each size",
        parameters = ParametersNumber.TWO)         
    public void setNumPerSize(int numPerSize) {
        this.numPerSize = numPerSize;
    }

    @CommandInput(
        consoleFormat = "-from",
        defaultValue = "",
        mandatory = true,
        description = "minimum size of the query to be generated",
        parameters = ParametersNumber.TWO)     
    public void setFromSize(int fromSize) {
        this.fromSize = fromSize;
    }

    @CommandInput(
        consoleFormat = "-to",
        defaultValue = "",
        mandatory = true,
        description = "maximum size of the query to be generated",
        parameters = ParametersNumber.TWO)     
    public void setToSize(int toSize) {
        this.toSize = toSize;
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
        defaultValue = "queries",
        mandatory = false,
        description = "output query file",
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
        consoleFormat = "-directed",
        defaultValue = "false",
        mandatory = false,
        description = "specify if it is a directed or undirected graph",
        parameters = ParametersNumber.ONE)     
    public void setDirected(boolean directed) {
        this.directed = directed;
    }
    
    @CommandInput(
            consoleFormat = "-dbsize",
            defaultValue = "0",
            mandatory = false,
            description = "take only a subset of the graph database",
            parameters = ParametersNumber.TWO)
    public void setNumberOfGraphs(int numberOfGraphs) {
        this.numberOfGraphs = numberOfGraphs;
    }
    
    @CommandInput(
            consoleFormat = "-step",
            defaultValue = "1",
            mandatory = false,
            description = "generate queries only after jumping in the size",
            parameters = ParametersNumber.TWO)
    public void setStep(int step) {
        this.step = step;
    }

    @CommandInput(
        consoleFormat = "-minRes",
        defaultValue = "1",
        mandatory = false,
        description = "minimum number of results per query",
        parameters = ParametersNumber.TWO)
    public void setMinResults(int minResults) {
        this.minResults = minResults;
    }

    @CommandInput(
        consoleFormat = "-mulRatio",
        defaultValue = "1",
        mandatory = false,
        description = "maximum percentage of queries with multiple answers",
        parameters = ParametersNumber.TWO)
    public void setMaxMultiple(float maxMultiple) {
        this.maxMultiple = maxMultiple;
    }
}
