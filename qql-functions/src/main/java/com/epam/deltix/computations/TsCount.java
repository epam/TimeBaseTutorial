package com.epam.deltix.computations;


import com.epam.deltix.computations.api.annotations.*;

@Function("TSCOUNT")
public class TsCount {

    private long timestamp = Long.MIN_VALUE;
    private long count = 0;

    @Compute
    public void increment(@BuiltInTimestampMs long timestamp) {
        if (this.timestamp != timestamp) {
            count = 0;
        }
        this.timestamp = timestamp;
        count++;
    }

    @Result
    public long get() {
        return count;
    }

    @Reset
    public void reset() {
        count = 0;
    }

}
