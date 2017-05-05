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

import de.parmol.graph.GraphFactory;
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.postings.Impl.GraphDatabase_OnDisk;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.gref.algorithms.GraphClustering;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the graph clustering results. 
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class Clustering extends Command {
    private String dbFileName;    
    
    @Override
    protected void execute() throws ExecutionException {
        GraphClustering algo;
        GraphParser dbParser;

        try {
            MyFactory.getDFSCoder();//Used to load the static factories
            dbParser = (GraphParser) Class.forName("de.parmol.parsers.LineGraphParser").newInstance();
            
            GraphDatabase_OnDisk dbD = new GraphDatabase_OnDisk(dbFileName, dbParser);
            GraphFactory gFactory = GraphFactory.getFactory(dbParser.getDesiredGraphFactoryProperties() | GraphFactory.CLASSIFIED_GRAPH);
            
            algo = new GraphClustering();
            algo.setDb(dbD.loadAllGraphs());
            algo.setK(10);
            
            algo.compute();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(TryGIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected String commandDescription() {
        return "Perform graph clustering";
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

    
}
