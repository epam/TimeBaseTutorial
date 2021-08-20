package com.epam.deltix.samples.timebase;

import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.GregorianCalendar;
import java.util.Random;

public class TradesGenerator extends BaseGenerator<InstrumentMessage>
{
    private Random rnd = new Random();
    private int count;

    public TradesGenerator(GregorianCalendar calendar, int size, int count, String ... tickers) {
        super(calendar, size, tickers);
        this.count = count;
    }

    public boolean next() {
        if (isAtEnd())
            return false;

        TradeMessage message = new TradeMessage();
        message.setSymbol(symbols.next());
        message.setTimeStampMs(symbols.isReseted() ? getNextTime() : getActualTime());

        message.setPrice(rnd.nextDouble()*100);
        message.setSize(rnd.nextInt(1000));

        current = message;
        count--;
        return true;
    }

    public boolean isAtEnd() {
        return count == 0;
    }
}
