package org.apache.asterix.external.feed.ml.tools.textanalysis;

import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer.Term;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TwitterFeatures implements IFeatureExtractor  {

    private Features.NumericFeature terms;
    private Features.NumericFeature topics;
    private Features.NumericFeature tags;
    private Features.NumericFeature links;
    private Features.NumericFeature sentiment;
    private Lexicon lexicon;
    private Features features;

    public TwitterFeatures() {
        terms = new Features.NumericFeature("terms", 0);
        topics = new Features.NumericFeature("topics", 0);
        tags = new Features.NumericFeature("tags", 0);
        links = new Features.NumericFeature("links", 0);
        sentiment = new Features.NumericFeature("sentiment", 0);
        lexicon = new Lexicon();
        features = new Features(new ArrayList<IFeature>() {{
            add(terms);
            add(topics);
            add(tags);
            add(links);
            add(sentiment);
        }});
    }

    public Features getFeatures() {
        return features;
    }

    public void extract(String token, int frequency) {
        int topicSentimentWeight = 2;

        terms.updateValue(frequency > 0 ? frequency : 1);

        if (token.startsWith("#")) {
            topics.updateValue(1);
            sentiment.updateValue(lexicon.checkToken(token.substring(1)) * topicSentimentWeight);
        } else {
            sentiment.updateValue(lexicon.checkToken(token));
        }
        if (token.startsWith("@")) {
            tags.updateValue(1);
        }
        if (token.startsWith("http") || token.startsWith("www")) {
            links.updateValue(1);
        }

    }

    public void extract(Term terms[]) {
        this.reset();
        for (Term term : terms) {
            extract(term.getTerm(), term.getFrequence());
        }
    }

    public void reset() {
        terms.resetValue();
        topics.resetValue();
        tags.resetValue();
        links.resetValue();
        sentiment.resetValue();
    }

    @Override
    public String toString() {
        return "{" +
                terms.getName() + ": " + terms.getValue() + ", " +
                topics.getName() + ": " + topics.getValue() + ", " +
                tags.getName() + ": " + tags.getValue() + ", " +
                links.getName() + ": " + links.getValue() + ", " +
                sentiment.getName() + ": " + sentiment.getValue() +
                "}";
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
