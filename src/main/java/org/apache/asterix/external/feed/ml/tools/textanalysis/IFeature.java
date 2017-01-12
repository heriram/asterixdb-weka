package org.apache.asterix.external.feed.ml.tools.textanalysis;

public interface IFeature<Type> {
    String getType();
    String getName();
    Type getValue();
    void updateValue(Type value);
    void resetValue();
}
