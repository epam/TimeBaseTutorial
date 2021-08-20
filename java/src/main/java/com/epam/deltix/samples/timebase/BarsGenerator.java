package com.epam.deltix.samples.timebase;

import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.GregorianCalendar;
import java.util.Random;

public class BarsGenerator extends BaseGenerator<InstrumentMessage>
{
    private Random rnd = new Random();
    private int count;

    public BarsGenerator(GregorianCalendar calendar, int interval, int count, String ... tickers) {
        super(calendar, interval, tickers);
        this.count = count;
    }

    public boolean next() {
        if (isAtEnd())
            return false;

        BarMessage message = new BarMessage();
        message.setSymbol(symbols.next());
        message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

        message.setHigh(getDouble(100, 6));
        message.setOpen(message.getHigh() - getDouble(10, 6));
        message.setClose(message.getHigh() - getDouble(10, 6));
        message.setLow(Math.min(message.getOpen(), message.getClose()) + getDouble(10, 6));
        message.setVolume(count);

        current = message;
        count--;
        return true;
    }

    public double getDouble(int max, int precision) {
        return (int)(rnd.nextDouble() * Math.pow(10, max + precision)) / Math.pow(10, precision);
    }

    public boolean isAtEnd() {
        return count == 0;
    }
}
