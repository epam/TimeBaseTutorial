package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.ServerException;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.samples.timebase.BarMessage;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs 3 threads.
 * Two treads attempt to create a stream and then read a message from it.
 * Third thread polls TB for this stream and then tries to read message.
 * <p>
 * Time must guarantee that:
 * <ul>
 *     <li>
 *         Exactly one thread successfully creates stream.
 *     </li>
 *     <li>
 *         Thread that failed to create stream must get "Duplicate stream key" message.
 *     </li>
 *     <li>
 *         Polling thread must successfully open cursor as soon as stream created.
 *         Client may not observer partially created streams.
 *     </li>
 * </ul>
 */
@Category(Long.class)
public class Test_ConcurrentCreateThenReadWithSpaces extends TDBTestBase {

    static RecordClassDescriptor BAR_DESCRIPTOR;

    static {
        try {
            BAR_DESCRIPTOR = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);
        } catch (Introspector.IntrospectionException e) {
            e.printStackTrace();
        }
    }

    private static final String testSpace = "testSpace";

    @Test
    public void testSameSpaces() throws InterruptedException {
        DXTickDB db = getTickDb();

        for (int i = 0; i < 10; i++) {
            testIteration(db, "sp1", "sp1", "sp1", "sp1");
        }
    }

    @Test
    public void testDifferentSpaces() throws InterruptedException {
        DXTickDB db = getTickDb();

        for (int i = 0; i < 100; i++) {
            testIteration(db, "sp1", "sp2", "sp3", "sp4");
        }
    }

    private void testIteration(DXTickDB db, String sp1, String sp2, String sp3, String sp31) throws InterruptedException {
        CreateStreamTask t1 = new CreateStreamTask(db, sp1);
        CreateStreamTask t2 = new CreateStreamTask(db, sp2);
        PollStreamTask t3 = new PollStreamTask(db, sp3);
        PollStreamTask t4 = new PollStreamTask(db, sp31);

        Thread thread1 = new Thread(t1);
        Thread thread2 = new Thread(t2);
        Thread thread3 = new Thread(t3);
        Thread thread4 = new Thread(t4);

        thread3.start();
        thread4.start();

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        if (!t1.success.get() || !t2.success.get() || !t3.success.get() || !t4.success.get()) {
            throw new AssertionError("One of threads failed");
        }

        db.getStream("test").delete();
    }


    private static class CreateStreamTask implements Runnable {

        private final DXTickDB db;
        private final String space;
        private final AtomicBoolean success = new AtomicBoolean(false);

        public CreateStreamTask(DXTickDB db, String space) {
            this.db = db;
            this.space = space;
        }

        @Override
        public void run() {
            StreamOptions streamOptions =
                    StreamOptions.fixedType(StreamScope.DURABLE, "test", "test", 0, BAR_DESCRIPTOR);

            try {
                DXTickStream stream;
                try {
                    stream = db.createStream("test", streamOptions);
                } catch (ServerException | IllegalArgumentException e) {
                    if (!e.getMessage().endsWith("Duplicate stream key: test")) {
                        throw new AssertionError("Unexpected message", e);
                    }
                    stream = db.getStream("test");
                }

                LoadingOptions loadingOptions = new LoadingOptions();
                loadingOptions.space = space;
                stream.createLoader(loadingOptions);

                SelectionOptions options = new SelectionOptions();
                TickCursor cursor = stream.createCursor(options);
                cursor.reset(Long.MIN_VALUE);
                cursor.next();
                cursor.close();

                success.set(true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }


    private static class PollStreamTask implements Runnable {

        private final DXTickDB db;
        private final AtomicBoolean success = new AtomicBoolean(false);
        private final String space;

        public PollStreamTask(DXTickDB db, String space) {
            this.db = db;
            this.space = space;
        }

        @Override
        public void run() {
            try {
                DXTickStream stream = null;
                while (stream == null) {
                    stream = db.getStream("test");
                }

                LoadingOptions loadingOptions = new LoadingOptions();
                loadingOptions.space = space;
                stream.createLoader(loadingOptions);

                SelectionOptions options = new SelectionOptions();
                TickCursor cursor = stream.createCursor(options);
                cursor.reset(Long.MIN_VALUE);
                cursor.next();
                cursor.close();

                success.set(true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}