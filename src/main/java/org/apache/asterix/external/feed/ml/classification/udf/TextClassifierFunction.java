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
import org.apache.asterix.external.statistics.Statistics;
import weka.core.DenseInstance;
import weka.core.Instance;

import java.io.InputStream;
import java.util.List;
import java.util.Timer;
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
    private Statistics statistics;

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

        this.statistics = new Statistics();
        Timer timer = new Timer();
        timer.schedule(this.statistics, 0, 2000);
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

        statistics.addToCount(1);

        functionHelper.setResult(outputRecord);

    }

    private void extractFeatures(String text) {
        analyzer.analyze(text);

        List<IFeature> features = analyzer.getFeatures().getFeatureList();

        for (int i = 0; i < features.size(); i++) {
            IFeature feature = features.get(i);

            if (feature.getType().equals("numeric")) {
                JObjects.JDouble value = new JObjects.JDouble((Integer)feature.getValue());
                instanceHolder.setValue(i, value.getValue());
            }
        }

    }
}
