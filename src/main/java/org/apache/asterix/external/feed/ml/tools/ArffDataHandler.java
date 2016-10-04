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

import java.util.Enumeration;
import java.util.UUID;

import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.IAType;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**

 Converts the following format:

 @relation <dataset-name>

 % Comments (ignored)

 @attribute1 [{value1,value2,value3} | datatype]
 @attribute2 [{value1,value2,value3} | datatype]
 ...

 @data
 [attr1,attr2,...]
 [attr1,attr2,...]
 [attr1,attr2,...]
 [attr1,attr2,...]

 to the following:

    {
       "relation":dataset-name,
       "attributes":[
          {'name':attribute1,'values': [value1,value2...]},
          {'name':attribute2,'values': [value1,value2...]}
       ],
       "data":[
          {attr1:val1,attr2:val2...},
          {attr1:val1,attr2:val2...},
          {attr1:val1,attr2:val2...},
          ...
       ]
    }

 */

public class ArffDataHandler {

    //Instances data;

    private static final String RELATION = "\"relation\":"; // The dataset name
    private static final String ATTRIBUTES = "\"attributes\":"; // Specification of the attributes
    private static final String DATA = "\"data\":"; // The data
    private static final String ATTR_NAME_FIELD = "\"name\":"; // Attribute name
    private static final String ATTR_VALUES_FIELD = "\"values\":"; // Attribute value


    public ArffDataHandler() {
    }

    /**
     * Build the record type using attributes as the record fields
     * @param data Instances pointing the dataset content
     * @return ARecordType for the feature record
     */

    public static ARecordType getFeatureRecordType(Instances data, boolean classIndexIsSpecified) {
        int numFields = data.numAttributes() +  1;
        int i=0;
        String fieldNames[] = new String[numFields];
        fieldNames[i] =  "fid"; //"feat-" + UUID.randomUUID().toString();
        IAType fieldTypes[] = new IAType[numFields];
        fieldTypes[i] = BuiltinType.ASTRING;
        i++;

        Enumeration<Attribute> attributeEnumeration = data.enumerateAttributes();
        while(attributeEnumeration.hasMoreElements()) {
            Attribute attribute = attributeEnumeration.nextElement();
            fieldNames[i] = attribute.name();
            if (attribute.isNumeric()) {
                fieldTypes[i] = BuiltinType.ADOUBLE;
            } else {
                fieldTypes[i] = BuiltinType.ASTRING;
            }
            i++;
        }

        if (classIndexIsSpecified) {
            Attribute attribute = data.classAttribute();
            fieldNames[i] = attribute.name();
            fieldTypes[i] = BuiltinType.ASTRING;
        }

        return new ARecordType(data.relationName() + "_feature_record", fieldNames, fieldTypes, false);
    }

    public static String getFeatureValueAdmString(Instance featureInstance) {
        StringBuilder sb = new StringBuilder();
        int numberOfValues = featureInstance.numValues();
        // Get a unique feature ID:
        sb.append("{\"fid\":\"feat-" + UUID.randomUUID().toString() + "\", ");

        for (int j=0; j< numberOfValues; j++) {
            Attribute attribute = featureInstance.attribute(j);
            sb.append("\"" + attribute.name() + "\":");
            if (attribute.isNumeric()) {
                sb.append(featureInstance.value(attribute));
            } else {
                sb.append("\""+ featureInstance.stringValue(attribute) + "\"");
            }
            if (j<numberOfValues-1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String getFeatureInfoAdmString(Instances data) {
        StringBuilder sb = new StringBuilder();

        // Adding the relation name
        sb.append(RELATION);
        sb.append(data.relationName() + ",\n");
        // Building the attribute
        sb.append(buildAttributesFields(data));

        return sb.toString();
    }

    public static String toAdmString(Instances data) {

        StringBuilder sb = new StringBuilder();

        sb.append("{"); // Start of the ADM object

        sb.append(getFeatureInfoAdmString(data));
        sb.append(",\n");

        // Building list of data/features
        sb.append(buildFeatureValues(data));

        sb.append("}"); // The end of the bracket for the ADM object

        return sb.toString();
    }


    private static StringBuilder buildAttributesFields(Instances data) {
        StringBuilder sb = new StringBuilder();
        sb.append(ATTRIBUTES);
        sb.append("[");
        int numAttributes = data.numAttributes();
        Enumeration<Attribute> attributeEnumeration = data.enumerateAttributes();

        int i=0;
        while(attributeEnumeration.hasMoreElements()) {
            Attribute attribute = attributeEnumeration.nextElement();
            sb.append("{" + ATTR_NAME_FIELD);
            sb.append(attribute.name() + ",");
            sb.append(ATTR_VALUES_FIELD);

            if(attribute.isNominal() || attribute.isRelationValued()) {
                sb.append("[");
                Enumeration<Object> attributeValueEnumeration = attribute.enumerateValues();
                int j = 0;
                int numValues = attribute.numValues();
                while (attributeValueEnumeration.hasMoreElements()) {
                    sb.append((String) attributeValueEnumeration.nextElement());
                    j++;
                    if (j < (numValues)) {
                        sb.append(",");
                    }
                }
                sb.append("]");
            } else {
                if (attribute.isNumeric()) {
                    sb.append(Attribute.ARFF_ATTRIBUTE_NUMERIC);
                } else if (attribute.isDate()) {
                    sb.append(Attribute.ARFF_ATTRIBUTE_DATE);
                } else {
                    sb.append(attribute.toString());
                }
            }
            sb.append("}");
            i++;
            // For the next item
            if (i<numAttributes) {
                sb.append(",\n");
            }
        }
        sb.append("]");
        return sb;
    }

    private static StringBuilder buildFeatureValues(Instances data) {
        StringBuilder sb = new StringBuilder();
        int dataSize = data.size();

        sb.append(DATA);
        sb.append("[");
        Enumeration<Instance> instanceEnumeration = data.enumerateInstances();
        int i=0;

        while(instanceEnumeration.hasMoreElements()) {
            sb.append(getFeatureValueAdmString(instanceEnumeration.nextElement()));
            i++;
            if (i<dataSize) {
                sb.append(",\n");
            }

        }
        sb.append("]");

        return sb;
    }
}
