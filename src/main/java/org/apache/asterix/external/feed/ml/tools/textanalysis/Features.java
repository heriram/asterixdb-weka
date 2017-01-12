package org.apache.asterix.external.feed.ml.tools.textanalysis;

import java.util.List;
import java.util.stream.Collectors;

public class Features {

    public List<IFeature> features;

    public Features(List<IFeature> features) {
        this.features = features;
    }

    public List<IFeature> getFeatureList() {
        return features;
    }

    public List<Object> getFeatureValues() {
        return features.stream().map(IFeature::getValue).collect(Collectors.toList());
    }

    public List<String> getFeatureNames() {
        return features.stream().map(IFeature::getName).collect(Collectors.toList());
    }

    public List<String> getFeatureTypes() {
        return features.stream().map(IFeature::getType).collect(Collectors.toList());
    }

//----------------------------------------------------------------------------------------------------------------------
    public static class NumericFeature implements IFeature<Integer> {

        private String type = "numeric";
        private String name;
        private Integer value;

        public NumericFeature(String name, int value) {
            this.name = name;
            this.value = value;
        }
        public String getType() {
            return this.type;
        }
        public String getName() {
            return this.name;
        }
        public Integer getValue() {
            return this.value;
        }
        public void updateValue(Integer value) {
            this.value += value;
        }
        public void resetValue() {
            this.value = 0;
        }
    }
//----------------------------------------------------------------------------------------------------------------------
}
