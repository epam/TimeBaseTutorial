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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public class HeartBitSample implements DisconnectEventListener {

    private static final Log    LOG = LogFactory.getLog(HeartBitSample.class);
    public final Object         lock = new Object();
    public boolean              connected = true;

    public void run (DXTickStream stream) throws Exception {
        LoadingOptions options = new LoadingOptions();
        options.channelPerformance = ChannelPerformance.LOW_LATENCY;

        options.typeLoader = new TypeLoaderImpl() {
            @Override
            public Class<?> load(ClassDescriptor cd) throws ClassNotFoundException {
                if (HEARTBIT_CLASS.getName().equals(cd.getName()))
                    return HeartBeatMessage.class;

                return super.load(cd);
            }

            @Override
            public void handle(Throwable x) {
                if (x instanceof ClassNotFoundException)
                    LOG.trace("Bind error: type loader is unable to load class. (will try mapping parent): %s").with(x);
                else
                    super.handle(x);
            }
        };

        final InstrumentMessage msg = new HeartBeatMessage();
        msg.setSymbol("HEARTBEAT");

        for (;;) {
            checkConnection();

            try (TickLoader loader = stream.createLoader(options)) {

                for (;;) {
                    loader.send(msg);
                    System.out.println("Sent: " + msg);
                    Thread.sleep(1000 * 2);
                }

            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    /*
        Check that we have Timebase connected. If not - wait for connected event.
     */
    public void checkConnection() throws InterruptedException {
        synchronized (lock) {
            while (!connected)
                lock.wait(5000);
        }
    }

	/*
      Method will be invoked when client lost connection to the Timebase Server
    */ 
    @Override
    public void onDisconnected() {
        synchronized (lock) {
            connected = false;
            lock.notifyAll();
        }
    }

	/*
      Method will be invoked when client restored connection to the Timebase Server
    */ 
    @Override
    public void onReconnected() {
        synchronized (lock) {
            connected = true;
            lock.notifyAll();
        }
    }

    public static void main (String [] args) throws Exception {
        try (DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8011")) {
            db.open(false);

            String key = "myevents";
            DXTickStream stream = db.getStream(key);
            if (stream == null)
                stream = db.createStream(key, StreamOptions.fixedType(StreamScope.DURABLE, key, key, 1, HEARTBIT_CLASS));

            HeartBitSample sample = new HeartBitSample();
            ((Disconnectable) db).addDisconnectEventListener(sample);
            sample.run(stream);
            ((Disconnectable) db).removeDisconnectEventListener(sample);
        }
    }

    public static final RecordClassDescriptor HEARTBIT_CLASS =
            new RecordClassDescriptor (
                    "deltix.samples.timebase.basics.HeartBeatMessage",
                    null,
                    false,
                    null,
                    new NonStaticDataField(
                            "time",
                            "Time",
                            new DateTimeDataType(true),
                            null
                    )
            );

    public static class HeartBeatMessage extends InstrumentMessage {

        public long time;

        public HeartBeatMessage() {
        }
    }
}
