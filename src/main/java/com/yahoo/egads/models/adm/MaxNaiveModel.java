/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A simple model that does not require a forecasting model.
// It looks weather or not the max value for the past N hours exceeds
// X %.

package com.yahoo.egads.models.adm;

import java.util.Properties;
import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.Anomaly.Interval;
import com.yahoo.egads.data.TimeSeries.DataSequence;
import com.yahoo.egads.utilities.Storage;
import com.yahoo.egads.data.AnomalyErrorStorage;

import org.json.JSONObject;
import org.json.JSONStringer;

public class MaxNaiveModel extends AnomalyDetectionAbstractModel {

    // The constructor takes a set of properties
    // needed for the simple model. This includes the sensitivity.
    private Float[] threshold;
    private int maxHrsAgo;
    // modelName.
    private static final String modelName = "MaxNaive-NA";
    private AnomalyErrorStorage aes = new AnomalyErrorStorage();
    
    public MaxNaiveModel(Properties config) {
        super(config);

        if (config.getProperty("MAX_ANOMALY_TIME_AGO") == null) {
            throw new IllegalArgumentException("MAX_ANOMALY_TIME_AGO is NULL");
        }
        this.maxHrsAgo = new Integer(config.getProperty("MAX_ANOMALY_TIME_AGO"));
        if (config.getProperty("THRESHOLD") == null) {
        	throw new IllegalArgumentException("THRESHOLD is NULL");
        }
        this.threshold = new Float[]{new Float(config.getProperty("THRESHOLD"))};
    }
    
    public void toJson(JSONStringer json_out) {

    }

    public void fromJson(JSONObject json_obj) {

    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public String getType() {
        return "point_outlier";
    }

    @Override
    public void reset() {
        // At this point, reset does nothing.
    }

    private Float max(DataSequence d, int from, int to) {
        Float max = (float) 0.0;
        
        for (int i = from; i < to; i++) {
            if (d.get(i).value > max) {
                max = d.get(i).value;
            }
        }
        return max;
    }
    
    @Override
    public void tune(DataSequence observedSeries, DataSequence expectedSeries,
            IntervalSequence anomalySequence) {
    }
   
    private boolean isAnomaly(Float[] error, Float[] threshold) {
        return error[0] >= threshold[0];
    }
    
    @Override
    public IntervalSequence detect(DataSequence observedSeries,
            DataSequence expectedSeries) {
        
        IntervalSequence output = new IntervalSequence();
        int n = observedSeries.size();
        int cutIndex = (n - maxHrsAgo);
        
        Float maxNow = max(observedSeries, cutIndex, n);
        Float maxBefore = max(observedSeries, 0, Math.max(0, cutIndex - 1));
        if (maxBefore == 0.0) {
            return output;
        }
        
        Float buzzScore = maxNow - maxBefore;
        Float[] error = new Float[1];
        if ((maxNow + maxBefore) == 0) {
        	error[0] = (float) 0;
        } else {
        	error[0] = buzzScore * (Math.abs(buzzScore) / (maxNow + maxBefore));
        }
        
        if (Storage.debug == 3) {
            System.out.println("MaxNaiveModel: CI " + cutIndex + " n " + n +
                               " maxNow " + maxNow + " maxBefore " + maxBefore + " Error " + error[0]);
        }
        if (isAnomaly(error, threshold)) {
            Float locMax = Float.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int i = cutIndex; i < n; i++) {
                if (observedSeries.get(i).value > locMax) {
                    locMax = observedSeries.get(i).value;
                    maxIndex = i;
                }
            }
            
            output.add(new Interval(observedSeries.get(maxIndex).time,
                       error,
                       threshold,
                       maxNow,
                       maxBefore,
                       isAnomaly(error, threshold)));
            return output;
        }
        return output;
      
    }
}
