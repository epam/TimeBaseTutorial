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
package deltix.samples.test;


import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.samples.timebase.*;
import com.epam.deltix.samples.timebase.DataGenerator;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;

public class Test_IncrementalUpdates extends TDBTestBase {


    public void testUpdate() {

        DXTickDB db = getTickDb();
        DXTickStream stream = db.getStream("zzz");
        long[] range = stream.getTimeRange();

        GregorianCalendar c = new GregorianCalendar(2010, 1, 1);
        c.setTimeInMillis(range[1] - BarMessage.BAR_DAY);
        DataGenerator generator = new DataGenerator(c, 1000, "MSFT");

        for (int i = 0; i < 100; i++) {
           c.add(Calendar.HOUR, -1);
           update(stream, generator, 10000);
        }
    }

    public void testUpdate1() {

        DXTickDB db = getTickDb();
        DXTickStream stream = db.getStream("zzz");
        long[] range = stream.getTimeRange();

        GregorianCalendar c = new GregorianCalendar(2000, 1, 1);
        //c.setTimeInMillis(range[1] - BarMessage.BAR_DAY);
        DataGenerator generator = new DataGenerator(c, 1000, "MSFT");

        for (int i = 0; i < 10; i++) {
           c.add(Calendar.HOUR, -1);
           update(stream, generator, 10000);
        }
    }

    @Test
    public void test() throws InterruptedException, Introspector.IntrospectionException {

        RecordClassDescriptor bbo = (RecordClassDescriptor) Introspector.introspectSingleClass(BestBidOfferMessage.class);
        RecordClassDescriptor trade = (RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class);

        DXTickDB db = getTickDb();
        if (!db.isOpen())
            db.open(false);

        DXTickStream st = db.getStream("zzz");
        if (st != null)
            st.delete();

        DXTickStream stream = db.createStream("zzz",
                StreamOptions.polymorphic(StreamScope.DURABLE, "zzz", null, 1, bbo, trade));

        DataGenerator generator = new DataGenerator(new GregorianCalendar(2000, 1, 1), 1000,
                "MSFT", "ORCL");

        TickLoader loader = stream.createLoader(new LoadingOptions());

        try {
            long count = 0;
            while (generator.next() && count < 100000) {
                loader.send(generator.getMessage());
                count++;
            }
        } finally {
            Util.close(loader);
        }

        db.close();
        db.open(false);

        testUpdate();

        testRange(db.getStream("zzz"));

        testUpdate();

        testRange(db.getStream("zzz"));

        db.close();
    }

    public static void testRange(DXTickStream stream) {

        long[] range = stream.getTimeRange();

        long[] rangeAll = stream.getTimeRange(stream.listEntities());

        assertEquals(GMT.formatDateTimeMillis(range[0]) + " : " + GMT.formatDateTimeMillis(rangeAll[0]), range[0], rangeAll[0]);
        assertEquals(GMT.formatDateTimeMillis(range[1]) + " : " + GMT.formatDateTimeMillis(rangeAll[1]), range[1], rangeAll[1]);
    }

    @Test
    public void test1() throws InterruptedException, Introspector.IntrospectionException {

        RecordClassDescriptor bbo = (RecordClassDescriptor) Introspector.introspectSingleClass(BestBidOfferMessage.class);
        RecordClassDescriptor trade = (RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class);

        DXTickDB db = getTickDb();

        if (!db.isOpen())
            db.open(false);

        DXTickStream st = db.getStream("zzz");
        if (st != null)
            st.delete();

        DXTickStream stream = db.createStream("zzz",
                StreamOptions.polymorphic(StreamScope.DURABLE, "zzz", null, 1, bbo, trade));

        DataGenerator generator = new DataGenerator(new GregorianCalendar(2000, 1, 1), 1000,
                "MSFT", "ORCL");

//        TickLoader loader = stream.createLoader(new LoadingOptions());
//
//        try {
//            long count = 0;
//            while (generator.next() && count < 10) {
//                loader.send(generator.getMessage());
//                count++;
//            }
//        } finally {
//            Util.close(loader);
//        }

        testUpdate1();

        testUpdate1();

    }

    public void update(DXTickStream stream, DataGenerator gen, int count) {
        TickLoader loader = stream.createLoader();

        int total = 0;
        while (gen.next() && total < count) {
            //System.out.println(gen.getMessage());
            loader.send(gen.getMessage());
            total++;
        }

        loader.close();
    }
}
