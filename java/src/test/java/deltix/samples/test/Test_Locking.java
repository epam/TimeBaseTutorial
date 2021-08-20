package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.EventMessage;
import com.epam.deltix.timebase.messages.service.EventMessageType;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test Locking streams
 */
public class Test_Locking extends TDBTestBase {

    static RecordClassDescriptor BAR_DESCRIPTOR;

    static {
        try {
            BAR_DESCRIPTOR = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);
        } catch (Introspector.IntrospectionException e) {
            e.printStackTrace();
        }
    }

    public DXTickStream createTestStream(String name) {
        return createStream(getTickDb(), name, BAR_DESCRIPTOR);
    }

    @Test
    public void test0() {
        DXTickStream stream = createTestStream("lock_test0");

        DBLock lock = stream.lock(LockType.READ);
        try {
            stream.lock(LockType.WRITE);
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        lock.release();

        DBLock lock1 = stream.lock(LockType.READ);
        DBLock lock2 = stream.lock(LockType.READ);

        lock1.release();
        lock2.release();
        
        stream.delete();
    }

    @Test // create loader under exclusive lock
    public void test1() {
        DXTickStream stream = createTestStream("lock_test1");

        stream.lock();
        TickLoader loader = stream.createLoader();
        loader.close();

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test1");

        try {
            //stream1.lock();
            stream1.createLoader();
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        } finally {
            client.close();
        }

        stream.delete();
    }

    @Test
    public void test2() throws InterruptedException {
        DXTickStream stream = createTestStream("lock_test2");
        DBLock lock = stream.lock();
        TickLoader loader = stream.createLoader();
        loader.close();
        lock.release();

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test2");
       
        stream1.lock();
        client.close();               
    }

    @Test
    public void test3() throws IOException {
        DXTickStream stream = createTestStream("lock_test3");

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test3");

        stream1.lock();

        client.close();

        stream.tryLock(1000);

        stream.delete();
    }

    @Test
    public void test4() {
        DXTickStream stream = createTestStream("lock_test4");
        DBLock lock = stream.lock();

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test4");
        try {
            stream1.lock(LockType.READ);
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        } finally {
            client.close();
        }

        stream.delete();
    }

    @Test
    public void test5() {
        DXTickStream stream = createTestStream("lock_test5");
        DBLock lock = stream.lock(LockType.READ);

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test5");
        stream1.lock(LockType.READ);
        client.close();

        stream.delete();
    }

    @Test
    public void test6() {
        DXTickStream stream = createTestStream("lock_test6");
        stream.lock();

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test6");
        
        TickLoader loader = null;
        try {
            loader = stream1.createLoader(new LoadingOptions());
            assertTrue("StreamLockedException is NOT thrown", false);
        }
        catch (StreamLockedException ex) {
            // passed
        }
        finally {
            Util.close(loader);
        }

        TickCursor cursor = stream1.createCursor(new SelectionOptions());
        cursor.close();

        TickCursor cursor1 = stream.createCursor(new SelectionOptions());
        cursor1.close();

        TickLoader loader1 = stream.createLoader();
        loader1.close();
        
        client.close();

        //stream.delete();
    }

    @Test
    public void test7() {
        DXTickStream stream = createTestStream("lock_test7");

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test7");

        TickLoader loader = stream1.createLoader(new LoadingOptions());

        DBLock lock = stream.lock();

        try {
            BarMessage message = new BarMessage();
            message.setSymbol("ORCL");
            for (int i = 0; i < 100000; i++)
                loader.send(message);

            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        } finally {
            Util.close(loader);
            if (lock != null)
                lock.release();

            Util.close(client);
        }

        stream.delete();
    }

    @Test
    public void test71() {
        DXTickStream stream = createTestStream("lock_test71");

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test71");

        TickLoader loader = stream1.createLoader(new LoadingOptions());        

        try {
            BarMessage message = new BarMessage();
            message.setSymbol("ORCL");
            for (int i = 0; i < 100000; i++) {
                if (i == 1000)
                    stream.lock();
                
                loader.send(message);
            }

            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        } finally {
            Util.close(loader);
            client.close();
        }

        stream.delete();
    }

    @Test
    public void test72() {
        DXTickStream stream = createTestStream("lock_test72");

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
         
        DXTickStream stream1 = client.getStream("lock_test72");
        TickLoader loader = stream1.createLoader(new LoadingOptions());

        try {
            BarMessage message = new BarMessage();
            message.setSymbol("ORCL");
            for (int i = 0; i < 5000; i++) {
                if (i == 1000)
                    stream1.lock();

                loader.send(message);
            }

        } finally {
            Util.close(loader);
        }

        client.close();

        stream.delete();
    }

    @Test
    public void test9() {
        DXTickStream stream = createTestStream("lock_test9");

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test9");

        stream.lock();

        try {
            stream1.tryLock(1000);
        } catch (StreamLockedException e) {
            // valid case
        }

        client.close();

        stream.delete();
    }

    @Test
    public void test8() {
        DXTickStream stream = createTestStream("lock_test8");
        stream.tryLock(LockType.READ, Long.MAX_VALUE);

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("lock_test8");
        stream1.tryLock(LockType.READ, Long.MAX_VALUE);

        TickLoader loader = null;
        try {
            loader = stream1.createLoader(new LoadingOptions());
            BarMessage message = new BarMessage();
            message.setSymbol("ORCL");
            for (int i = 0; i < 1000; i++)
                loader.send(message);

            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        } finally {
            Util.close(loader);
        }

        client.close();

        stream.delete();
    }

    @Test
    public void checkOperations() throws InterruptedException {
        checkLockOperations(LockType.READ);
    }

    @Test
    public void checkOperations1() throws InterruptedException {
        checkLockOperations(LockType.WRITE);
    }

    public void checkLockOperations(LockType type) throws InterruptedException {
        DXTickStream stream = createTestStream("checkOperations");
        DBLock lock = stream.lock(type);

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("checkOperations");

        try {
            stream1.rename("aa");
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        try {
            stream1.setName("aa");
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        try {
            stream1.setDescription("aa");
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        try {
            stream1.setPeriodicity(Periodicity.mkRegular(Interval.DAY));
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        try {
            stream1.setFixedType(BAR_DESCRIPTOR);
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        try {
            stream1.truncate(Long.MIN_VALUE);
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

        stream1.purge(Long.MIN_VALUE);

        BackgroundProcessInfo process;
        while ((process = stream1.getBackgroundProcess()) != null && !process.isFinished())
            Thread.sleep(500);

        try {
            StreamMetaDataChange change = new StreamMetaDataChange();
            stream1.execute(new SchemaChangeTask(change));
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }

//        try {
//            stream1.purge(Long.MIN_VALUE);
//            assertTrue("StreamLockedException is NOT thrown", false);
//        } catch (StreamLockedException e) {
//            // valid case
//        }

        try {
            stream1.clear();
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // valid case
        }
         
        client.close();
        stream.delete();
    }

    @Test
    public void testEvents() throws InterruptedException {
        DXTickStream stream = createTestStream("test_events");
        final ArrayList<InstrumentMessage> messages = new ArrayList<InstrumentMessage>();

        final TickCursor cursor = getTickDb().getStream(TickDBFactory.EVENTS_STREAM_NAME).
                select(System.currentTimeMillis(), new SelectionOptions(false, true));

        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    assertTrue(cursor.next());
                    messages.add((InstrumentMessage) cursor.getMessage().clone());
                }
            }
        });

        consumer.start();

        // wait thread to start
        Thread.sleep(1000);

        DBLock lock = null;
        try {
            lock = stream.lock(LockType.WRITE);
        } finally {
            if (lock != null)
                lock.release();
        }

        try {
            lock = stream.lock(LockType.READ);
        } finally {
            if (lock != null)
                lock.release();
        }

        consumer.join();
        cursor.close();

        assertTrue(messages.get(0) instanceof EventMessage);
        EventMessage message = (EventMessage) messages.get(0);
        assertEquals(EventMessageType.WRITE_LOCK_ACQUIRED, message.getEventType());

        assertTrue(messages.get(1) instanceof EventMessage);
        message = (EventMessage) messages.get(1);
        assertEquals(EventMessageType.WRITE_LOCK_RELEASED, message.getEventType());

        assertTrue(messages.get(2) instanceof EventMessage);
        message = (EventMessage) messages.get(2);
        assertEquals(EventMessageType.READ_LOCK_ACQUIRED, message.getEventType());

        assertTrue(messages.get(3) instanceof EventMessage);
        message = (EventMessage) messages.get(3);
        assertEquals(EventMessageType.READ_LOCK_RELEASED, message.getEventType());
    }

    @Test
    public void checkDelete() {
        DXTickStream stream = createTestStream("checkDelete");
        stream.lock(LockType.READ);

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);
        DXTickStream stream1 = client.getStream("checkDelete");

        try {
            stream1.delete();
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // correct
        }
        client.close();

        stream.delete();
    }

    @Test
    public void testMultiply() {
        DXTickStream stream = createTestStream("testMultiply");
        DBLock lock1 = stream.lock(LockType.WRITE);
        DBLock lock2 = stream.lock(LockType.WRITE);

        DXTickDB client = TickDBFactory.createFromUrl(url);
        client.open(false);

        DXTickStream stream1 = client.getStream("testMultiply");

        try {
            stream1.delete();
            assertTrue("StreamLockedException is NOT thrown", false);
        } catch (StreamLockedException e) {
            // correct
        }

        lock1.release();
        lock2.release();

        stream1.delete();

        client.close();
    }
}
