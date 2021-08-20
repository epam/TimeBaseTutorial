package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.util.LiveCursorWatcher;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.samples.timebase.BarsGenerator;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;
import org.junit.Test;

import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;


public class Test_EventMessages extends TDBTestBase {

    public DXTickStream getStream(DXTickDB db, String name) throws Introspector.IntrospectionException {
        return getStream(db, name, 0);
    }

    public DXTickStream getStream(DXTickDB db, String name, int df) throws Introspector.IntrospectionException {
        DXTickStream stream = db.getStream(name);
        if (stream != null)
            stream.delete();
        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, name, name, df, descriptor);
        options.bufferOptions = new BufferOptions ();
        return db.createStream(name, options);
    }

    @Test
    public void test1() throws InterruptedException, Introspector.IntrospectionException {

        DXTickDB tickDb = getTickDb();

        DXTickStream stream = getStream(tickDb, "events");

        BarsGenerator gn = new BarsGenerator(
                new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10000,
                "AAPL", "MSFT");

        final TickCursor cursor = tickDb.getStream(TickDBFactory.EVENTS_STREAM_NAME).select(0,
                new SelectionOptions(false, true), null, (CharSequence[]) null);

        final int count[] = new int[1];
        LiveCursorWatcher watcher = new LiveCursorWatcher(cursor, new LiveCursorWatcher.MessageListener() {
            @Override
            public void onMessage(InstrumentMessage m) {
                count[0]++;
                if (count[0] == 2000)
                    cursor.close();
                //System.out.println(cursor.getMessage());
            }
        });

        for (int i = 0; i < 1000; i++) {
            DBLock lock = stream.lock();
            TickLoader loader = stream.createLoader();

            try {
                 while (gn.next()) {
                    loader.send(gn.getMessage());
                 }
            } finally {
                Util.close(loader);
            }

            lock.release();
        }

        watcher.join();

        assertEquals(2000, count[0]);

        stream.delete();
    }

    @Test
    public void test2() throws InterruptedException, Introspector.IntrospectionException {

        BarsGenerator gn = new BarsGenerator(
                new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10000,
                "AAPL", "MSFT");

        DXTickDB client = getTickDb();
        
        DXTickStream stream = getStream(client, "events1");

        final TickCursor cursor = client.getStream(TickDBFactory.EVENTS_STREAM_NAME).select(
                TimeConstants.USE_CURRENT_TIME, new SelectionOptions(false, true), null, (CharSequence[]) null);

        final int count[] = new int[1];
        LiveCursorWatcher watcher = new LiveCursorWatcher(cursor, new LiveCursorWatcher.MessageListener() {
            @Override
            public void onMessage(InstrumentMessage m) {
                count[0]++;
                if (count[0] == 4)
                    cursor.close();
                //System.out.println(cursor.getMessage());
            }
        });

        DBLock lock = stream.lock();
        TickLoader loader = stream.createLoader ();
        try {
             while (gn.next()) {
                loader.send(gn.getMessage());
             }
        } finally {
            Util.close(loader);
        }

        lock.release();

        lock = stream.lock(LockType.READ);
        lock.release();

        watcher.join();

        assertEquals(4, count[0]);
    }
}
