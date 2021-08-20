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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

/*
    First step - how to connect to Timebase and Create streams
 */

public class Step1_CreatingStreams {

    public static void main(String[] args) {
        String connection = "dxtick://localhost:8011";

        // Create Timebase connection using connection string
        try (DXTickDB db = TickDBFactory.createFromUrl(connection)) {
            // It require to open database connection.
            db.open(false);

            // 1. Create stream using explicit methods for schema definition: creating fields and class descriptors

            // 1.1 Define data fields according to the POJO
            final DataField[]      fields = {
                    new NonStaticDataField ("closePrice", "Close Price", new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
                    new NonStaticDataField ("openPrice", "Open Price", new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true)),
                    new NonStaticDataField ("highPrice", "High", new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true)),
                    new NonStaticDataField ("lowPrice", "Low", new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true)),
                    new NonStaticDataField ("volume", "Volume", new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true)),
                    new NonStaticDataField ("exchange", "Exchange Code", new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true)),
                    new NonStaticDataField ("currency", "Currency Code", new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true))
            };

            // 1.2 Create a Descriptor defines message with given fields
            String typeName = MyBarMessage.class.getName();
            RecordClassDescriptor descriptor = new RecordClassDescriptor(typeName, "My Bar Message", false, null, fields);

            // 1.3 Define stream name
            String streamName = "mybars";

            // 1.4 Define stream Options to creating Durable (Persistent storage) stream with maximum distribution with given descriptor
            StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, streamName, "My Bar Messages Stream", 0, descriptor);

            // 1.5 Create stream in database.
            //  first check if stream already exists in database, and delete it
            DXTickStream bars = db.getStream(streamName);
            if (bars != null)
                bars.delete();

            // create stream
            DXTickStream stream = db.createStream(streamName, options);

            // 1.6 Get new stream options to validate.
            StreamOptions streamOptions = stream.getStreamOptions();

            System.out.println(stream.getKey() + " successfully created with description: " + streamOptions.description);

            // 2. Create stream using implicit methods for schema definition: 'introspecting'

            //
            String newStreamName = "mybars1";

            // 2.1 Introspect MyBarMessage.class to define it's schema
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            descriptor = introspector.introspectRecordClass(MyBarMessage.class);

            // 2.2 Define stream Options to creating Durable (Persistent storage) stream with maximum distribution with given descriptor
            options = StreamOptions.fixedType(StreamScope.DURABLE, newStreamName, "Bar Messages", 0, descriptor);

            // 2.3 Create stream in database.
            //  first check if stream already exists in database, and delete it
            bars = db.getStream(newStreamName);
            if (bars != null)
                bars.delete();

            // create stream
            stream = db.createStream(newStreamName, options);

            System.out.println(stream.getKey() + " successfully created using Introspector with description: " + streamOptions.description);

        } catch (Introspector.IntrospectionException e) {
            e.printStackTrace();
        }
    }
}
