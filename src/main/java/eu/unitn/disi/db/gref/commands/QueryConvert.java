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
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Convert a query file into another format
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class QueryConvert extends Command {
    private String queryFile; 
    private String parserClass;
    private String serializerClass; 
    private String outputFile;
    private boolean directed = false; 

    @Override
    protected void execute() throws ExecutionException {
        GraphParser parser;
        int mask;
        List<Graph> db;
        Graph g; 
        StopWatch watch = new StopWatch();
        GraphFactory factory; 
        GraphParser serializer; 
        
        MyFactory.getDFSCoder();//To load the factories (insane, I know)
        
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(queryFile)); 
             BufferedWriter out = new BufferedWriter(new FileWriter(outputFile, true))) {
            parser = (GraphParser) Class.forName(parserClass).newInstance();
            serializer = (GraphParser) Class.forName(serializerClass).newInstance();
            
            mask = directed? GraphFactory.DIRECTED_GRAPH: GraphFactory.UNDIRECTED_GRAPH;
            factory = GraphFactory.getFactory(mask);
            watch.start();
            db = Arrays.asList(parser.parse(in, factory));
            info("Loaded graph database %s in %dms", queryFile, watch.getElapsedTimeMillis());
            
            info("Writing to file %s", outputFile);
            for (Graph graph : db) {    
                out.append(serializer.serialize(graph) + "\n");
            }
        } catch (IOException ex) {
            throw new ExecutionException("Error while reading the file %s", ex, queryFile);
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException ex) {
            fatal("Graph parser %s cannot be instantiated", parserClass);
        } catch (ParseException ex) {
            throw new ExecutionException("Cannot parse the database file", ex);
        }
    }

    @Override
    protected String commandDescription() {
        return "Convert a query file from a format to another";
    }

    @CommandInput(
        consoleFormat = "-in",
        defaultValue = "",
        mandatory = true,
        description = "query file",
        parameters = ParametersNumber.TWO) 
    public void setQueryFile(String queryFile) {
        this.queryFile = queryFile;
    }
    
    @CommandInput(
        consoleFormat = "-parser",
        defaultValue = "de.parmol.parsers.LineGraphParser",
        mandatory = false,
        description = "parser used to parse queries",
        parameters = ParametersNumber.TWO) 
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }

    @CommandInput(
        consoleFormat = "-serializer",
        defaultValue = "edu.psu.chemxseer.structure.iso.CanonicalDFS",
        mandatory = false,
        description = "parser used for the output format",
        parameters = ParametersNumber.TWO) 
    public void setSerializerClass(String serializerClass) {
        this.serializerClass = serializerClass;
    }

    @CommandInput(
        consoleFormat = "-out",
        defaultValue = "queries",
        mandatory = false,
        description = "output file",
        parameters = ParametersNumber.TWO) 
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
}
