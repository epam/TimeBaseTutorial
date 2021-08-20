package com.epam.deltix.samples.timebase.advanced;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.Interval;

import java.util.Arrays;

/**
 *  This example shows how to synchronize data in two streams. For convenience,
 *  it also auto-creates the stream and has a built-in test fixture
 *  that adds more data between synchronization sessions.
 *  <p>
 *  The following limitations must be understood:
 *  </p>
 *  <ul>
 *  <li>This code does not account for the possibility of any additional mutation of
 *      the target stream, or for any retroactive corrections in the source
 *      stream. We assume that for every symbol the last known timestamp in
 *      the target stream is the only necessary consideration for
 *      synchronization.</li>
 *  <li>This code may not work correctly if the source stream is still being
 *      added to while synchronization is underway.</li>
 *  <li>This code will not work with custom types.</li>
 *  </ul>
 */
public class Synchronizer {
    private static long             timestampForGeneratedData =
        System.currentTimeMillis () / 1000 * 1000; // truncate milliseconds

    public static DXTickStream      createSampleStream (DXTickDB db, String key, int df) throws Introspector.IntrospectionException {
        //
        //  Create a one-second bar stream
        //
        DXTickStream            stream = db.getStream (key);

        if (stream != null)
            stream.delete ();
        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);
        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, key, "Test 1-second bar stream for\nthe Synchronizer sample.", 0, descriptor);
        stream =
            db.createStream (
                key,
                options
            );
        //
        // Passing stream name, exchange code, currency code, and bar size.
        //
        //StreamConfigurationHelper.setBar (stream, "XNYS", 840, Interval.SECOND);
        
        return (stream);
    }

    public static void      generateTestData (
        DXTickStream            stream,
        String ...              symbols
    )
    {
        //  Create a reusable BarMessage object
        BarMessage bar = new BarMessage();

        TickLoader              loader = stream.createLoader ();

        //  Generate 60 messages per symbol
        for (int ii = 0; ii < 60; ii++) {
            bar.setTimeStampMs(timestampForGeneratedData);

            for (String s : symbols) {
                bar.setSymbol(s);
                //
                //  Fill with fairly meaningless data; this is not essential to
                //  the synchronization issue being illustrated.
                //
                bar.setOpen(ii + 10);
                bar.setClose(ii + 11);
                bar.setHigh(ii + 12);
                bar.setLow(ii + 9);
                bar.setVolume(ii + 1);

                loader.send (bar);
            }

            timestampForGeneratedData += 1000;
        }

        loader.close ();        
    }

    public static void      printDimensions (DXTickStream s) {
        System.out.println ("Dimensions of stream " + s.getName () + ": [");

        IdentityKey[]   ids = s.listEntities ();

        Arrays.sort (ids);

        for (IdentityKey id : ids) {
            long []             tr = s.getTimeRange (id);

            System.out.print (" " + id + ": ");

            if (tr == null)
                System.out.println ("NO DATA");
            else
                System.out.println (
                    GMT.formatDateTime (tr [0]) + " .. " +
                    GMT.formatDateTime (tr [1])
                );
        }

        System.out.println ("]");
    }

    public static void      synchronize (DXTickStream source, DXTickStream target) {
        long            globalStartTime = Long.MAX_VALUE;

        for (IdentityKey id : source.listEntities ()) {
            long []     sourceRange = source.getTimeRange (id);

            if (sourceRange == null) {
                //
                //  No data in source. Can happen in real life,
                //  but should not happen in this test
                //
                System.out.println ("No data in source stream for " + id + "; skipping...");
                continue;
            }

            long []     targetRange = target.getTimeRange (id);
            
            if (targetRange == null) {
                //
                //  Target stream has no data for this entity.
                //
                if (globalStartTime > sourceRange [0])
                    globalStartTime = sourceRange [0];
            }
            else {
                //
                //  Give a warning if source time ranges do not match.
                //
                if (targetRange [0] != sourceRange [0]) 
                    System.out.println (
                        "Warning: " + id + " has source data starting at " +
                        GMT.formatDateTime (sourceRange [0]) +
                        ",\n    but target data starting at " +
                        GMT.formatDateTime (targetRange [0]) +
                        ".\n    This discrepancy is reported, but ignored."
                    );
                //
                //  While this is in no way a complete consistency check, 
                //  at least check that target does not have data that is
                //  LATER than source.
                //
                if (targetRange [1] > sourceRange [1]) {
                    System.out.println (
                        "Error: " + id + " has target data ending at " +
                        GMT.formatDateTime (targetRange [1]) +
                        ",\n    which is LATER than source data ending at " +
                        GMT.formatDateTime (sourceRange [1]) +
                        ".\n    Synchronization of this symbol is aborted."
                    );

                    continue;
                }

                if (globalStartTime > targetRange [1])
                    globalStartTime = targetRange [1];
            }
        }
        //
        //  Select all source data beginning at globalStartTime
        //  and load it into target.
        //
        InstrumentMessageSource     cur = null;
        TickLoader                  loader = null;

        try {
            cur = source.createCursor (null);
            loader = target.createLoader ();
            //
            //  Download the increment.
            //
            cur.reset (globalStartTime);
            cur.subscribeToAllEntities ();
            //
            //  The actual copying is trivial, as follows:
            //
            while (cur.next ())
                loader.send (cur.getMessage ());
        } finally {
            if (cur != null)
                cur.close ();

            if (loader != null)
                loader.close ();
        }
    }

    public static void      main (String [] args) throws Exception {
        String      sourceUrl;
        String      targetUrl;
        String      sourceKey = "synchronizer.source";
        String      targetKey = "synchronizer.target";
        //
        //  A few different ways to send command arguments...
        //
        switch (args.length) {
            case 0:
                sourceUrl = targetUrl = "dxtick://localhost:8011";
                break;
                
            case 1:
                sourceUrl = targetUrl = args [0];
                break;

           case 2:
                sourceUrl = args [0];
                targetUrl = args [1];
                break;

            case 3:
                sourceUrl = targetUrl = args [0];
                sourceKey = args [1];
                targetKey = args [2];
                break;

            case 4:
                sourceUrl = args [0];
                targetUrl = args [1];
                sourceKey = args [2];
                targetKey = args [3];
                break;

            default:
                throw new Exception ("Too many arguments");
        }

        DXTickDB    sourceDb = TickDBFactory.createFromUrl (sourceUrl);
        DXTickDB    targetDb = TickDBFactory.createFromUrl (targetUrl);
        //
        //  Opening source as Read-Write because we will be generating test data.
        //  For pure synchronization, it should be opened as Read-Only.
        //
        sourceDb.open (false);
        targetDb.open (false);

        try {
            //
            //  Test preparation. Create two identical streams with DF=MAX.
            //
            DXTickStream    sourceStream = 
                createSampleStream (sourceDb, sourceKey, StreamOptions.MAX_DISTRIBUTION);

            DXTickStream    targetStream =
                createSampleStream (targetDb, targetKey, StreamOptions.MAX_DISTRIBUTION);
            //
            //  Add some data to source stream and print it out
            //
            generateTestData (sourceStream, "ORCL", "GOOG");
            //
            //  Wait for the data to be digested.
            //
            //
            //  Print test data
            //
            printDimensions (sourceStream);
            //
            //  Finally! We are ready to synchronize data.
            //
            synchronize (sourceStream, targetStream);

            //
            //  Now we should see the test data - synchronized.
            //
            printDimensions (targetStream);
            //
            //  Add some more test data, including new symbols and some old
            //
            generateTestData (sourceStream, "ORCL", "IBM", "AAPL");            
            //
            //  Print test data again
            //
            printDimensions (sourceStream);
            //
            //  Finally! We are ready to synchronize data.
            //
            synchronize (sourceStream, targetStream);                        
            //
            //  Now we should see the test data - synchronized.
            //
            printDimensions (targetStream);

        } finally {
            targetDb.close ();
            sourceDb.close ();
        }
    }
}
