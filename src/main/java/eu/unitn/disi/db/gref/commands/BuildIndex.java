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

import de.parmol.Settings;
import de.parmol.parsers.GraphParser;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.gref.algorithms.index.FrequencyIndex;
import eu.unitn.disi.db.gref.algorithms.index.GIndexBuild;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This command builds the gindex [1] and stores it in a file. 
 * The index is stored differently from what declaered in the paper in order 
 * to be easily used in our framework. 
 * 
 * [1] Graph indexing: A frequent structure based approach (2004) by X. Yan, P. Yu, J. Han
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class BuildIndex extends Command {
    private double minSupport;
    private int maxGraphSize; 
    private String outputFile; 
    private String db; 
    private String parserClass; 
    private int verbosity;
    
    @Override
    protected void execute() throws ExecutionException {
        if (minSupport > 1.0 && minSupport < 0) {
            throw new ExecutionException("Min support must be between 0 and 1");
        }
        
        String[] args = {
            "-minimumFrequencies=" + (minSupport),
            "-maximumFragmentSize=" + maxGraphSize,
            "-graphFile=" + db,
            "-findTreesOnly=true",
            "-closedFragmentsOnly=false",
            "-outputFile=temp",
            "-parserClass=" + parserClass,
            "-serializerClass=edu.psu.chemxseer.structure.iso.CanonicalDFS",
            "-memoryStatistics=false", 
            "-debug=" + verbosity
        };
        FrequencyIndex index;
        try (ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            GIndexBuild indexBuilder = new GIndexBuild(new Settings(args));
            indexBuilder.setUp();
            info("Start building index");
            indexBuilder.startMining();
            
            index = indexBuilder.getIndex();
            info("Starting serialization");
            writer.writeObject(index);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(BuildIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BuildIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(BuildIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected String commandDescription() {
        return "Build gIndex and stores in a file";
    }

    @CommandInput(
        consoleFormat = "-s",
        defaultValue = "0.1",
        mandatory = true,
        description = "specify the minimu support for the index (between 0 and 1)",
        parameters = ParametersNumber.TWO)
    public void setMinSupport(double minSupport) {
        this.minSupport = minSupport;
    }


    @CommandInput(
        consoleFormat = "-db",
        defaultValue = "",
        mandatory = true,
        description = "file containing the graph database",
        parameters = ParametersNumber.TWO)
    public void setDbFileName(String dbFileName) {
        this.db = dbFileName;
    }

    @CommandInput(
        consoleFormat = "-o",
        defaultValue = "index.dat",
        mandatory = false,
        description = "index file to be created",
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
        consoleFormat = "-max",
        defaultValue = "100",
        mandatory = false,
        description = "maximum reformulated graph size",
        parameters = ParametersNumber.TWO) 
    public void setMaxGraphSize(int maxGraphSize) {
        this.maxGraphSize = maxGraphSize;
    }
    
    @CommandInput(
        consoleFormat = "-v",
        defaultValue = "0",
        mandatory = false,
        description = "debug level",
        parameters = ParametersNumber.TWO) 
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }
}
