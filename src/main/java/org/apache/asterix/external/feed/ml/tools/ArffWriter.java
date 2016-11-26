package org.apache.asterix.external.feed.ml.tools;

import org.apache.asterix.external.feed.ml.tools.textanalysis.Features;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer;
import org.apache.asterix.external.feed.ml.tools.textanalysis.TextAnalyzer.Term;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ArffWriter {

    public ArffWriter() {}

    public String createRelation(String relation) {
        return "@relation " + relation;
    }

    public String createAttribute(String name, String type) {
        return "@attribute " + name + " " + type;
    }

    public String createAttributes(String[] names, String[] types) {
        String result = "\n";
        for (int i = 0; i < names.length; i++) {
            result += createAttribute(names[i], types[i]);
            result += "\n";
        }
        return result;
    }

    public String createData(List<String> data) {
        String result = "\n@data";
        for (String values : data) {
            result += '\n';
            result += values;
        }
        return result;
    }

    public void writeArff(List<String> lines) {
        // TODO: relative filePath or move to config-file
        String filePath = "/Users/thormartin/asterix-machine-learning/src/main/resources/data/twitter.arff";
        Path file = Paths.get(filePath);
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {e.printStackTrace();}
    }


    public static void main(String[] args) {

        List<String> featuresList = new ArrayList<>();
        TextAnalyzer analyzer = new TextAnalyzer();
        Features features = new Features();

        String negative = "/Users/thormartin/asterix-machine-learning/src/main/resources/data/twitter-data/training/negative.txt";
        String positive = "/Users/thormartin/asterix-machine-learning/src/main/resources/data/twitter-data/training/positive.txt";

        for (String file : Arrays.asList(negative, positive)) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                String line;
                while ((line = br.readLine()) != null) {
                    analyzer.analyze(line);
                    Term tokens[] = analyzer.getTerms();
                    features.check(tokens);
                    String classValue = file.substring((file.lastIndexOf(".")-8),file.lastIndexOf("."));
                    String featureValues = Arrays.toString(features.getFeatureValues()).replaceAll("\\[|\\]| |\\s", "");
                    featuresList.add(featureValues + "," + classValue);
                }

            } catch (IOException e) {e.printStackTrace();}

        }
        ArffWriter a = new ArffWriter();
        String relation = a.createRelation("tweets");
        String attributes = a.createAttributes(features.getFeatureNames(), features.getFeatureTypes());
        String classAttribute = a.createAttribute("class", "{positive, negative}");
        String data = a.createData(featuresList);
        List<String> lines = Arrays.asList(relation, attributes, classAttribute, data);
        a.writeArff(lines);

    }
}
