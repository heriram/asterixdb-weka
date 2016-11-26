package org.apache.asterix.external.feed.ml.classification.udf;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.feed.ml.classification.InstanceClassifier;
import org.apache.asterix.external.feed.ml.tools.textanalysis.Features;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer;
import org.apache.asterix.external.library.java.JObjects;
import org.apache.asterix.external.library.java.JTypeTag;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * INPUT: TextType OPEN { id: int, text: string }
 * OUTPUT: TextType OPEN { id: int, text: string, class: string}
 *
 * Extract features from the textfield and add to instanceHolder
 * Classify instanceHolder
 * Return original record plus class-attribute
 *
 */

public class TextClassifierFunction implements IExternalScalarFunction {
    private static final Logger LOGGER = Logger.getLogger(TextClassifierFunction.class.getName());
    private InstanceClassifier instanceClassifier;
    private Instance instanceHolder;
    private TextAnalyzer analyzer;

    @Override
    public void deinitialize() {
        System.out.println("De-Initialized");
    }

    @Override
    public void initialize(IFunctionHelper functionHelper) throws Exception {
        //TODO Get the necessary files
        String modelFile = "data/models/J48.model";
        String featureHeader = "data/twitter_header.json";

        InputStream headerInputStream = TextClassifierFunction.class.getClassLoader().getResourceAsStream(featureHeader);
        InputStream modelInputStream = TextClassifierFunction.class.getClassLoader().getResourceAsStream(modelFile);

        instanceClassifier = new InstanceClassifier(modelInputStream, headerInputStream);
        instanceHolder = new DenseInstance(instanceClassifier.getDatasetHeader().numAttributes());

        this.analyzer = new TextAnalyzer();
    }

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        JObjects.JRecord inputRecord = (JObjects.JRecord) functionHelper.getArgument(0);
        JObjects.JRecord outputRecord = (JObjects.JRecord) functionHelper.getResultObject();

        IJObject inputRecordFields[] = inputRecord.getFields();

        for (int i = 0; i < inputRecordFields.length; i++) {
            outputRecord.setValueAtPos(i, inputRecordFields[i]);
        }

        JObjects.JString text = (JObjects.JString) inputRecord.getValueByName("text");
        extractFeatures(text.getValue());
        JObjects.JString classValueString = (JObjects.JString) functionHelper.getObject(JTypeTag.STRING);
        classValueString.setValue(instanceClassifier.classify(instanceHolder));

        outputRecord.addField(instanceHolder.classAttribute().name(), classValueString);

        functionHelper.setResult(outputRecord);

    }

    private void extractFeatures(String text) {
        analyzer.analyze(text);

        Integer featureValues[] = analyzer.getFeatureValues();

        for (int i = 0; i < featureValues.length; i++) {
            JObjects.JDouble value = new JObjects.JDouble(featureValues[i]);
            instanceHolder.setValue(i, value.getValue());
        }
    }
}
