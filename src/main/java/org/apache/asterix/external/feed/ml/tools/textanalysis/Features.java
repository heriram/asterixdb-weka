package org.apache.asterix.external.feed.ml.tools.textanalysis;

import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer.Term;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Features {

    private int terms;      // numeric
    private int topics;     // numeric
    private int tags;       // numeric
    private int links;      // numeric
    private int sentiment;  // numeric
    private Lexicon lexicon;

    public Features() {
        this.terms = 0;
        this.topics = 0;
        this.tags = 0;
        this.links = 0;
        this.sentiment = 0;
        this.lexicon = new Lexicon();
    }

    public String[] getFeatureNames() {
        return new String[] {"terms", "topics", "tags", "links", "sentiment"};
    }

    public String[] getFeatureTypes() {
        return new String[] {"numeric", "numeric", "numeric", "numeric", "numeric"};
    }

    public Integer[] getFeatureValues() {
        return new Integer[] {this.terms, this.topics, this.tags, this.links, this.sentiment};
    }

    public void extract(String token, int frequency) {
        int topicSentimentWeight = 2;

        this.terms += (frequency > 0 ? frequency : 1);
        if (token.startsWith("#")) {
            this.topics += 1;
            this.sentiment += (lexicon.checkToken(token.substring(1)) * topicSentimentWeight);
        } else {
            this.sentiment += lexicon.checkToken(token);
        }
        if (token.startsWith("@")) {
            this.tags += 1;
        }
        if (token.startsWith("http") || token.startsWith("www")) {
            this.links += 1;
        }
    }

    public void extract(Term terms[]) {
        this.reset();
        for (Term term : terms) {
            extract(term.getTerm(), term.getFrequence());
        }
    }

    private void reset() {
        this.terms = 0;
        this.topics = 0;
        this.tags = 0;
        this.links = 0;
        this.sentiment = 0;
    }

    @Override
    public String toString() {
        return "{terms: " + terms + ", topics: " + topics + ", tags: " + tags +
                ", links: " + links + ", sentiment: " + sentiment + "}";
    }

//----------------------------------------------------------------------------------------------------------------------

    private class Lexicon {
        private Set positive;
        private Set negative;

        public Lexicon() {
            String lexiconDir = "/Users/thormartin/asterix-machine-learning/src/main/resources/data/sentimentLexicon/";
            // TODO: move path to config-file or make relative
            this.positive = loadLexicon(lexiconDir + "positive-words.txt");
            this.negative = loadLexicon(lexiconDir + "negative-words.txt");
        }

        private Set loadLexicon(String filePath) {
            Set lexicon = new LinkedHashSet();

            try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line = reader.readLine();
                while (line != null) {
                    if (!line.startsWith(";") && !line.isEmpty()) {
                        lexicon.add(line);
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {e.printStackTrace();}

            return lexicon;
        }

        private int checkToken(String token) {
            if (this.positive.contains(token)) {
                return 1;
            } else if (this.negative.contains(token)) {
                return -1;
            } else {
                return 0;
            }
        }
    }

//----------------------------------------------------------------------------------------------------------------------

}
