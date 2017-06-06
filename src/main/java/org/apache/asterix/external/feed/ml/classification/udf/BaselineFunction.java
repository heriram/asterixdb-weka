package org.apache.asterix.external.feed.ml.classification.udf;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.api.IJObject;
import org.apache.asterix.external.feed.ml.classification.InstanceClassifier;
import org.apache.asterix.external.feed.ml.tools.textanalysis.IFeature;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer;
import org.apache.asterix.external.library.java.JObjects;
import org.apache.asterix.external.statistics.Statistics;
import weka.core.Instance;

import java.util.List;
import java.util.Timer;
import java.util.logging.Logger;

/**
 * INPUT: TextType OPEN { text: string }
 * OUTPUT: TextType OPEN { text: string }
 *
 * Extract features from the textfield and add to instanceHolder
 * Classify instanceHolder
 * Return original record plus class-attribute
 *
 */

public class BaselineFunction implements IExternalScalarFunction {
    private Statistics statistics;

    @Override
    public void deinitialize() {
        System.out.println("De-Initialized");
    }

    @Override
    public void initialize(IFunctionHelper functionHelper) throws Exception {
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

        statistics.addToCount(1);

        functionHelper.setResult(outputRecord);

    }

}
