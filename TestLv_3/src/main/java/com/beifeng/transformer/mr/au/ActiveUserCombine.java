package com.beifeng.transformer.mr.au;

import java.io.IOException;

import com.beifeng.transformer.model.dim.StatsUserDimension;
import org.apache.hadoop.mapreduce.Reducer;

import com.beifeng.transformer.model.value.map.TimeOutputValue;

/**
 * combine类
 * 
 * @author gerry
 *
 */
public class ActiveUserCombine extends Reducer<StatsUserDimension, TimeOutputValue, StatsUserDimension, TimeOutputValue> {
    @Override
    protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context) throws IOException, InterruptedException {
        for (TimeOutputValue tov : values) {
            context.write(key, tov);
        }
    }
}
