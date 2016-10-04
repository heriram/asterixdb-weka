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

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class BatchClassifier {
    private String inputModelDir = "/Volumes/USBStorage/dataset";
    private String classifierAlgorithm = "J48";
    private String testFile = "/Volumes/USBStorage/dataset/testData.txt";
    private String outputFile = "/Volumes/USBStorage/dataset/predicted.txt";

    static final Logger LOGGER = Logger.getLogger(BatchClassifier.class.getName());

    public BatchClassifier(String inputModelDir, String classifierAlgorithm, String testFile, String outputFile) {
        this.inputModelDir = inputModelDir;
        this.classifierAlgorithm = classifierAlgorithm;
        this.testFile = testFile;
        this.outputFile = outputFile;
    }

    public Instance predictClass(Classifier model, Instance unlabeledInstance, Instances dataset) throws
            Exception {
        unlabeledInstance.setDataset(dataset);
        Instance labeledInstance = new DenseInstance(unlabeledInstance);
        labeledInstance.setClassValue(model.classifyInstance(unlabeledInstance));
        return labeledInstance;
    }

    public void predictClasses() throws Exception {
        LOGGER.info("Start predicting classes.");
        String modelPath = inputModelDir + "/" + classifierAlgorithm + ".model";
        Classifier model = (Classifier) SerializationHelper.read(modelPath);
        WekaClassifier classifier = new WekaClassifier(model);
        Instances unlabeledData = classifier.loadTestData(testFile);

        // This is where to put the classification results
        Instances labeledData = new Instances(unlabeledData);

        for (int i=0; i<unlabeledData.numInstances(); i++) {
            double classLabel = model.classifyInstance(unlabeledData.instance(i));
            labeledData.instance(i).setClassValue(classLabel);
        }

        classifier.saveLabeledData(labeledData, outputFile);

        LOGGER.info("Done predicting. Result stored at " + outputFile);
    }

    public void predictClasses(String headerFileName) throws Exception {
        LOGGER.info("Start predicting classes.");
        String modelPath = inputModelDir + "/" + classifierAlgorithm + ".model";
        Classifier model = (Classifier) SerializationHelper.read(modelPath);
        WekaClassifier classifier = new WekaClassifier(model);
        Instances unlabeledData = classifier.loadTestData(testFile);

        //InstanceClassifier instanceClassifier = new InstanceClassifier(model, unlabeledData);
        InstanceClassifier instanceClassifier = new InstanceClassifier(modelPath, headerFileName);
        ArrayList<Attribute> attributeInfo = Collections.list(unlabeledData.enumerateAttributes());
        attributeInfo.add(unlabeledData.classAttribute());


        // This is where to put the classification results
        Instances labeledData = new Instances("LabeledWeather", attributeInfo, 0);

        for (int i=0; i<unlabeledData.numInstances(); i++) {
            Instance labeledInstance = unlabeledData.instance(i);
            instanceClassifier.classify(labeledInstance);
            labeledData.add(labeledInstance);
        }

        classifier.saveLabeledData(labeledData, outputFile);

        LOGGER.info("Done predicting. Result stored at " + outputFile);
    }

    public static void main(String[] args) throws Exception {
        String inputModelDir = "/Volumes/USBStorage/dataset";
        String classifierAlgorithm = "RANDOM_FOREST";
        String testFile = "/Volumes/USBStorage/dataset/testData.txt";
        String outputFile = "/Volumes/USBStorage/dataset/predicted.txt";
        String headerFile = "/Volumes/USBStorage/dataset/weather_header.json";

        if (args.length < 2) {
            System.err.println("Usage: BatchClassifier -i inputDir -t /where/to/get/the/testFile.txt -c"
                    + " classfication-algorithm (e.g., J48) -o /where/to/put/the/result.txt");
            System.exit(-1);
        }
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-i")) {
                inputModelDir = args[++i];
            } else if (args[i].equals("-o")) {
                outputFile = args[++i];
            } else if (args[i].equals("-c")) {
                classifierAlgorithm = args[++i];
            } else if (args[i].equals("-t")) {
                testFile = args[++i];
            } else {
                System.err.println("Usage: BatchClassifier -i inputDir -t /where/to/get/the/testFile.txt -c"
                        + " classfication-algorithm (e.g., J48) -o /where/to/put/the/result.txt");
                System.exit(-1);
            }
        }

        BatchClassifier bc = new BatchClassifier(inputModelDir, classifierAlgorithm, testFile, outputFile);
        bc.predictClasses(headerFile);
    }
}
