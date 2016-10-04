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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.library.java.JObjects.JDouble;
import org.apache.asterix.external.library.java.JObjects.JRecord;
import org.apache.asterix.external.library.java.JObjects.JString;
import org.apache.asterix.om.types.BuiltinType;
import org.json.JSONException;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class AsterixWekaDataHandler {
    public static final String ASTERIX_DOUBLE = BuiltinType.ADOUBLE.toString().toLowerCase();
    public static final String ASTERIX_STRING = BuiltinType.ASTRING.toString().toLowerCase();
    public static final String ASTERIX_UUID = BuiltinType.AUUID.toString().toLowerCase();
    private final Map<String, Integer> fieldPositions = new HashMap<>();
    private final Instances dataset;
    private boolean isDenseFeatures = true;
    private final List<Attribute> attributeList;

    public AsterixWekaDataHandler(Instances dataset, boolean isDenseFeatures) {
        this.isDenseFeatures = isDenseFeatures;
        this.dataset = dataset;
        this.attributeList = Collections.list(dataset.enumerateAttributes());
        initializeFieldPositions();
    }

    public void initializeFieldPositions() {
        int index = 0;

        // Note this assumes consitency
        for (Attribute attribute: attributeList) {
            fieldPositions.put(attribute.name(), index);
            index++;
        }

        // Make sure the class attribute is not missing
        if (attributeList.size() != dataset.numAttributes()) {
            // Add class attribute at the end of the list
            fieldPositions.put(dataset.classAttribute().name(), index);
        }
    }

    public Instance getWekaInstance(JRecord inputRecord) throws JSONException {
        IJObject fields[] = inputRecord.getFields();
        Instance instance = isDenseFeatures ? new DenseInstance(dataset.numAttributes()) :
                new SparseInstance(dataset.numAttributes());
        instance.setDataset(dataset);
        for (Attribute attribute: attributeList) {
            IJObject field = fields[fieldPositions.get(attribute.name())];
            //TODO Add an extra type consitency check here
            if (attribute.isNumeric()) {
                instance.setValue(attribute, ((JDouble)field).getValue());
            } else {
                instance.setValue(attribute, ((JString)field).getValue());
            }

        }
        return instance;
    }

    /**
     * Create a record type from WEKA instances
     *
     * @param dataset
     *            the dataset containing the attribute information
     * @return
     *         ARecordType
     */
    public static void writeAsterixDDL(String outputFileName, Instances dataset) throws FileNotFoundException {
        File outFile = new File(outputFileName);
        PrintWriter writer = new PrintWriter(outFile);

        int numAttributes = dataset.numAttributes();
        String relationName = dataset.relationName();
        writer.write("create type ");
        writer.write(relationName);
        writer.write("Type as closed {\n");
        writer.write("  id: ");
        writer.write(ASTERIX_UUID);
        writer.write(",\n"); // Feature ID (will be made auto-generated)

        Enumeration<Attribute> attributeEnumeration = dataset.enumerateAttributes();
        int i = 0;
        while (attributeEnumeration.hasMoreElements()) {
            Attribute attribute = attributeEnumeration.nextElement();
            writer.write("  ");
            writer.write(attribute.name());
            writer.write(": ");
            if (attribute.isNumeric()) {
                writer.write(ASTERIX_DOUBLE);
            } else {
                writer.write(ASTERIX_STRING);
            }
            i++;
            if (i < numAttributes) {
                writer.write(",\n");
            }
        }

        // Make sure we have the class attribute as well
        if (i < numAttributes) {
            Attribute attribute = dataset.classAttribute();
            writer.write("  ");
            writer.write(attribute.name());
            writer.write(": ");
            writer.write(ASTERIX_STRING);
        }

        writer.write("\n};\n");

        // Write create statement
        writer.write("create dataset ");
        writer.write(relationName);
        writer.write("Dataset(");
        writer.write(relationName);
        writer.write("Type)\nprimary key id autogenerated;");

        writer.close();
    }
}
