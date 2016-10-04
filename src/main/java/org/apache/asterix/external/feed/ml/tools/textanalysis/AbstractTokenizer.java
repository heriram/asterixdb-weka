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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.asterix.external.feed.ml.api.ITokenizer;

public abstract class AbstractTokenizer implements ITokenizer {
    protected char lettersAndDidit[];
    protected char[][] utf8CharIntervals = { { '0' + 0, '0' + 9 }, { 'A', 'Z' }, { 'a', 'z' }, { 0x00C0, 0x00F6 },
            { 0x00F8, 0x02AF } };

    protected final char SPACE_CHARS[] = " \n\t\r".toCharArray();
    protected final char SPECIAL_CHARS[] = "-_@".toCharArray();

    public static final Set<?> ENGLISH_STOP_WORDS_SET;

    static {
        final List<String> stopWords = Arrays.asList("a", "about", "above", "after", "again", "against", "all", "am",
                "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below",
                "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does",
                "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
                "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
                "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm",
                "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more",
                "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
                "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd",
                "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the",
                "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd",
                "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up",
                "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's",
                "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with",
                "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
                "yourself", "yourselves", "--", "_", "-");
        final Set<String> stopSet = new HashSet<>(stopWords.size());
        stopSet.addAll(stopWords);
        ENGLISH_STOP_WORDS_SET = Collections.unmodifiableSet(stopSet);
    }

    public static boolean isLetterOrDigit(char ch) {
        return (ch >= toDigit(0) && ch <= toDigit(9)) || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                || (ch >= 0x00C0 && ch <= 0x00F6) || (ch >= 0x00F8 && ch <= 0x02AF);
    }

    public static char toDigit(int n) {
        return (char) ('0' + n);
    }

    public boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= 0x00C0 && c <= 0x00F6)
                || (c >= 0x00F8 && c <= 0x02AF);
    }

    public String[] removeStopWord(String tokens[]) {
        String tmp[] = new String[tokens.length];
        int count = 0;
        for (String token : tokens) {
            if (!ENGLISH_STOP_WORDS_SET.contains(token)) {
                tmp[count] = token;
                count++;
            }
        }

        String result[] = new String[count];
        System.arraycopy(tmp, 0, result, 0, count);

        return result;
    }

}
