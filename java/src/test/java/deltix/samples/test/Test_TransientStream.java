package deltix.samples.test;


import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.samples.timebase.TradeMessage;
import com.epam.deltix.samples.timebase.TradesGenerator;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.lang.Util;
import org.junit.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.GregorianCalendar;

/**
 *  Test transient stream features.
 */
public class Test_TransientStream  extends  TDBTestBase{
    public static final String      STREAM_KEY = "transient.stream";

    @Test
    public void testSelect() throws Exception {
        StreamOptions               options =
            new StreamOptions (
                StreamScope.TRANSIENT,
                STREAM_KEY,
                "Description Line1\nLine 2\nLine 3",
                1
            );

        options.setFixedType((RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class));
        (options.bufferOptions = new BufferOptions()).lossless = true;

        final DXTickStream      stream = createStream (STREAM_KEY, options);
        TickCursor cursor = stream.select(Long.MAX_VALUE - 1, null, new String[0], new IdentityKey[0]);

        cursor.subscribeToAllEntities();
        cursor.subscribeToAllTypes();

        assertFalse(cursor.next());
    }

    static class TransientStreamTester {
        public TransientStreamTester () {
        }

        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.TRANSIENT,
                    STREAM_KEY,
                    "Description Line1\nLine 2\nLine 3",
                    1
                );

            options.setFixedType((RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class));

            final DXTickStream      stream = createStream (db, STREAM_KEY, options);

            TradeMessage msg = new TradeMessage();

            msg.setSymbol("DLTX");

            TickLoader              loader = stream.createLoader ();

            msg.setTimeStampMs(TimeKeeper.currentTime);
            loader.send (msg);

            loader.close ();

            long []                 tr = stream.getTimeRange ();
        }
    }

     static class TransientStreamTester1 {
        public TransientStreamTester1 () {
        }

        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.TRANSIENT,
                    STREAM_KEY,
                    "Description Line1\nLine 2\nLine 3",
                    1
                );

            options.setFixedType((RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class));

            final DXTickStream      stream = createStream (db, STREAM_KEY, options);

            TradesGenerator generator =
                    new TradesGenerator(
                        new GregorianCalendar(2009, 1, 1),
                            (int) BarMessage.BAR_MINUTE, -1, "DLTX", "ORCL");

            boolean passed = false;
            TickLoader        loader = null;
            try {
                loader = stream.createLoader ();
                int count = 0;
                while (generator.next()) {
                    loader.send(generator.getMessage());
                    count++;
                    if (count == 1000)
                        stream.delete();
                }
                loader.close();
                loader = null;
            }
            catch (WriterAbortedException e) {
                // valid case
                passed = true;
            }
            finally {
                Util.close(loader);
            }
            
            assertTrue(passed);
        }
    }

    @Test (timeout=60000)
    public void             transStreamTestLocal () throws Exception {
        new TransientStreamTester().run (getTickDb());
    }

    @Test (timeout=60000)
    public void             testDelete() throws Exception {
        new TransientStreamTester1().run (getTickDb());
    }    
}
