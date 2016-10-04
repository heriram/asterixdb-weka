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

package org.apache.asterix.external.feed.ml.tools.textanalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.asterix.external.feed.ml.api.ITokenizer;

public class TextAnalyzer {
    public static class Term {
        private String term;
        private int frequence;

        public Term(String term, int frequence) {
            this.term = term;
            this.frequence = frequence;
        }

        public String getTerm() {
            return this.term;
        }

        public int getFrequence() {
            return this.frequence;
        }

        @Override
        public String toString() {
            return "{" + term + ": " + frequence + "}";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + frequence;
            result = prime * result + ((term == null) ? 0 : term.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Term other = (Term) obj;
            if (frequence != other.frequence)
                return false;
            if (term == null) {
                if (other.term != null)
                    return false;
            } else if (!term.equals(other.term))
                return false;
            return true;
        }

    }

    private int length;
    private int maxTf = 0;

    private Map<String, Integer> termVector;

    private ITokenizer tokenizer;
    private Term terms[];
    private String tokens[];

    private PorterStemmer stemmer = null;

    public TextAnalyzer() {
        this(Tokenizer.INSTANCE, null);
    }

    public TextAnalyzer(ITokenizer tokenizer, PorterStemmer stemmer) {
        this.tokenizer = tokenizer;
        termVector = new HashMap<>();
        this.stemmer = stemmer;
    }

    ITokenizer getTokenizer() {
        return this.tokenizer;
    }

    public Term[] getTerms() {
        return terms;
    }

    public String[] getTokens() {
        return tokens;
    }

    public String[] getAnalyzedTerms() {
        String t[] = new String[termVector.size()];
        Iterator<String> it = termVector.keySet().iterator();

        for (int i = 0; i < t.length && it.hasNext(); i++)
            t[i] = it.next();

        return t;
    }

    public void getTerms(Set<String> termSet) {
        termSet.addAll(termVector.keySet());
    }

    public void getTermVector(Map<String, Integer> termVector) {
        termVector.putAll(this.termVector);
    }

    public Integer[] getRawTermFrequencies() {
        return termVector.values().toArray(new Integer[termVector.size()]);
    }

    public double[] getTermNormalizedFrequencies() {
        List<Integer> tfList = (List<Integer>) termVector.values();

        int size = tfList.size();
        double ntfs[] = new double[size];

        for (int i = 0; i < size; i++) {
            ntfs[i] = tfList.get(i) / (double) maxTf;
        }
        return ntfs;
    }

    public void reset() {
        termVector.clear();
        maxTf = 0;
        length = 0;
    }

    public int getLength() {
        return length;
    }

    public void analyze(String text) {
        tokens = tokenizer.tokenize(text);
        reset();
        length = tokens.length;

        for (String t : tokens) {
            int f = 1;

            String token = (stemmer != null) ? stemmer.stem(t) : t;

            if (termVector.containsKey(token))
                f += termVector.get(token);

            if (f > maxTf)
                maxTf = f;

            termVector.put(token, f);
        }
        terms = new Term[termVector.size()];

        Iterator<Entry<String, Integer>> it = termVector.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Entry<String, Integer> e = it.next();
            terms[i] = new Term(e.getKey(), e.getValue());
            i++;
        }
    }

    public static void main(String[] args) {
        TextAnalyzer analyzer = new TextAnalyzer();
        TextAnalyzer stemAnalyzer = new TextAnalyzer(Tokenizer.INSTANCE, new PorterStemmer());
        String test = "@johnsmith: this is a test of this new #tokenizer. It won't be bad if this test works as good "
                + "Big-Data.";
        analyzer.analyze(test);
        stemAnalyzer.analyze(test);

        Term tokens[] = analyzer.getTerms();
        Term stems[] = stemAnalyzer.getTerms();
        System.out.println("Un-stemmed tokens: " + Arrays.toString(tokens) + ".\n Stems: " + Arrays.toString(stems));
    }

}
