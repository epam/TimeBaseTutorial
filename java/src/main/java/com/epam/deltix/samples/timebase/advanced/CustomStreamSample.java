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

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class CustomStreamSample {
    public static final String      STREAM_KEY = "custom.stream";

    public static final RecordClassDescriptor   CUSTOM_CLASS =
        new RecordClassDescriptor (
            "MyClass",
            "My Custom Class Title",
            false,
            null,
            new NonStaticDataField (
                "price",
                "Price (FLOAT)",
                new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true),
                null
            ),
            new NonStaticDataField (
                "count",
                "Count (INTEGER)",
                new IntegerDataType (IntegerDataType.ENCODING_INT32, true),
                null
            ),
            new NonStaticDataField (
                "description",
                "Description (VARCHAR)",
                new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true),
                null
            ),
            new NonStaticDataField (
                "dueDate",
                "Due Date (DATE)",
                new DateTimeDataType (true),
                null
            ),
            new NonStaticDataField (
                "accepted",
                "Accepted (BOOLEAN)",
                new BooleanDataType (true),
                null
            )
        );
    
    public static void      createSampleStream (DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);

        if (stream == null) {
            stream =
                db.createStream (
                    STREAM_KEY,
                    STREAM_KEY,
                    "Description Line1\nLine 2\nLine 3",
                    0
                );

            stream.setFixedType (CUSTOM_CLASS);
        }
    }

    public static void      readData (DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);
        RecordClassDescriptor   classDescriptor = stream.getFixedType ();

        //
        //  Always use raw = true for custom messages.
        //
        SelectionOptions        options = new SelectionOptions (true, false);

        //
        //  List of entities to subscribe (if null, all stream entities will be used)
        //
        IdentityKey[] entities = null;

        //
        //  List of types to subscribe - select only "MyClass" messages
        //
        String[] types = new String[] { "MyClass" };

        //
        //  Cursor is equivalent to a JDBC ResultSet
        //
        TickCursor              cursor = stream.select (Long.MIN_VALUE, options, types, entities);
        
        MemoryDataInput in = new MemoryDataInput ();
        UnboundDecoder          decoder =
            CodecFactory.COMPILED.createFixedUnboundDecoder (classDescriptor);
        
        try {
            while (cursor.next ()) {
                //
                //  We can safely cast to RawMessage because we have requested
                //  a raw message cursor.
                //
                RawMessage      msg = (RawMessage) cursor.getMessage ();
                //
                //  Print out standard fields
                //
                System.out.printf (
                    "%tT.%<tL %s",
                        msg.getTimeStampMs(),
                        msg.getSymbol()
                );
                //
                //  Iterate over custom fields.
                //
                in.setBytes (msg.data, msg.offset, msg.length);
                decoder.beginRead (in);

                while (decoder.nextField ()) {
                    NonStaticFieldInfo  df = decoder.getField ();
                    //
                    //  Any data type can be retrieved via getString ()
                    //  but in most cases specific get... methods should be
                    //  invoked, e.g. getLong (), getDouble (), etc.
                    //
                    System.out.printf (",%s: %s", df.getName (), decoder.getString ());
                }

                System.out.println ();
            }
        } finally {
            cursor.close ();
        }
    }

    public static void writeIntoStream(DXTickDB db) {
        DXTickStream            stream = db.getStream (STREAM_KEY);
        RecordClassDescriptor   classDescriptor = stream.getFixedType ();
        RawMessage              msg = new RawMessage (classDescriptor);

        //  Always use raw = true for custom messages.
        LoadingOptions          options = new LoadingOptions (true); 
        TickLoader              loader = stream.createLoader (options);

        //  Re-usable buffer for collecting the encoded message
        MemoryDataOutput dataOutput = new MemoryDataOutput ();
        FixedUnboundEncoder     encoder =
            CodecFactory.COMPILED.createFixedUnboundEncoder (classDescriptor);

        try {
            //  Generate a few messages
            for (int ii = 1; ii < 100; ii++) {
                //
                //  Set up standard fields
                //
                msg.setTimeStampMs(System.currentTimeMillis ());
                msg.setSymbol("AAPL");
                //
                //  Set up custom fields
                //
                dataOutput.reset ();
                encoder.beginWrite (dataOutput);
                //
                //  Fields must be set in the order the encoder
                //  expects them, which in the case of a fixed-type stream
                //  with a non-inherited class descriptor is equivalent to the
                //  order of the class descriptor's fields.
                //
                encoder.nextField ();
                encoder.writeDouble (ii * 0.25);

                encoder.nextField ();   // count
                encoder.writeInt (ii);

                encoder.nextField ();   // description
                encoder.writeString ("Message #" + ii);

                encoder.nextField ();   // dueDate
                encoder.writeLong (msg.getTimeStampMs() + 864000000L); // add 10 days

                encoder.nextField ();   // accepted
                encoder.writeBoolean (ii % 2 == 0);

                if (encoder.nextField ())   // make sure we are at end
                    throw new RuntimeException ("unexpected field: " + encoder.getField ().toString ());

                msg.setBytes (dataOutput, 0);

                loader.send (msg);
            }
        } finally {
            loader.close ();
        }
    }

    public static void      main (String [] args) {
        if (args.length == 0)
            args = new String [] { "dxtick://localhost:8011" };

        DXTickDB    db = TickDBFactory.createFromUrl (args [0]);
        
        db.open (false);

        try {
            createSampleStream (db);
            writeIntoStream (db);            
            readData (db);
        } finally {
            db.close ();
        }
    }
}
