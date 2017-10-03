package org.apache.asterix.external.feed.ml.tools.textanalysis;

import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer.Term;

import java.io.*;
import java.util.*;

public class TwitterFeatures implements IFeatureExtractor  {

    private Features.NumericFeature terms; // Number of unique terms
    private Features.NumericFeature topics; // Number of hashtags
    private Features.NumericFeature tags; // Number of taged user
    private Features.NumericFeature links; // Number of links in the tweet
    private Features.NumericFeature sentiment; // Number of sentiment-related words
    private Lexicon lexicon;
    private Features features;

    public TwitterFeatures() {
        terms = new Features.NumericFeature("terms", 0);
        topics = new Features.NumericFeature("topics", 0);
        tags = new Features.NumericFeature("tags", 0);
        links = new Features.NumericFeature("links", 0);
        sentiment = new Features.NumericFeature("sentiment_rate", 0);
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
            String positivePath = "data/sentimentLexicon/positive-words.txt";
            String negativePath = "data/sentimentLexicon/negative-words.txt";
            InputStream positiveInputStream = Lexicon.class.getClassLoader().getResourceAsStream(positivePath);
            InputStream negativeInputStream = Lexicon.class.getClassLoader().getResourceAsStream(negativePath);
            this.positive = loadLexicon(positiveInputStream);
            this.negative = loadLexicon(negativeInputStream);
        }

        private Set loadLexicon(InputStream inputStream) {
            Set lexicon = new LinkedHashSet();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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
