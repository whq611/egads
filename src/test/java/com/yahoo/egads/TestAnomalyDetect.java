/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.egads;

import com.yahoo.egads.models.tsmm.OlympicModel;
import com.yahoo.egads.models.adm.*;
import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.*;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

// Tests the basic anoamly detection piece of EGADS.
public class TestAnomalyDetect {

    @Test
    public void testOlympicModel() throws Exception {
        // Test cases: ref window: 10, 5
        // Drops: 0, 1
        String[] refWindows = new String[]{"10", "5"};
        String[] drops = new String[]{"0", "1"};
        // Load the true expected values from a file.
        ArrayList<TimeSeries> actual_metric = com.yahoo.egads.utilities.FileUtils
                .createTimeSeries("src/test/resources/model_input.csv");
        for (int w = 0; w < refWindows.length; w++) {
            for (int d = 0; d < drops.length; d++) {
                 String configFile = "src/test/resources/sample_config.ini";
                 InputStream is = new FileInputStream(configFile);
                 Properties p = new Properties();
                 p.load(is);
                 p.setProperty("NUM_WEEKS", refWindows[w]);
                 p.setProperty("NUM_TO_DROP", drops[d]);
                 p.setProperty("THRESHOLD", "mapee:100,mase:10");
                 // Parse the input timeseries.
                 ArrayList<TimeSeries> metrics = com.yahoo.egads.utilities.FileUtils
                            .createTimeSeries("src/test/resources/model_output_" + refWindows[w] + "_" + drops[d] + ".csv");
                 OlympicModel model = new OlympicModel(p);
                 model.train(actual_metric.get(0).data);
                 TimeSeries.DataSequence sequence = new TimeSeries.DataSequence(metrics.get(0).startTime(),
                                                                                metrics.get(0).lastTime(),
                                                                                new Long(p.getProperty("PERIOD")));
                 sequence.setLogicalIndices(metrics.get(0).startTime(), new Long(p.getProperty("PERIOD")));
                 model.predict(sequence);
                 // Initialize the anomaly detector.
                 ExtremeLowDensityModel bcm = new ExtremeLowDensityModel(p);
                 IntervalSequence anomalies = bcm.detect(actual_metric.get(0).data, sequence);
                 Assert.assertTrue(anomalies.size() > 10);
            }
        }
    }
}
