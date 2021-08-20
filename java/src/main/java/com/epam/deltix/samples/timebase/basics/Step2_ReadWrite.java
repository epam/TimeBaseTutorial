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

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

/*
    Second step - how to read and write data to the streams
 */

public class Step2_ReadWrite {

    public static void main(String[] args) {
        String connection = "dxtick://localhost:8011";

        // 1. Create Timebase connection using connection string
        try (DXTickDB db = TickDBFactory.createFromUrl(connection)) {

            // It require to open database connection.
            db.open(false);

            // 2. Get already created stream from database
            String streamName = "mybars";
            DXTickStream stream = db.getStream(streamName);

            //
            // 3. Writing new messages to the stream
            //

            // 3.1 Create Loader
            try (TickLoader loader = stream.createLoader()) {

                // 3.2 Create message to send
                MyBarMessage message = new MyBarMessage();

                // 3.3 set time for this message
                LocalDateTime localTime = LocalDateTime.of(2020, Month.MARCH, 9, 0, 0, 0);
                Instant utc = localTime.atZone(ZoneId.of("UTC")).toInstant();
                message.setTimeStampMs(utc.toEpochMilli());

                // 3.4 define field values
                message.setSymbol("AAPL"); // Apple
                message.openPrice = 263.75;
                message.highPrice = 278.09;
                message.lowPrice = 263.00;
                message.closePrice = 266.17;
                message.volume = 71_690_000;
                message.exchange = "NYSE";

                // 3.5. send first message
                System.out.println("Sending 1st message: " + message);
                loader.send(message);

                // 3.6. reuse message to send another
                message.setSymbol("GOOG"); // Apple
                message.openPrice = 1205.3;
                message.highPrice = 1254.76;
                message.lowPrice = 1200.00;
                message.closePrice = 1215.56;
                message.volume = 33_700_000;

                // 3.7. send second message
                System.out.println("Sending 2nd message: " + message);
                loader.send(message);
            }

            //
            // 4. Read data from the stream
            //

            // 4.1. Define List of entities to subscribe (if null, all stream entities will be used)
            // Use GOOG

            String[] entities = new String[] { "GOOG"};

            // 4.2. Define list of types to subscribe - select only "MyBarMessage" messages
            String typeName = MyBarMessage.class.getName();
            String[] types = new String[] { typeName };


            // 4.3. Create a new 'cursor' to read messages
            try (TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(), types, entities)) {

                // 4.4 Iterate cursor and get messages
                while (cursor.next()) {
                    InstrumentMessage message = cursor.getMessage();
                    System.out.println("Read message: " + message.toString());

                }
            }
        }
    }
}
