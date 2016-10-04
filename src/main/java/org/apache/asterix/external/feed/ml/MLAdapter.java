/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.feed.ml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.asterix.external.dataset.adapter.FeedAdapter;
import org.apache.asterix.external.feed.ml.classification.WekaClassifier;
import org.apache.asterix.external.feed.ml.tools.ArffDataHandler;
import org.apache.asterix.external.feed.ml.tools.FeatureReader;
import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.IAType;
import org.apache.hyracks.api.comm.IFrameWriter;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.dataflow.std.file.ITupleParser;
import org.apache.hyracks.dataflow.std.file.ITupleParserFactory;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class MLAdapter extends FeedAdapter {

    private static final long serialVersionUID = 1L;

    private final PipedOutputStream pos;

    private final PipedInputStream pis;

    private final Map<String, String> configuration;

    private DataGenerator generator;

    protected final ITupleParser tupleParser;

    protected final IAType sourceDatatype;

    protected static final Logger LOGGER = Logger.getLogger(MLAdapter.class.getName());

    public MLAdapter(ITupleParserFactory parserFactory, ARecordType sourceDatatype, IHyracksTaskContext ctx,
            Map<String, String> configuration, int partition) throws IOException {
        super(null);
        pos = new PipedOutputStream();
        pis = new PipedInputStream(pos);
        this.configuration = configuration;
        this.tupleParser = parserFactory.createTupleParser(ctx);
        this.sourceDatatype = sourceDatatype;

    }

    @Override
    public void start(int partition, IFrameWriter writer) throws HyracksDataException {
        LOGGER.info("Start running the adapter");
        generator = new DataGenerator(configuration, pos);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(generator);
        if (pis != null) {
            tupleParser.parse(pis, writer);
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(
                        "Could not obtain input stream for parsing from adapter " + this + "[" + partition + "]");
            }
        }
    }

    private static class DataGenerator implements Runnable {
        private final OutputStream os;
        private final byte[] EOL = "\n".getBytes();
        private boolean continueIngestion;
        private final FeatureReader featureReader = new FeatureReader();
        private Instances dataset;
        private Attribute classAttribute;
        private WekaClassifier classifier;

        public DataGenerator(Map<String, String> configuration, OutputStream os) {
            this.os = os;
            this.continueIngestion = true;

            String dataFile = configuration.get(MLAdapterFactory.KEY_DATA_FILE);
            try {
                dataset = featureReader.getWekaInstances(dataFile, true);
                classAttribute = dataset.classAttribute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String classifierName = configuration.get(MLAdapterFactory.KEY_CLASSIFIER);
            if (classifierName == null) {
                classifierName = "J48"; // use a default classifier
            }
            classifier = new WekaClassifier(classifierName);
            try {
                LOGGER.info("Start training based on the training dataset.");
                classifier.trainClassifier(dataset, featureReader.getClassAttributeIndex());
                LOGGER.info("Finished training");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            LOGGER.log(Level.INFO, "Feed adapter created and loaded successfully. Now start ingesting data.");

            try {
                Enumeration<Instance> dataEnumeration = dataset.enumerateInstances();
                while (continueIngestion && dataEnumeration.hasMoreElements()) {
                    String dataRecord = ArffDataHandler.getFeatureValueAdmString(dataEnumeration.nextElement());
                    LOGGER.log(Level.INFO, "Trying to ingest: " + dataRecord);
                    os.write(dataRecord.getBytes());
                    os.write(EOL);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void stop() {
            continueIngestion = false;
        }
    }

    @Override
    public boolean stop() {
        LOGGER.info(MLAdapter.class.getName() + " stopped successfully.");
        generator.stop();
        return true;
    }

    @Override
    public boolean handleException(Throwable e) {
        return false;
    }

    @Override
    public boolean pause() {
        return false;
    }

    @Override
    public boolean resume() {
        return false;
    }
}
