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
package org.apache.asterix.external.feed.ml.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.asterix.external.feed.ml.classification.WekaClassifier;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.json.JSONInstances;
import weka.core.json.JSONNode;

public class FeatureReader {
    protected static final Logger LOGGER = Logger.getLogger(FeatureReader.class.getName());
    int classAttributeIndex = -1;

    public FeatureReader(){
    }

    public BufferedReader readDataFile(String path) {
        BufferedReader inputReader = null;

        try {
            inputReader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + path);
        }

        return inputReader;
    }

    public Instances getWekaInstances(String filename, boolean isSetClassIndex) throws IOException {
        Instances data = new Instances(readDataFile(filename));
        if (isSetClassIndex) {
            this.classAttributeIndex = data.numAttributes() - 1;
            data.setClassIndex(classAttributeIndex);
        }
        return data;
    }


    public Instances getWekaInstances(String filename) throws IOException {
        return getWekaInstances(filename, false);
    }

    public int getClassAttributeIndex() {
        return classAttributeIndex;
    }

    public static void main(String[] args) throws Exception {
        FeatureReader featureReader = new FeatureReader();
        boolean useClassIndex = true;
        Instances data = featureReader.getWekaInstances("/Volumes/USBStorage/dataset/weather.txt", useClassIndex);

        StringBuffer stringBuffer = new StringBuffer();
        JSONNode json = JSONInstances.toJSON(data);
        json.toString(stringBuffer);

        String features = ArffDataHandler.toAdmString(data);

        System.out.println(features);
        System.out.println(ArffDataHandler.getFeatureRecordType(data, useClassIndex));

        System.out.print("Start training the classifier...");
        WekaClassifier classifier = new WekaClassifier("J48");
        classifier.trainClassifier(data, featureReader.getClassAttributeIndex());
        System.out.println("done.");

        double testValues[] = new double[] {0, 71, 100, 0, -1};
        Instance testData = new DenseInstance(1.0, testValues);
        testData.setDataset(data);

        String classPred = classifier.classify(data, testData);
        System.out.print("Class for the test data: [" + classPred + "]");

    }
}
