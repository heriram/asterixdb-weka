/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.asterix.external.feed.ml.classification;

import java.io.File;
import java.util.logging.Logger;

import org.apache.asterix.external.feed.ml.tools.AsterixWekaDataHandler;
import org.apache.asterix.external.feed.ml.tools.FeatureReader;
import org.apache.asterix.external.feed.ml.tools.ResourceHelper;
import weka.core.Instances;
import weka.core.converters.JSONSaver;

public class ClassificationModelBuilder {
    private String inputArffFile;
    private String inputFileName; // Name without extension

    private String outputDir;
    private String classifierAlgorithm;

    private final FeatureReader featureReader = new FeatureReader();
    static final Logger LOGGER = Logger.getLogger(ClassificationModelBuilder.class.getName());

    private final JSONSaver jsonSaver = new JSONSaver();


    public ClassificationModelBuilder (String inputArffFile, String outputDir, String classifierAlgorithm) {
        this.inputArffFile = inputArffFile;
        this.inputFileName = inputArffFile.substring(0, inputArffFile.lastIndexOf('.'));
        this.outputDir = outputDir;
        this.classifierAlgorithm = classifierAlgorithm;
    }

    private void storeFeatureHeader(Instances dataset) throws Exception {
        int ext_index = inputArffFile.lastIndexOf('.');
        // The name of the JSON file for the header
        String featureHeaderFile =  inputFileName + "_header.json";

        // Create an empty dataset based on the input to get the header
        Instances headerInstances = new Instances(dataset, 0);
        int classIndex = dataset.classIndex();
        if (classIndex == -1) { // If not set, use default value
            classIndex = dataset.numAttributes() - 1;
        }

        jsonSaver.setFile(new File(featureHeaderFile));
        jsonSaver.setInstances(headerInstances);
        jsonSaver.setClassIndex("" + classIndex);
        jsonSaver.writeBatch();
    }

    public void buildModel() throws Exception {
        Instances data = featureReader.getWekaInstances(inputArffFile, true);
        LOGGER.info("Start training the classifier.");
        WekaClassifier classifier = new WekaClassifier(classifierAlgorithm);
        String modelFile = outputDir + "/" + classifierAlgorithm + ".model";
        classifier.trainClassifier(data, featureReader.getClassAttributeIndex(), modelFile);
        LOGGER.info("Done training. Model stored at " + modelFile);

        // Store the header as JSON file
        data.setClassIndex(featureReader.getClassAttributeIndex());
        storeFeatureHeader(data);


        // Store the Asterix DDL file
        AsterixWekaDataHandler.writeAsterixDDL(inputFileName + ".ddl", data);
    }

    public static void main(String[] args) throws Exception {
        //String DATA_DIR = ResourceHelper.getResourcePath("data");
        String DATA_DIR = "src/main/resources/data/";
        String DOMAIN = ResourceHelper.configLookup("domain");
        String CLASSIFIER_ALGORITHM = ResourceHelper.configLookup("classifier-algorithm");

        String modelDir = DATA_DIR + "models";
        String arffFile = DATA_DIR + DOMAIN + ".arff";

        /*
        if (args.length < 2) {
            System.err.println("Usage: ClassificationModelBuilder -f inputfile.arff -o /where/to/put/the/model -c"
                    + " classfication-algorithm (e.g., J48)");
            System.exit(-1);
        }
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-f")) {
                arffFile = args[++i];
            } else if (args[i].equals("-o")) {
                modelDir = args[++i];
            } else if (args[i].equals("-c")) {
                classifierAlgorithm = args[++i];
            } else {
                System.err.println("Usage: ClassificationModelBuilder -f inputfile.arff -o /where/to/put/the/model -c"
                        + " classfication-algorithm (e.g., J48)");
                System.exit(-1);
            }
        }
        */

        ClassificationModelBuilder cmb = new ClassificationModelBuilder(arffFile, modelDir, CLASSIFIER_ALGORITHM);
        cmb.buildModel();
    }
}
