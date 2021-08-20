/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
