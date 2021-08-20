package com.epam.deltix.samples.timebase.basics;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.GMT;

/**
 * POJO class to store Stock Market information like Bars in Timebase stream
 */

public class MyBarMessage extends InstrumentMessage {

    public double closePrice;

    public double openPrice;

    public double highPrice;

    public double lowPrice;

    public double volume;

    public String exchange;

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("{ \"$symbol\": \"").append(symbol).append("\"");
        sb.append(",  \"$type\":  \"MyBarMessage\"");
        if (hasTimeStampMs()) {
            sb.append(", \"time\": \"").append(GMT.formatDateTimeMillis(timestamp)).append("\"");
        }

        sb.append(", \"closePrice\": ").append(closePrice);
        sb.append(", \"openPrice\": ").append(openPrice);
        sb.append(", \"highPrice\": ").append(highPrice);
        sb.append(", \"lowPrice\": ").append(lowPrice);
        sb.append(", \"volume\": ").append(volume);
        sb.append("}");

        return sb;
    }
}
