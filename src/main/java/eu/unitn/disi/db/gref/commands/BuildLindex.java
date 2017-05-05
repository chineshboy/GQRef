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

import de.parmol.parsers.GraphParser;
import de.parmol.util.Debug;
import edu.psu.chemxseer.structure.factory.MyFactory;
import edu.psu.chemxseer.structure.postings.Impl.GraphDatabase_OnDisk;
import edu.psu.chemxseer.structure.subsearch.Gindex.SubSearch_Gindex;
import edu.psu.chemxseer.structure.subsearch.Gindex.SubSearch_GindexBuilder;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeatureFactory;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeatureProcessorG;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeaturesWithPostings;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeaturesWoPostings;
import edu.psu.chemxseer.structure.subsearch.Impl.indexfeature.FeaturesWoPostingsRelation;
import edu.psu.chemxseer.structure.subsearch.Interfaces.IFeature;
import edu.psu.chemxseer.structure.subsearch.Lindex.SubSearch_LindexSimple;
import edu.psu.chemxseer.structure.subsearch.Lindex.SubSearch_LindexSimpleBuilder;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * Test LIndex, build and try having supergraphs of a query
 *
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class BuildLindex extends Command {

    private String dbFileName;
    private String baseName;
    private String parserClass; 
    private double minFrequency; 

    @Override
    protected void execute() throws ExecutionException {
        GraphParser dbParser;
                
        MyFactory.getDFSCoder();//To load the factories (insane, I know)
            
        try {
            dbParser = (GraphParser) Class.forName(parserClass).newInstance();
            loadLindex(dbParser, true);
        } catch (IOException ex) {
            error("Index file cannot be read, error at line");
        } catch (ParseException ex) {
            error("Graph database cannot be read");
            throw new ExecutionException(ex);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            error("Cannot instanciate the parser with class %s", parserClass);
            throw new ExecutionException("Parser cannot be instanciated", ex);
        } 
        info("Index built correctly");
    }

    private SubSearch_LindexSimple loadLindex(GraphParser dbParser, boolean build) 
            throws IOException, CorruptIndexException, LockObtainFailedException, ParseException 
    {
        // 0. Create Folder
        SubSearch_LindexSimple index; 
        String temp = baseName + "LindexDF" + "/";
        File folder = new File(temp);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (build) {
            buildFeatures(dbParser, minFrequency);
        }
        
        // 1. Use DF features
        String gIndexPatterns = baseName + "GindexDF/GPatterns";
        FeaturesWoPostings<IFeature> features = FeaturesWoPostings
                .LoadFeaturesWoPostings(gIndexPatterns, MyFactory
                .getFeatureFactory(FeatureFactory.FeatureFactoryType.SingleFeature));
        FeaturesWoPostingsRelation<IFeature> lindexFeatures = FeaturesWoPostingsRelation
                .buildFeaturesWoPostingsRelation(features);
        
        SubSearch_LindexSimpleBuilder builder = new SubSearch_LindexSimpleBuilder();
        if (build) {
            index = builder.buildIndex(lindexFeatures, new GraphDatabase_OnDisk(dbFileName,
                dbParser), temp, dbParser);
        } else {
            index = builder.loadIndex(new GraphDatabase_OnDisk(dbFileName,
                dbParser), temp, dbParser, false);
        }
        return index;
    }
    
    
    private void buildFeatures(GraphParser dbParser, double minFreq) throws IOException, ParseException {
                // 0. Create Folder
        String temp = baseName + "GindexDF/";
        File folder = new File(temp);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        info("Debug level %d", Debug.dlevel);
        
        // 1. Mine Features
        FeaturesWithPostings candidateFeatures = FeatureProcessorG
                .frequentSubgraphMining(dbFileName, temp + "patterns", temp
                + "postings", minFreq, 4, 10, dbParser);
        // FeaturesWithPostings candidateFeatures = new
        // FeaturesWithPostings(temp +
        // "postings",
        // new FeaturesWoPostings(temp + "patterns",
        // MyFactory.getFeatureFactory(FeatureFactoryType.OneFeature)));
        // 2. Build Index
        SubSearch_GindexBuilder builder = new SubSearch_GindexBuilder();
        SubSearch_Gindex gIndex = builder.buildIndex(candidateFeatures,
                new GraphDatabase_OnDisk(dbFileName, dbParser), false, temp,
                temp + "GPatterns", temp + "GPostings", dbParser);
    }
    
    
    @Override
    protected String commandDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        consoleFormat = "-index",
        defaultValue = "",
        mandatory = false,
        description = "base folder name for the index",
        parameters = ParametersNumber.TWO)
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    @CommandInput(
        consoleFormat = "-parser",
        defaultValue = "de.parmol.parsers.LineGraphParser",
        mandatory = false,
        description = "parser class for the graph",
        parameters = ParametersNumber.TWO)
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }

    @CommandInput(
        consoleFormat = "-f",
        defaultValue = "0.01",
        mandatory = false,
        description = "minimum frequency for features",
        parameters = ParametersNumber.TWO)
    public void setMinFrequency(double minFrequency) {
        this.minFrequency = minFrequency;
    }
}
