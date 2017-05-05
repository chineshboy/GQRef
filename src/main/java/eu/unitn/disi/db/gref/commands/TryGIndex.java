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

import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.postings.Impl.GraphDatabase_OnDisk;
import edu.psu.chemxseer.structure.subsearch.Gindex.SubSearch_Gindex;
import edu.psu.chemxseer.structure.subsearch.Gindex.SubSearch_GindexBuilder;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeatureProcessorG;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeaturesWithPostings;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class TryGIndex extends Command {

    private double minFreq;
    private String dbFileName;
    private String baseName;
    private String parserClass;

    @Override
    protected void execute() throws ExecutionException {
        //GraphParser dbParser = MyFactory.getSmilesParser();
        int flag = 0;
        String temp = baseName + "GindexDF" + flag + "/";
        SubSearch_GindexBuilder builder = new SubSearch_GindexBuilder();
        GraphParser dbParser;

        // 0. Create Folder
        if (flag == 0) {
            temp = baseName + "GindexDF/";
        }

        // FeaturesWithPostings candidateFeatures = new
        // FeaturesWithPostings(temp +
        // "postings",
        // new FeaturesWoPostings(temp + "patterns",
        // MyFactory.getFeatureFactory(FeatureFactoryType.OneFeature)));
        // 2. Build Index
        try {
            dbParser = (GraphParser) Class.forName(parserClass).newInstance();

            File folder = new File(temp);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            // 1. Mine Features
            FeaturesWithPostings candidateFeatures = FeatureProcessorG
                    .frequentSubgraphMining(dbFileName, temp + "patterns", temp
                    + "postings", minFreq, 0, 100, dbParser);
            SubSearch_Gindex gIndex = builder.buildIndex(candidateFeatures,
                    new GraphDatabase_OnDisk(dbFileName, dbParser), false, temp,
                    temp + "GPatterns", temp + "GPostings", dbParser);
            gIndex.getAnswer(null, null);
            
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(TryGIndex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ParseException ex) {
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
    parameters = ParametersNumber.TWO)
    public void setMinFreq(double minFreq) {
        this.minFreq = minFreq;
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
        consoleFormat = "-out",
    defaultValue = "output",
    mandatory = false,
    description = "prefix for the output folder",
    parameters = ParametersNumber.TWO)
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @CommandInput(
        consoleFormat = "-parser",
    defaultValue = "de.parmol.parsers.LineGraphParser",
    mandatory = false,
    description = "parser to be used for the input file",
    parameters = ParametersNumber.TWO)
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }
}
