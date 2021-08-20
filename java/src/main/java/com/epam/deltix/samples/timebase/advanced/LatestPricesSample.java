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
package com.epam.deltix.samples.timebase.advanced;

import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.samples.timebase.BestBidOfferMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.TimeKeeper;


public class LatestPricesSample {
    public static final long    REPORT_INTERVAL = 1000;

    static class PriceInfo {
        public double offerSize = 0;
        public double offerPrice = Double.NaN;

        public double bidSize = 0;
        public double bidPrice = Double.NaN;

        void        process(BestBidOfferMessage msg) {
            if (Double.isNaN(bidPrice)) {
                if (msg.getBidSize() > 0 && !Double.isNaN(msg.getBidPrice())) {
                    bidPrice = msg.getBidPrice();
                    bidSize = msg.getBidSize();
                }
            }

            if (Double.isNaN(offerPrice)) {
                if (msg.getOfferSize() > 0 && !Double.isNaN(msg.getOfferPrice())) {
                    offerPrice = msg.getOfferPrice();
                    offerSize = msg.getOfferSize();
                }
            }
        }

        boolean isComplete() {
            return !Double.isNaN(offerPrice) && !Double.isNaN(bidPrice);
        }
    }

    public static void      readData (DXTickDB db, String streamKey) {
        DXTickStream                            stream = 
            db.getStream (streamKey);
        
        //
        //  Create a tabel of indicators, one per instrument
        //
        InstrumentToObjectMap <PriceInfo>     prices =
            new InstrumentToObjectMap <> ();
        
        //
        //  InstrumentMessageSource is similar to a JDBC ResultSet
        //

        SelectionOptions options = new SelectionOptions(false, false);
        options.reversed = true;

        InstrumentMessageSource cursor = stream.createCursor (options);
        // subscribe BBO's only
        cursor.setTypes(BestBidOfferMessage.CLASS_NAME);
        
        cursor.reset (System.currentTimeMillis());          // read from current time
        cursor.subscribeToAllEntities ();

        //
        //  Measure read performance
        //
        long                    startTime = TimeKeeper.currentTime;
        long                    count = 0;        
        
        try {
            while (cursor.next ()) {
                InstrumentMessage msg = cursor.getMessage ();

                if (msg instanceof BestBidOfferMessage) {
                    BestBidOfferMessage      bbo = (BestBidOfferMessage) msg;

                    PriceInfo info = prices.get (bbo);
                    
                    if (info == null) {
                        // System.out.println ("Creating a new price info for " + bbo.symbol + " ...");
                        
                        info = new PriceInfo ();
                        prices.put (bbo, info);
                    }
                    
                    info.process (bbo);

                    // waiting for both sides of BBOs
                    // unsubscribe when both side recieved
                    if (info.isComplete()) {
                        cursor.removeEntity(bbo);
                        System.out.println ("Removing instrument from subscription " + bbo.getSymbol() + " ...");
                    }

                }

                count++;
                
                long            now = TimeKeeper.currentTime;
                long            elapsed = now - startTime;
                
                if (elapsed >= REPORT_INTERVAL) {
                    System.out.println (String.format("%,9.0f messages/s\r", count * 1000.0 / elapsed));
                    startTime = now;
                    count = 0;
                }
            }
        } finally {
            cursor.close ();
        }

        System.out.println ("\nDone.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "dxtick://localhost", "playback" };

        DXTickDB    db = TickDBFactory.createFromUrl (args [0]);

        db.open (true);

        try {
            readData (db, args [1]);
        } finally {
            db.close ();
        }
    }
}
