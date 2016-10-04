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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.asterix.external.feed.ml.tools.WekaJSONLoader;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.json.JSONInstances;

public class InstanceClassifier {
    private Classifier classifierModel;
    private Instances dataset;
    private final Map<String, Integer> attributeIndexes = new HashMap<>();

    private final WekaJSONLoader wekaJsonLoader = new WekaJSONLoader();

    public InstanceClassifier(String modelFileName, String featureHeaderFileName) {
        try {
            this.classifierModel =  (Classifier) SerializationHelper.read(modelFileName);
            this.dataset = readFeatureHeaderToInstances(featureHeaderFileName);
            initializeFieldPositions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InstanceClassifier(InputStream modelFileName, InputStream featureHeaderFileName) {
        try {
            this.classifierModel =  (Classifier) SerializationHelper.read(modelFileName);
            this.dataset = readFeatureHeaderToInstances(featureHeaderFileName);
            initializeFieldPositions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeFieldPositions() {
        Enumeration<Attribute> attributeEnumeration = dataset.enumerateAttributes();
        int classIndex = dataset.classIndex();
        boolean hasClassAttribute = false;
        int index = 0;
        while (attributeEnumeration.hasMoreElements()) {
            Attribute attribute = attributeEnumeration.nextElement();
            attributeIndexes.put(attribute.name(), index);
            hasClassAttribute = (classIndex == index);
            index++;
        }

        if (!hasClassAttribute) {
            attributeIndexes.put(dataset.classAttribute().name(), classIndex);
        }
    }

    public int getAttributeIndex(String fieldName) {
        return attributeIndexes.get(fieldName) == null ? -1 : attributeIndexes.get(fieldName);
    }

    public Instances getDatasetHeader() {
        return dataset;
    }

    private Instances loadFeatureHeader()
            throws Exception {
        Instances instancesHeader = JSONInstances.toInstances(wekaJsonLoader.getRootNode());
        int classIndex = instancesHeader.classIndex();
        if (classIndex == -1) {
            classIndex = wekaJsonLoader.getClassIndex();
        }
        instancesHeader.setClassIndex(classIndex);
        return instancesHeader;
    }

    public Instances readFeatureHeaderToInstances(String featureHeaderFileName) throws Exception {
        wekaJsonLoader.reset();
        wekaJsonLoader.setSource(new File(featureHeaderFileName));
        return loadFeatureHeader();
    }

    public Instances readFeatureHeaderToInstances(InputStream featureHeaderInputStream) throws Exception {
        wekaJsonLoader.reset();
        wekaJsonLoader.setSource(featureHeaderInputStream);
        return loadFeatureHeader();
    }

    public String classify(Instance instance) throws Exception {
        instance.setDataset(dataset);
        double label = classifierModel.classifyInstance(instance);
        instance.setClassValue(label);
        Attribute classAttribute = dataset.classAttribute();
        return classAttribute.value((int)label);
    }

}
