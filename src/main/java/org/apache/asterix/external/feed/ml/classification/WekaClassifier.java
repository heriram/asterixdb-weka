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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class WekaClassifier {

    private Classifier model;
    private boolean modelTrained = false;
    private Attribute classAttribute;

    public WekaClassifier(String name) {
        this.model = Classifiers.valueOf(name).model;
        this.modelTrained = false;
    }

    public WekaClassifier(Classifier model) {
        this.model = model;
        this.modelTrained = false;
    }

    public Instances loadTrainingData(String fileName) throws IOException {
        BufferedReader inputReader = null;

        inputReader = new BufferedReader(new FileReader(fileName));
        Instances data = new Instances(inputReader);
        data.setClassIndex(data.numAttributes() - 1);
        this.classAttribute = data.classAttribute();

        return data;
    }

    public Instances loadTestData(String fileName) throws IOException {
        BufferedReader inputReader = null;

        inputReader = new BufferedReader(new FileReader(fileName));
        Instances data = new Instances(inputReader);
        data.setClassIndex(data.numAttributes() - 1);
        this.classAttribute = data.classAttribute();
        return data;
    }

    public void trainClassifier(Instances trainingSet, int classAttributeIndex, String modelPath) throws Exception {
        model.buildClassifier(trainingSet);
        trainingSet.setClassIndex(classAttributeIndex);
        this.classAttribute = trainingSet.classAttribute();
        this.modelTrained = true;
        if (modelPath != null) {
            SerializationHelper.write(modelPath, model);
        }
    }

    public void trainClassifier(Instances trainingSet, int classAttributeIndex) throws Exception {
        trainClassifier(trainingSet, classAttributeIndex, null);
    }


    public String classify(Instances trainingSet, Instance data) throws Exception {
        return classify(trainingSet, data, null);
    }

    public String classify(Instances trainingSet, Instance data, String modelPath) throws Exception {
        data.setDataset(trainingSet);

        if (modelPath != null) {
            this.model = (Classifier) SerializationHelper.read(modelPath);
        }
        double prediction = model.classifyInstance(data);
        return classAttribute.value((int)prediction);
    }


    public void saveLabeledData(Instances labeledData, String outputFile) throws IOException {
        // save labeled data
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(outputFile));
        writer.write(labeledData.toString());
        writer.newLine();
        writer.flush();
        writer.close();
    }

    public enum Classifiers {
        J48(new J48()), // a decision tree
        PART(new PART()),
        DECISION_TABLE(new DecisionTable()), //decision table majority classifier
        DECISIONSTUMP(new DecisionStump()), //one-level decision tree
        RANDOM_FOREST(new RandomForest()), // Random Forest
        RANDOM_TREE(new RandomTree()), // Random Tree
        NAIVE_BAYES(new NaiveBayes()); // Naive Bayes

        Classifier model;

        Classifiers(Classifier model) {
            this.model = model;
        }

        public Classifier getModel() {
            return model;
        }
    }

}
