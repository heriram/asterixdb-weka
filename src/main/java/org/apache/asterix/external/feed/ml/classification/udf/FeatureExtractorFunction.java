package org.apache.asterix.external.feed.ml.classification.udf;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.feed.ml.classification.InstanceClassifier;
import org.apache.asterix.external.feed.ml.tools.ResourceHelper;
import org.apache.asterix.external.feed.ml.tools.textanalysis.IFeature;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer;
import org.apache.asterix.external.library.java.JObjects;
import org.apache.asterix.external.library.java.JTypeTag;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * INPUT: TextType OPEN { id: int, text: string }
 * OUTPUT: FeatureType CLOSED {
 *      id: uuid,
 *      terms: double,
 *      topics: double,
 *      tags: double,
 *      links: double,
 *      sentiment: double,
 *      class: string
 }
 *
 * Extract features from the textfield and add to instanceHolder
 * Classify instanceHolder
 * Return original record plus class-attribute
 *
 */

public class FeatureExtractorFunction implements IExternalScalarFunction {
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
        String CLASSIFIER_ALGORITHM = ResourceHelper.configLookup("classifier-algorithm");
        String DOMAIN = ResourceHelper.configLookup("domain");

        String modelFile = "data/models/" + CLASSIFIER_ALGORITHM + ".model";
        String featureHeader = "data/" + DOMAIN + "_header.json";

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

        // Exctract the features from the text fields
        JObjects.JString text = (JObjects.JString) inputRecord.getValueByName("text");
        analyzer.analyze(text.getValue());
        List<IFeature> features = analyzer.getFeatures().getFeatureList();

        for (int i = 0; i < features.size(); i++) {
            IFeature feature = features.get(i);

            if (feature.getType().equals("numeric")) {
                JObjects.JDouble value = new JObjects.JDouble((Integer)feature.getValue());
                outputRecord.addField(feature.getName(), value);
            }

            // For simplicity ignore other types of features for now
        }

        JObjects.JString classValueString;
        if (inputRecord.getRecordType().doesFieldExist("class")) {
            classValueString = (JObjects.JString) inputRecord.getValueByName("class");
        } else {
            classValueString = (JObjects.JString) functionHelper.getObject(JTypeTag.STRING);
            classValueString.setValue("?");
        }

        outputRecord.addField("class", classValueString);
        functionHelper.setResult(outputRecord);

    }
}
