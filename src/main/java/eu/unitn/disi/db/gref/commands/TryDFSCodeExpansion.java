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

import de.parmol.GSpan.DFSCode;
import de.parmol.GSpan.DataBase;
import de.parmol.GSpan.GSpanEdge;
import de.parmol.GSpan.GSpanGraph;
import de.parmol.graph.ClassifiedGraph;
import de.parmol.graph.Graph;
import de.parmol.graph.GraphFactory;
import de.parmol.parsers.GraphParser;
import de.parmol.util.FragmentSet;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.iso.CanonicalDFS;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.gref.utils.GraphUtilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class TryDFSCodeExpansion extends Command {
    
    private double minFreq; 
    private String dbFileName; 
    private String queryFileName;
    private String baseName; 
    private String parserClass;     
    
    
    @Override
    protected void execute() throws ExecutionException {
        //GraphParser dbParser = MyFactory.getSmilesParser();
        GraphParser dbParser;
        
        try {
            dbParser = (GraphParser) Class.forName(parserClass).newInstance();
//            CanonicalDFS coder = MyFactory.getDFSCoder();
            GraphFactory gFactory = GraphFactory.getFactory(dbParser.getDesiredGraphFactoryProperties() | GraphFactory.CLASSIFIED_GRAPH);
            Graph[] graphs = dbParser.parse(new FileInputStream(dbFileName), gFactory);
//                info(coder.serialize(graphs[i]));
//            }
            Graph query = dbParser.parse(GraphUtilities.readFileToString(new File(queryFileName)), gFactory);
            GSpanEdge[] edges = GraphUtilities.graphToDFSCodeEdges(graphs[0]);
            GSpanEdge[] qEdges = GraphUtilities.graphToDFSCodeEdges(query);
            
            DataBase db = new DataBase(Arrays.asList(graphs), new float[]{(float)minFreq}, new FragmentSet(), gFactory);
            GSpanGraph graph = new GSpanGraph((ClassifiedGraph)graphs[0],db.getNodeRelabeler(), db.getEdgeRelabeler(), gFactory);
            DFSCode code = new DFSCode(qEdges, query, Collections.singleton(graph),db);
            
            subgraph_Mining(code);
            
            
        } catch (InstantiationException| IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(TryGIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            throw new ExecutionException(ex);
        } catch (ParseException ex) {
            throw new ExecutionException(ex);
        }
    }

    @Override
    protected String commandDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @CommandInput(
        consoleFormat = "-f",
        defaultValue = "0.0",
        mandatory = false,
        description = "min Frequency",
        parameters = ParametersNumber.TWO
    )
    public void setMinFreq(double minFreq) {
        this.minFreq = minFreq;
    }
    
    @CommandInput(
        consoleFormat = "-db",
        defaultValue = "",
        mandatory = true,
        description = "file containing the graph database",
        parameters = ParametersNumber.TWO
    )
    public void setDbFileName(String dbFileName) {
        this.dbFileName = dbFileName;
    }

    @CommandInput(
        consoleFormat = "-out",
        defaultValue = "output",
        mandatory = false,
        description = "prefix for the output folder",
        parameters = ParametersNumber.TWO
    )
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @CommandInput(
        consoleFormat = "-parser",
        defaultValue = "de.parmol.parsers.LineGraphParser",
        mandatory = false,
        description = "parser to be used for the input file",
        parameters = ParametersNumber.TWO
    )
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }

    @CommandInput(
        consoleFormat = "-query",
        defaultValue = "",
        mandatory = true,
        description = "query file to test the db",
        parameters = ParametersNumber.TWO
    )
    public void setQueryFileName(String queryFileName) {
        this.queryFileName = queryFileName;
    }
    
    
    //DEBUG CODE To test potentials. 
    private GraphParser parser = MyFactory.getDFSCoder();
    private int maximumFragmentSize = 100;
    private int minMustSelectSize = 1;
    private int numberOfPatterns = 0;
    
    private void subgraph_Mining(DFSCode code) {
        
        if (!code.isMin()) {
            info(code.toString(parser) + " not min");    
        }
        //float[] max = empty;

        float[] my = code.getFrequencies();
        info("  found graph " + code.toString(parser));

        if (code.getSubgraph().getEdgeCount() < maximumFragmentSize) {
            Iterator it = code.childIterator(false, false);
            for (; it.hasNext();) {
                DFSCode next = (DFSCode) it.next();
                numberOfPatterns++;
                // This is edited by Dayu, affect efficiency
                // Calculate new minimum Class Frequency
//                float[] dayuFrequency = new float[m_settings.minimumClassFrequencies.length];
//                for (int i = 0; i < dayuFrequency.length; i++) {
//                    dayuFrequency[i] = (float) java.lang.Math
//                            .sqrt((double) next.getSubgraph().getEdgeCount()
//                            / (double) m_settings.maximumFragmentSize)
//                            * m_settings.minimumClassFrequencies[i];
//                }
                if (next.getSubgraph().getEdgeCount() < this.minMustSelectSize
                //        || (next.isFrequent(dayuFrequency))
                ) {
                    subgraph_Mining(next);
                    //max = getMax(max, a);
                } else {
                    continue; // early pruning
                }
            }
        }
//        if ((!m_settings.closedFragmentsOnly || max == empty || unequal(my, max))
//                && m_settings.checkReportingConstraints(code.getSubgraph(),
//                code.getFrequencies())) {
//            m_frequentSubgraphs.add(code.toFragment());
//        } else {
//            m_settings.stats.earlyFilteredNonClosedFragments++;
//        }
//        return my;
    }
}


