/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.asterix.external.feed.ml.classification.udf.tests;

import java.io.InputStream;
import java.util.Properties;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.feed.ml.classification.InstanceClassifier;
import org.apache.asterix.external.feed.ml.classification.udf.WekaClassifierFunction;
import org.apache.asterix.external.feed.ml.classification.udf.tests.CapitalFinderFunction;
import org.apache.asterix.external.library.java.JObjects.JRecord;
import org.apache.asterix.external.library.java.JObjects.JString;
import org.apache.asterix.external.library.java.JTypeTag;
import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.IAType;

import weka.core.DenseInstance;
import weka.core.Instance;

public class ClassifierTestFunction implements IExternalScalarFunction {
    private static Properties capitalList;
    private static final String NOT_FOUND = "NOT_FOUND";
    private JString capital;

    private InstanceClassifier instanceClassifier;
    private Instance instanceHolder;
    private int classIndex;

    @Override
    public void initialize(IFunctionHelper functionHelper) throws Exception {
        String modelFile = "data/RANDOM_FOREST.model";
        String featureHeader = "data/weather_header.json";

        InputStream headerInputStream = WekaClassifierFunction.class.getClassLoader().getResourceAsStream(featureHeader);
        InputStream modelInputStream = WekaClassifierFunction.class.getClassLoader().getResourceAsStream(modelFile);

        instanceClassifier = new InstanceClassifier(modelInputStream, headerInputStream);
        instanceHolder = new DenseInstance(instanceClassifier.getDatasetHeader().numAttributes());
        classIndex = instanceClassifier.getDatasetHeader().classIndex();

        System.out.println("Initialiazing the classifier function. Model: " + modelFile + ". Header: " + featureHeader);

        InputStream in = CapitalFinderFunction.class.getClassLoader().getResourceAsStream("data/countriesCapitals.txt");
        capitalList = new Properties();
        capitalList.load(in);
        capital = (JString) functionHelper.getObject(JTypeTag.STRING);
    }

    @Override
    public void deinitialize() {
        System.out.println("De-Initialized");
    }

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        JRecord inputRecord = (JRecord) functionHelper.getArgument(0);
        ARecordType inputRecordType = inputRecord.getRecordType();
        String fieldNames[] = inputRecordType.getFieldNames();
        IAType fieldType[] = inputRecordType.getFieldTypes();
        IJObject inputRecordFields[] = inputRecord.getFields();

        JRecord outputRecord = (JRecord) functionHelper.getResultObject();
        for (int i = 0; i < inputRecordFields.length; i++) {
            outputRecord.setValueAtPos(i, inputRecordFields[i]);
        }

        JString country = (JString)inputRecordFields[0];
        String capitalCity = capitalList.getProperty(country.getValue(), NOT_FOUND);
        capital.setValue(capitalCity);

        outputRecord.addField("capital", capital);
        functionHelper.setResult(outputRecord);
    }

}
