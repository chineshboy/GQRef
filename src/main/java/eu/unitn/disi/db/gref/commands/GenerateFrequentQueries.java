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
import eu.unitn.disi.db.gref.algorithms.index.FrequencyIndex;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate only the top frequent queries. 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class GenerateFrequentQueries extends Command {
    private int numPerSize; 
    private int fromSize;
    private int toSize;
    private int step; 
    private String indexFile; 
    private String parserClass;
    private String outputFile;
    private boolean directed;
    private boolean sortFrequent;
    
    private GraphFactory factory; 
    
    
    @Override
    protected void execute() throws ExecutionException {
        GraphParser parser;
        int mask;
        Graph g; 
        Set<String> queries;
        queries = new LinkedHashSet<>();
        StopWatch watch = new StopWatch();
        
        MyFactory.getDFSCoder();//To load the factories (insane, I know)
        
        try (ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(indexFile))) {
            parser = (GraphParser) Class.forName(parserClass).newInstance();

            mask = directed? GraphFactory.DIRECTED_GRAPH: GraphFactory.UNDIRECTED_GRAPH;
            factory = GraphFactory.getFactory(mask);
            watch.start();
            FrequencyIndex index = (FrequencyIndex) objIn.readObject();
            info("Loaded index %s in %dms", indexFile, watch.getElapsedTimeMillis());
            Map<Integer,List<ReformulatedQuery>> queriesPerSize = new HashMap<>(); 
            List<ReformulatedQuery> reformulations; 
            int edgeNum; 
            
            info("Index size: %d", index.size());
            for (ReformulatedQuery query : index.getIndex().values()) {
                edgeNum = query.getEdgeCount();
                if (edgeNum >= fromSize && edgeNum < toSize) {
                    reformulations = queriesPerSize.get(edgeNum);
                    if (reformulations == null) {
                        reformulations = new ArrayList<>();
                    }
                    reformulations.add(query);
                    queriesPerSize.put(edgeNum, reformulations);
                }   
            }
            for (int size = fromSize; size <= toSize; size += step) {
                reformulations = queriesPerSize.get(size);
                if (reformulations != null) {
                    if (sortFrequent) {
                        Collections.sort(reformulations, new Comparator<ReformulatedQuery>() {
                            @Override
                            public int compare(ReformulatedQuery o1, ReformulatedQuery o2) {
                                int r1 = o1.resultsNumber();
                                int r2 = o2.resultsNumber();
                                if (r1 > r2) {
                                    return -1; 
                                } else if (r1 < r2) {
                                    return 1;
                                } 
                                return 0;
                            }

                        });
                    } else {
                        Collections.shuffle(reformulations);
                    }
                    for (int i = 0; i < numPerSize && i < reformulations.size(); i++) {
                        debug("Inserted query %s, size %d, results: %d", reformulations.get(i),  reformulations.get(i).getEdgeCount(), reformulations.get(i).resultsNumber());
                        queries.add(reformulations.get(i).toString());
                    }
                } else {
                    warn("No frequent queries of size %d", size);
                }
            }
        } catch (IOException ex) {
            throw new ExecutionException("Error while reading the file %s", ex, indexFile);
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            fatal("Graph parser %s cannot be instantiated", parserClass);
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
        
    @Override
    protected String commandDescription() {
        return "Generate frequent queries used in the experiments";
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
        consoleFormat = "-i",
        defaultValue = "",
        mandatory = true,
        description = "index input file",
        parameters = ParametersNumber.TWO)     
    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    @CommandInput(
        consoleFormat = "-sort",
        defaultValue = "false",
        mandatory = false,
        description = "sort by decreasing frequency",
        parameters = ParametersNumber.ONE)     
    public void setSortFrequent(boolean sortFrequent) {
        this.sortFrequent = sortFrequent;
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
    
}
