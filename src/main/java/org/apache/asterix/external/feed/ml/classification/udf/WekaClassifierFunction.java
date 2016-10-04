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
package org.apache.asterix.external.feed.ml.classification.udf;

import java.io.InputStream;
import java.util.logging.Logger;
import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.feed.ml.classification.InstanceClassifier;
import org.apache.asterix.external.feed.ml.classification.udf.tests.ClassifierTestFunction;
import org.apache.asterix.external.library.java.JObjects;
import org.apache.asterix.external.library.java.JObjects.JRecord;
import org.apache.asterix.external.library.java.JObjects.JString;
import org.apache.asterix.external.library.java.JTypeTag;
import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.IAType;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

public class WekaClassifierFunction implements IExternalScalarFunction {
    private static final Logger LOGGER = Logger.getLogger(ClassifierTestFunction.class.getName());
    private InstanceClassifier instanceClassifier;
    private Instance instanceHolder;
    private int classIndex;

    private final String PRIMARY_KEY_NAME = "id";

    @Override
    public void deinitialize() {
        System.out.println("De-Initialized");
    }

    @Override
    public void initialize(IFunctionHelper functionHelper) throws Exception {
        //TODO Get the necessary files
        String modelFile = "data/RANDOM_FOREST.model";
        String featureHeader = "data/weather_header.json";

        LOGGER.info("Initialiazing the classifier function using: " + modelFile + " model, and " + featureHeader + " "
                + "header file.");

        InputStream headerInputStream = WekaClassifierFunction.class.getClassLoader().getResourceAsStream(featureHeader);
        InputStream modelInputStream = WekaClassifierFunction.class.getClassLoader().getResourceAsStream(modelFile);

        instanceClassifier = new InstanceClassifier(modelInputStream, headerInputStream);
        instanceHolder = new DenseInstance(instanceClassifier.getDatasetHeader().numAttributes());
        classIndex = instanceClassifier.getDatasetHeader().classIndex();
    }

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        JRecord inputRecord = (JRecord) functionHelper.getArgument(0);
        JRecord outputRecord = (JRecord) functionHelper.getResultObject();

        IJObject inputRecordFields[] = inputRecord.getFields();

        for (int i = 0; i < inputRecordFields.length; i++) {
            outputRecord.setValueAtPos(i, inputRecordFields[i]);
        }

        boolean containsClassAttributeField = getInstanceFromRecord(inputRecord);
        JString classValueString = (JString) functionHelper.getObject(JTypeTag.STRING);
        classValueString.setValue(instanceClassifier.classify(instanceHolder));

        if (containsClassAttributeField) {
            outputRecord.setField(instanceHolder.classAttribute().name(), classValueString);
        } else {
            outputRecord.addField(instanceHolder.classAttribute().name(), classValueString);
        }

        functionHelper.setResult(outputRecord);
    }

    private boolean getInstanceFromRecord(JRecord inputRecord) throws AsterixException {
        ARecordType inputRecordType = inputRecord.getRecordType();
        String fieldNames[] = inputRecordType.getFieldNames();
        IAType fieldType[] = inputRecordType.getFieldTypes();
        IJObject inputRecordFields[] = inputRecord.getFields();

        boolean containsClassAttributeField = false;
        for (int i = 0; i < fieldNames.length; i++) {
            int attributeIndex = instanceClassifier.getAttributeIndex(fieldNames[i]);
            if (attributeIndex == -1) {
                if (!fieldNames[i].equals(PRIMARY_KEY_NAME)) {
                    throw new AsterixException("[" + fieldNames[i] + "] is not a valid attribute name.");
                }
            } else {
                if (classIndex != attributeIndex) { // Ignore the class attribute for now
                    // Extract the attribute value
                    Attribute attribute = instanceClassifier.getDatasetHeader().attribute(attributeIndex);
                    switch (fieldType[i].getTypeTag()) {
                        case STRING:
                            instanceHolder.setValue(attribute, ((JString) inputRecordFields[i]).getValue());
                            break;
                        case DOUBLE:
                            instanceHolder.setValue(attribute, ((JObjects.JDouble) inputRecordFields[i]).getValue());
                            break;
                    }
                } else {
                    containsClassAttributeField = true;
                }
            }
        }
        return containsClassAttributeField;
    }
}
