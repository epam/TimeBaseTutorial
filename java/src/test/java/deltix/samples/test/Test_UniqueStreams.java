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

import com.epam.deltix.qsrv.hf.pub.md.*;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.util.lang.Util;


/**
 * Date: Feb 2, 2010
 */
public class Test_UniqueStreams extends TDBTestBase {

    public static class MyMessage extends BarMessage {
        public MyMessage() {
        }

        public String key;

        public double M1;

        public double M2;

        public String test;

        @Override
        public String toString() {
            return "MyMessage [key:" + key + ":" + M1 + "," + M2 + ";" + super.toString() + "]";
        }
    }

    @Test
    public void test() throws Introspector.IntrospectionException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        String name = MyMessage.class.getName();        
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        
        DXTickStream stream = createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.allowLateOutOfOrder = true;
        o.rebroadcast = true;
        TickCursor cursor = null;


        int count = 0;
        stream = db.getStream ("test");
        try {
            cursor = stream.select(System.currentTimeMillis(), o, null, (CharSequence[]) null);
            while (cursor.next()) {
                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(3, count);
    }

    @Ignore
    public void testNull() throws InterruptedException, Introspector.IntrospectionException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("test", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;

        DXTickStream stream = createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }
    }

    @Test
    public void test1() throws InterruptedException, Introspector.IntrospectionException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;

        DXTickStream stream = createStream ("test", options);

        MyMessage msg = new MyMessage();
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            long time = System.currentTimeMillis() - BarMessage.BAR_DAY;
            for (int i = 1; i < 10; i++) {
                msg.M1 = i;
                msg.M2 = i * 10;
                msg.key = String.valueOf(i % 3);
                msg.setTimeStampMs(time + i);
                loader.send(msg);
            }

            loader.removeUnique(msg);
            
            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.allowLateOutOfOrder = true;
        o.rebroadcast = true;
        TickCursor cursor = null;

        int count = 0;
        stream = db.getStream ("test");
        try {
            cursor = stream.select(System.currentTimeMillis(), o, null, (CharSequence[]) null);
            while (cursor.next()) {
                assertTrue(!cursor.getMessage().toString().contains("key:0:0.0"));
                //System.out.println(cursor.getMessage());
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(2, count);
    }

    @Test
    public void testDuplicates() throws InterruptedException, Introspector.IntrospectionException {
        testDuplicates(getTickDb());
    }
    
    @Test
    public void testDuplicates_Remote() throws InterruptedException, Introspector.IntrospectionException {
        testDuplicates(getTickDb());
    }

    public void testDuplicates(DXTickDB db) throws InterruptedException, Introspector.IntrospectionException {

        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        String name = MyMessage.class.getName();
        RecordClassDescriptor rcd = new RecordClassDescriptor(
            name, name, false,
            descriptor,
            new NonStaticDataField("key", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null),
            new NonStaticDataField("M1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null),
            new NonStaticDataField("M2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), false, null)
        );

        StreamOptions options = new StreamOptions (StreamScope.TRANSIENT, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        options.duplicatesAllowed = false;

        DXTickStream stream = db.getStream("test");
        if (stream != null)
            stream.delete();

        stream = createStream ("test", options);

        MyMessage msg = new MyMessage();
        msg.setTimeStampMs(System.currentTimeMillis());
        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());            
            loader = stream.createLoader (lo);

            for (int i = 0; i < 10000; i++) {
                msg.M1 = i % 3;
                msg.M2 = (i % 3) * 10;
                msg.key = "Garbage message_" + String.valueOf(i % 10);
                msg.setSymbol("X" + String.valueOf(i % 3));
                loader.send(msg);
            }

            loader.close();
            loader = null;
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.rebroadcast = true;
        TickCursor cursor = null;

        int count = 0;
        try {

            cursor = stream.select(0, o, null, (CharSequence[]) null);
            while (cursor.next()) {
                count++;
            }
            cursor.close();
            cursor = null;
        } finally {
           Util.close(cursor);
        }

        assertEquals(10, count);
    }

    @Test
    public void testNoKeys() throws Introspector.IntrospectionException {

        DXTickDB db = getTickDb();

        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, null, null, 1);
        options.setFixedType (rcd);
        options.unique = true;
        options.duplicatesAllowed = false;

        DXTickStream stream = db.getStream("test");
        if (stream != null)
            stream.delete();

        stream = createStream ("test", options);

        BarMessage msg = new BarMessage();
        msg.setTimeStampMs(System.currentTimeMillis());

        TickLoader        loader = null;
        try {
            LoadingOptions lo = new LoadingOptions(false);
            lo.typeLoader = new TypeLoaderImpl(MyMessage.class.getClassLoader());
            loader = stream.createLoader (lo);

            for (int i = 0; i < 10; i++) {
                msg.setOpen(i);
                msg.setClose(i % 3);
                msg.setSymbol("X" + (i % 3));
                msg.setTimeStampMs(msg.getTimeStampMs() + 1);

                loader.send(msg);
            }
        }
        finally {
            Util.close(loader);
        }

        SelectionOptions o = new SelectionOptions(false, false);
        o.rebroadcast = true;
        o.allowLateOutOfOrder = true;
        TickCursor cursor = null;

        int count = 0;
        try {
            cursor = stream.select(0, o, null, (CharSequence[]) null);
            while (cursor.next()) {
                //System.out.println(cursor.getMessage());
                count++;
            }
        } finally {
            Util.close(cursor);
        }

        // 10 messages + 3 unique (compared by default primary key {symbol, instrumentType})
        assertEquals(13, count);
    }
}
