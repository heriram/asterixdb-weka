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
package org.apache.asterix.external.feed.ml.classification.udf;

import java.util.ArrayList;
import java.util.List;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer;
import org.apache.asterix.external.library.java.JObjects.JRecord;
import org.apache.asterix.external.library.java.JObjects.JString;
import org.apache.asterix.external.library.java.JObjects.JUnorderedList;
import org.apache.asterix.external.library.java.JTypeTag;

public class TweetTokenizerFunction implements IExternalScalarFunction {

    private JUnorderedList list;
    private TextAnalyzer textAnalyzer;
    private List<String> topicList;

    @Override
    public void initialize(IFunctionHelper functionHelper) {
        list = new JUnorderedList(functionHelper.getObject(JTypeTag.STRING));
        textAnalyzer = new TextAnalyzer();
        topicList = new ArrayList<>();
    }

    @Override
    public void deinitialize() {
    }

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        list.clear();
        JRecord inputRecord = (JRecord) functionHelper.getArgument(0);
        JString id = (JString) inputRecord.getValueByName("id");
        JString text = (JString) inputRecord.getValueByName("text");

        textAnalyzer.analyze(text.getValue());
        TextAnalyzer.Term[] terms = textAnalyzer.getTerms();

        // Extract the topics from the tweets
        for (TextAnalyzer.Term term : terms) {
            if (term.getTerm().startsWith("#")) {
                topicList.add(term.getTerm());
            }
        }
        JRecord result = (JRecord) functionHelper.getResultObject();
        result.setField("id", id);
        result.setField("text", text);
        functionHelper.setResult(result);
    }

}
