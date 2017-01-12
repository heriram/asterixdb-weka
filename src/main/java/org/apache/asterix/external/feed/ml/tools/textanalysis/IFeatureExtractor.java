package org.apache.asterix.external.feed.ml.tools.textanalysis;

public interface IFeatureExtractor {
    void extract(TextAnalyzer.Term terms[]);
    void extract(String token, int frequency);
    void reset();
}
