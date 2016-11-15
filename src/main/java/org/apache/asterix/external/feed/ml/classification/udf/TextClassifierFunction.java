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
    private int classIndex;
    private Features features;
    private TextAnalyzer analyzer;

    private final String PRIMARY_KEY_NAME = "id";

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
        classIndex = instanceClassifier.getDatasetHeader().classIndex();

        this.features = new Features();
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

        boolean containsClassAttributeField = extractFeatures(text.getValue());
        JObjects.JString classValueString = (JObjects.JString) functionHelper.getObject(JTypeTag.STRING);
        classValueString.setValue(instanceClassifier.classify(instanceHolder));

        if (containsClassAttributeField) {
            outputRecord.setField(instanceHolder.classAttribute().name(), classValueString);
        } else {
            outputRecord.addField(instanceHolder.classAttribute().name(), classValueString);
        }

        functionHelper.setResult(outputRecord);

    }

    private boolean extractFeatures(String text) {
        analyzer.analyze(text);
        TextAnalyzer.Term tokens[] = analyzer.getTerms();
        features.check(tokens);
        System.out.println("\t DEBUG \t FEATURES \t" + features.getFeatureValues());

        String featureNames[] = features.getFeatureNames();
        String featureTypes[] = features.getFeatureTypes();
        Integer featureValues[] = features.getFeatureValues();

        for (int i = 0; i < featureNames.length; i++) {
            Attribute attribute = new Attribute(featureNames[i]);
            System.out.println("\t \t DEBUG \t ATTRIBUTES \t" + attribute);
            JObjects.JDouble value = new JObjects.JDouble(featureValues[i]);
            System.out.println("\t \t DEBUG \t VALUE \t" + value.getValue());
            instanceHolder.setValue(i, value.getValue());
        }
        return false;

    }
}
