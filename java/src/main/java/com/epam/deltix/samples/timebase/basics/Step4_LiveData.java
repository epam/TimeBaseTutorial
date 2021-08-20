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
package com.epam.deltix.samples.timebase.basics;


import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.util.LiveCursorWatcher;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.GMT;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Step4_LiveData {

    public static final String              STREAM_KEY = "live.stream";

    private static final SimpleDateFormat DF = new SimpleDateFormat ("MM/dd/yyyy");
    private static final TimeZone TZ = TimeZone.getTimeZone ("GMT");

    public static DXTickStream createSampleStream (DXTickDB db) throws Introspector.IntrospectionException {
        DXTickStream            stream = db.getStream (STREAM_KEY);

        if (stream == null) {
            //
            // Create class schema using instrospector
            //
            RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

            //
            // Define stream options
            //
            StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, STREAM_KEY, STREAM_KEY, 0, descriptor);

            //
            // Create stream
            //
            stream = db.createStream (STREAM_KEY, options);
        }

        return stream;
    }

    public static void      loadData (DXTickStream stream) throws ParseException {

        try (TickLoader loader = stream.createLoader ()) {

            //
            // Add listener for errors that may occurs while loading data
            //
            LoadingErrorListener listener = new LoadingErrorListener() {
                @Override
                public void onError(LoadingError e) {
                    System.out.println("Importing error: " + e.getMessage());
                }
            };

            loader.addEventListener(listener);

            //
            //  Always load daily bars in GMT.
            //
            DF.setTimeZone(TZ);
            Calendar calendar = Calendar.getInstance();

            //
            // Create message and reuse it for sending messages
            //
            BarMessage bar = new BarMessage();

            for (int ii = 1; ii < 10000; ii++) {
                bar.setSymbol("AAPL");

                //
                //  bar.timestamp sets RAW timestamp.
                //  Given this, storing data with RAW timestamp of Tuesday - Saturday 00:00:00.
                //  Following if statement skips Sunday/Monday.
                //
                if ((calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) || (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)) {
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
                    continue;
                }

                bar.setTimeStampMs(DF.parse(DF.format(calendar.getTime())).getTime());
                bar.setOpen(ii);
                bar.setHigh(ii + .5);
                bar.setLow(ii - .5);
                bar.setClose(ii + .25);
                bar.setVolume(100000 + ii);

                loader.send(bar);

                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        System.out.println ("Done.");
    }

    public static LiveCursorWatcher      monitorData (DXTickStream stream) {

        //
        //  List of entities to subscribe (if null, all stream entities will be used)
        //
        String[] entities = null;

        //
        //  List of types to subscribe - select 'BarMessage' only
        //
        String[] types = new String[] { BarMessage.class.getName() };

        //
        //  Additional options for select()
        //
        SelectionOptions options = new SelectionOptions();
        options.raw = false; // use decoding
        options.live = true;

        //
        //  Cursor is equivalent to a JDBC ResultSet
        //
        TickCursor              cursor = stream.select (Long.MIN_VALUE, options, types, entities);

        LiveCursorWatcher       watcher = new LiveCursorWatcher(cursor, new LiveCursorWatcher.MessageListener() {
            @Override
            public void onMessage(InstrumentMessage msg) {
                BarMessage bar = (BarMessage) msg;
                System.out.println("Symbol: " + bar.getSymbol() +
                                ", Date: " + GMT.formatDate(bar.getTimeStampMs()) +
                                ", Open: " + bar.getOpen()+
                                ", High: " + bar.getHigh() +
                                ", Low: " + bar.getLow() +
                                ", Close: " + bar.getClose() +
                                ", Volume: " + bar.getVolume()
                );
            }
        });

        return watcher;
    }

    public static void main(String[] args) throws ParseException, Introspector.IntrospectionException {
        if (args.length == 0)
            args = new String[]{"dxtick://localhost:8011"};

        DXTickDB db = TickDBFactory.createFromUrl(args[0]);
        db.open(false);

        LiveCursorWatcher watcher = null;

        try {
            DXTickStream stream = createSampleStream(db);
            watcher = monitorData(stream);
            loadData(stream);

        } finally {
            if (watcher != null)
                watcher.close();
            db.close();
        }
    }
}
