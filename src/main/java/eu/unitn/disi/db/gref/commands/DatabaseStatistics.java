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
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Print statistics about an input graph database
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class DatabaseStatistics extends Command {
    private String dbFile;
    private String parserClass;

    
    
    @Override
    protected void execute() throws ExecutionException {
        GraphParser parser;
        List<Graph> db;
        int maxSize = 0, minSize = Integer.MAX_VALUE;
        int minNodes = Integer.MAX_VALUE, maxNodes = 0; 
        Graph g; 
        GraphFactory factory;
        double avgDensity = 0, avgNodes = 0, avgEdges = 0;
        Set<Integer> nodeLabels = new HashSet<>(); 
        Set<Integer> edgeLabels = new HashSet<>(); 
        
        
        MyFactory.getDFSCoder();//To load the factories (insane, I know)
        
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(dbFile))) {
            parser = (GraphParser) Class.forName(parserClass).newInstance();

            factory = GraphFactory.getFactory(parser.getDesiredGraphFactoryProperties());
            db = Arrays.asList(parser.parse(in, factory));
            
            for (Graph graph : db) {
                avgDensity += 2.0 * graph.getEdgeCount()/(graph.getNodeCount() * (graph.getNodeCount() - 1.0));
                
                for (int i = 0; i < graph.getNodeCount(); i++) {
                    nodeLabels.add(graph.getNodeLabel(i));
                }
                for (int i = 0; i < graph.getEdgeCount(); i++) {
                    edgeLabels.add(graph.getEdgeLabel(i));
                }
                avgNodes += graph.getNodeCount();
                avgEdges += graph.getEdgeCount();
                if (graph.getEdgeCount() > maxSize) {
                    maxSize = graph.getEdgeCount();
                }
                if (graph.getEdgeCount() < minSize) {
                    minSize = graph.getEdgeCount();
                }                
                if (graph.getNodeCount() > maxNodes) {
                    maxNodes = graph.getNodeCount();
                }
                if (graph.getNodeCount() < minNodes) {
                    minNodes = graph.getNodeCount();
                }                
            }
            
            info("Number of graphs: %d", db.size());
            info("Edge labels: %d", edgeLabels.size());
            info("Node labels: %d", nodeLabels.size());
            info("Min edges: %d", minSize);
            info("Avg number of edges: %f", avgEdges/db.size());
            info("Max edges: %d", maxSize);
            info("Min nodes: %d", minNodes);
            info("Avg number of nodes: %f", avgNodes/db.size());
            info("Max nodes: %d", maxNodes);
            info("Average density: %f", avgDensity/db.size());
            
            
        } catch (IOException ex) {
            throw new ExecutionException("Error while reading the file %s", ex, dbFile);
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            fatal("Graph parser %s cannot be instantiated", parserClass);
        } catch (ParseException ex) {
            throw new ExecutionException("Cannot parse the database file", ex);
        }
    }

    @Override
    protected String commandDescription() {
        return "Print a bunch of statistics about a graph database";
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
        consoleFormat = "-parser",
        defaultValue = "de.parmol.parsers.LineGraphParser",
        mandatory = false,
        description = "parser used for the input graph",
        parameters = ParametersNumber.TWO) 
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }
}
