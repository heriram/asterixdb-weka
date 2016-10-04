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

public class Tokenizer extends AbstractTokenizer {

    public static final Tokenizer INSTANCE = new Tokenizer();

    private Tokenizer() {

    }

    public String[] tokenize(char textCharArray[], boolean removeStopWord) {
        int len = textCharArray.length;
        String[] temp = new String[(len / 2) + 2];
        int wordCount = 0;
        char wordBuff[] = new char[len];
        int index = 0;

        for (int i = 0; i < len; i++) {
            char c = textCharArray[i];
            c = Character.toLowerCase(c);
            switch (c) {
                case '\'': // Remove "'s"
                    if (i == (len - 1))
                        break;

                    char next_c = textCharArray[i + 1];
                    if (next_c == 's') {
                        i++;
                    } else if (next_c == 't') { // keep 't forms for now
                        wordBuff[index] = c;
                        index++;
                        wordBuff[index] = next_c;
                        index++;
                        i++;
                    }
                    break;
                case ' ':
                case '\n':
                case '\t':
                case '\r':
                    if (index > 0) {
                        String word = new String(wordBuff, 0, index);
                        index = 0;
                        temp[wordCount] = word;
                        wordCount++;

                    }
                    break;

                case '&':
                case '#':
                case '@':
                case '-':
                case '_':
                    wordBuff[index] = c;
                    index++;

                    break;
                default:
                    if ((c >= toDigit(0) && c <= toDigit(9)) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                            || (c >= 0x00C0 && c <= 0x00F6) || (c >= 0x00F8 && c <= 0x02AF)) {
                        wordBuff[index] = c;
                        index++;
                    }
            }

        }

        String lastToken = new String(wordBuff, 0, index).trim();
        if (!lastToken.isEmpty()) {
            temp[wordCount] = lastToken;
            wordCount++;
        }
        String result[] = new String[wordCount];
        System.arraycopy(temp, 0, result, 0, wordCount);

        if (removeStopWord)
            result = removeStopWord(result);

        return result;
    }

    @Override
    public String[] tokenize(String text, boolean removeStopWord) {
        return tokenize(text.trim().toCharArray(), removeStopWord);
    }

    @Override
    public String[] tokenize(String text) {
        String tokens[] = tokenize(text, true);
        return tokens;
    }
}
