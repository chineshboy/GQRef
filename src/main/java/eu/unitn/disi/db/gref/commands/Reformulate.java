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
package eu.unitn.disi.db.gref.commands;

import de.parmol.graph.Graph;
import de.parmol.graph.GraphFactory;
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.iso.CanonicalDFS;
import edu.psu.chemxseer.structure.postings.Impl.GraphDatabase_OnDisk;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.command.util.StopWatch;
import eu.unitn.disi.db.gref.algorithms.GQRExact;
import eu.unitn.disi.db.gref.algorithms.GQRIndex;
import eu.unitn.disi.db.gref.algorithms.GQRNaive;
import eu.unitn.disi.db.gref.algorithms.GQRPruning;
import eu.unitn.disi.db.gref.algorithms.LatticeAlgorithm;
import eu.unitn.disi.db.gref.algorithms.MinimalSupergraphs;
import eu.unitn.disi.db.gref.algorithms.QueryProcessing;
import eu.unitn.disi.db.gref.algorithms.ReformulationAlgorithm;
import eu.unitn.disi.db.gref.lattice.Query;
import eu.unitn.disi.db.gref.lattice.ReformulatedQuery;
import eu.unitn.disi.db.gref.lattice.ReformulationLattice;
import eu.unitn.disi.db.gref.utils.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command that produces reformulations and test performance
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class Reformulate extends Command {

    private String dbFileName;
    private String queryFileName;
    private String outputFile;
    private String indexPath; 
    private float lambda;
    private int k;
    private int algorithm;
    private int numberOfGraphs;
    private String parserClass;
    private String resultFile;  

    private enum RefAlgorithm {
        GREEDY_BF("Greedy_BF"),
        FAST_MMPG("Fast_MMPG"),
        K_FREQ("k-freq"), 
        INDEXED_MMPG("Indexed_MMPG"), 
        MIN_SUP("Indexed_MinSup"), 
        COMPARISON("Comparison");
        
        String name; 
        
        RefAlgorithm(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    
    @Override
    protected void execute() throws ExecutionException {
        GraphParser dbParser;
        Graph query;
        ReformulationAlgorithm algo = null;
        StopWatch watch = new StopWatch();
        Set<ReformulatedQuery> results;
        long queryTime;
        Graph[] gdb;
        String line;
        StringBuilder resString, refMatches; 
        CanonicalDFS queryParser;
        int lineNo = 0; 
        int dbSize, coverage;
        RefAlgorithm algType; 
        
        try (BufferedReader queryIn = new BufferedReader(new FileReader(queryFileName));
             BufferedInputStream in = new BufferedInputStream(new FileInputStream(dbFileName));
             BufferedWriter output = new BufferedWriter(new FileWriter(outputFile, true))) {
            queryParser = MyFactory.getDFSCoder();//To load the factories (insane, I know)
            dbParser = (GraphParser) Class.forName(parserClass).newInstance();
            algType = RefAlgorithm.values()[algorithm - 1];
            
            
            //GraphDatabase_OnDisk dbD = new GraphDatabase_OnDisk(dbFileName, dbParser);
            GraphFactory gFactory = GraphFactory.getFactory(dbParser.getDesiredGraphFactoryProperties() | GraphFactory.CLASSIFIED_GRAPH);
            Graph[] dbD = dbParser.parse(in, gFactory);
            if (numberOfGraphs > 0 && numberOfGraphs < dbD.length) {
                dbSize = numberOfGraphs;
                gdb = Arrays.copyOf(dbD, numberOfGraphs);
            } else {
                gdb = dbD;
                dbSize = gdb.length;
            }
            dbD = null;
            

            while ((line = queryIn.readLine()) != null) {
                line = line.trim();
                if (!"".equals(line) && !line.startsWith("#")) {
                    lineNo++;
                    query = queryParser.parse(line, gFactory);
                    watch.start();

                    
                    info("Time to load the database into memory: %dms", watch.getElapsedTimeMillis());
                    info("Size of the graph database: %d", gdb.length);

                //Step 1: Find the answers to the query and the mapping.
                    info("Executing query: %s", line);
                    info("Query edge number: %d", query.getEdgeCount());
                    info("Query node number: %d", query.getNodeCount());

                //lastNodeId = query.getNodeCount() - 1;
                    //DEBUG: Remove! 
    //                for (int i = 0; i < query.getEdgeCount(); i++) {
    //                    debug("Query edge: %d(%d) - [%d] - %d(%d)", query.getNodeA(i), query.getNodeLabel(query.getNodeA(i)), query.getEdgeLabel(i), query.getNodeB(i), query.getNodeLabel(query.getNodeB(i)));
    //                }
                    QueryProcessing qProc = new QueryProcessing();
                    qProc.setGdb(gdb);
                    qProc.setQuery(new Query(query));
                    watch.reset();
                    qProc.compute();
                    queryTime = watch.getElapsedTimeMillis();
                    info("Time to answer the query: %dms", queryTime);


                    //Step 1: Find the answers to the query and the mapping. 
                    switch (algType) {
                        case GREEDY_BF: //Exact
                            algo = new GQRExact();
                            ((GQRExact)algo).setLambda(lambda);
                            break;
                        case FAST_MMPG: //Pruning
                            algo = new GQRPruning(); 
                            ((GQRExact)algo).setLambda(lambda);
                            break;
                        case INDEXED_MMPG: 
                            algo = new GQRIndex();
                            ((GQRExact)algo).setLambda(lambda);
                            break;
                        case K_FREQ: 
                            algo = new GQRNaive(); 
                            ((GQRExact)algo).setLambda(0);
                            break;
                        case MIN_SUP: 
                            algo = new MinimalSupergraphs(new GraphDatabase_OnDisk(dbFileName, dbParser));
                            ((MinimalSupergraphs)algo).setQuery(query);
                            ((MinimalSupergraphs)algo).setIndexPath(indexPath);
                            break;
                        case COMPARISON: 
                            throw new ExecutionException("This method been used to test pruning code correctness");
                    }
                    algo.setK(k);

                    if (algo instanceof LatticeAlgorithm) {
                        ((LatticeAlgorithm)algo).setLattice(qProc.getLattice());
                        ((LatticeAlgorithm)algo).setDb(qProc.getResults());
                    }
                            
//                        case COMPARISON: //DEBUG: Comparison
//                            warn("This mode is only for debug purpose");
//                            name = "CHECK";
//                            algo = new GQRPruning();
//                            algo.setDb(gdb);
//                            algo.setK(k);
//                            ((GQRExact)algo).setLambda(lambda);
//                            ((GQRExact)algo).setLattice(lattice);
//                            algo.compute();
//                            results = algo.getS();
//                            algo = new GQRExact();
//                            qProc.compute();
//                            algo.setDb(gdb);
//                            algo.setK(k);
//                            ((GQRExact)algo).setLambda(lambda);
//                            ((GQRExact)algo).setCheckQueries(results);
//                            ((GQRExact)algo).setLattice(qProc.getLattice());
//                            break;
//                        default:
//                            throw new ExecutionException("Algorithm %d is not a valid algorithm", algorithm);
//                    }
                    
                    algo.compute();
                    results = algo.getS();
                    info("Reformulations: %s", results.toString());
                    coverage = ReformulationAlgorithm.coverage(results);
                    refMatches = new StringBuilder();
                    for (ReformulatedQuery q : results) {
                        refMatches.append(q.resultsNumber()).append("|");
                    }
                    output.append(
                        algType + ","
                        + algorithm + ","
                        + dbSize + ","
                        + gdb.length + "," //taken graphs
                        + qProc.getResults().length + "," //Number of query results
                        + query.getNodeCount() + ","
                        + query.getEdgeCount() + ","
                        + queryTime + ","
                        + k + ","
                        + lambda + ","
                        + algo.getAlgorithmTime() + ","
                        + ReformulationAlgorithm.ovelap(results) + ","
                        + coverage + ","
                        + algo.getDiversity() + ","
                        + algo.getNumberOfExpansions() + ","
                        + algo.getNumberOfReformulations() + ","
                        + (coverage + lambda * algo.getDiversity()) + ","
                        + (refMatches.length() > 0? refMatches.substring(0, refMatches.length() - 1) : "") + "\n"
                    );
                   
                    if (!"".equals(resultFile)) {
                        resString = new StringBuilder();
                        resString.append(
                                dbParser.serialize(query)
                        );
                        for (ReformulatedQuery res : results) {
                            resString.append("<EOG>").append(dbParser.serialize(res));
                        }
                        resString.append("<EOQ>");
                        Utils.writeStringToFile(resString.toString(), resultFile, true);
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(Reformulate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Reformulate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Reformulate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(Reformulate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ArrayIndexOutOfBoundsException ex) {
            error("Algorithm %d does not exists", algorithm);
        }
    }

    @Override
    protected String commandDescription() {
        return "Find the reformulations of a query and test the framework";
    }

    @CommandInput(
            consoleFormat = "-db",
            defaultValue = "",
            mandatory = true,
            description = "file containing the graph database",
            parameters = ParametersNumber.TWO)
    public void setDbFileName(String dbFileName) {
        this.dbFileName = dbFileName;
    }

    @CommandInput(
            consoleFormat = "-q",
            defaultValue = "",
            mandatory = true,
            description = "file containing the query",
            parameters = ParametersNumber.TWO)
    public void setQueryFileName(String queryFileName) {
        this.queryFileName = queryFileName;
    }

    @CommandInput(
            consoleFormat = "-l",
            defaultValue = "0.5",
            mandatory = false,
            description = "diversification factor lambda",
            parameters = ParametersNumber.TWO)
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    @CommandInput(
            consoleFormat = "-k",
            defaultValue = "10",
            mandatory = false,
            description = "number of reformulations",
            parameters = ParametersNumber.TWO)
    public void setK(int k) {
        this.k = k;
    }

    @CommandInput(
            consoleFormat = "-a",
            defaultValue = "1",
            mandatory = false,
            description = "algorithm to use (1 = exact, 2 = pruning, 3 = index, 4 = k-freq, 5 = LIndex)",
            parameters = ParametersNumber.TWO)
    public void setAlgorithm(int algorithm) {
        this.algorithm = algorithm;
    }

    @CommandInput(
            consoleFormat = "-o",
            defaultValue = "stats.csv",
            mandatory = false,
            description = "output file for statistics",
            parameters = ParametersNumber.TWO)
    public void setOutput(String output) {
        this.outputFile = output;
    }

    @CommandInput(
            consoleFormat = "-res",
            defaultValue = "",
            mandatory = false,
            description = "output file for reformulations",
            parameters = ParametersNumber.TWO)
    public void setReformulationDir(String results) {
        this.resultFile = results;
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
            consoleFormat = "-dbsize",
            defaultValue = "0",
            mandatory = false,
            description = "take only a subset of the graph database",
            parameters = ParametersNumber.TWO)
    public void setNumberOfGraphs(int numberOfGraphs) {
        this.numberOfGraphs = numberOfGraphs;
    }

    @CommandInput(
            consoleFormat = "-index",
            defaultValue = "",
            mandatory = false,
            description = "sepecify the index path, if needed",
            parameters = ParametersNumber.TWO)
    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }
}
