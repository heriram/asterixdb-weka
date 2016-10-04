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

package org.apache.asterix.external.feed.ml.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import weka.core.Instances;
import weka.core.converters.JSONLoader;
import weka.core.json.JSONInstances;
import weka.core.json.JSONNode;

public class WekaJSONLoader extends JSONLoader {
    private Instances dataSet = null;

    public WekaJSONLoader() {
        super();
    }

    public JSONNode getRootNode() throws IOException {
        if (m_JSON == null) {
            this.dataSet = getDataSet();
        }
        return m_JSON;
    }

    public Instances toInstances() throws IOException {
        if (dataSet == null) {
            dataSet = getDataSet();
        }
        return dataSet;
    }

    @Override
    public void setSource(InputStream in) throws IOException {
        m_sourceReader = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public void reset() {
        m_structure = null;
        m_JSON      = null;

        setRetrieval(NONE);
    }

    public int getClassIndex() throws Exception  {
        return getClassIndex(m_JSON);
    }

    public static int getClassIndex(JSONNode rootNode) throws Exception {
        if (rootNode == null) {
            throw new Exception("No root node was found. Please make sure the JSON file was loaded properly.");
        }
        JSONNode attributeNode = rootNode.getChild(JSONInstances.HEADER).getChild(JSONInstances.ATTRIBUTES);
        Enumeration<JSONNode> attributeValueNodes = attributeNode.children();
        int index = 0;
        while (attributeValueNodes.hasMoreElements()) {
            JSONNode node = attributeValueNodes.nextElement();
            JSONNode classNode =node.getChild(JSONInstances.CLASS);
            boolean classNodeValue = (Boolean) classNode.getValue();
            if (classNodeValue == true) {
                return index;
            }
            index++;
        }
        return -1;
    }
}