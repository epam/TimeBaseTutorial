package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;


public class Test_AsyncChangeFilter extends TDBTestBase {

    final static  String STREAM_KEY = "bars";

    @Test(timeout = 30_000)
    public void test() throws InterruptedException, Introspector.IntrospectionException {

        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);

        DXTickStream            stream = createStream(getTickDb(), STREAM_KEY, descriptor);

        Thread th2 = new AppleSupplier(getTickDb());
        th2.start();

        Thread th1 = new AyncClient(stream);
        th1.start();

        th1.join (/*20000*/);
        th2.join();
    }

    private volatile boolean stopped = false;

    private class AppleSupplier extends Thread {
        private WritableTickStream stream;
        private final int BAR_SIZE = 500;

        public AppleSupplier(DXTickDB db) {
            super("Apple Supplier");
            setDaemon(true);
            stream = db.getStream(STREAM_KEY);
        }

        @Override
        public void     run () {
            BarMessage msg = new BarMessage();
            msg.setSymbol("AAPL");
            //msg.barSize = BAR_SIZE;
            msg.setOpen(12.34);
            msg.setHigh(msg.getOpen());
            msg.setLow(msg.getOpen());
            msg.setClose(msg.getOpen());

            msg.setVolume(0);

            try (TickLoader loader = stream.createLoader()) {
                while (!stopped) {
                    msg.setTimeStampMs(TimeKeeper.currentTime);
                    loader.send(msg);
                    Thread.sleep(BAR_SIZE);
                    msg.setVolume(msg.getVolume() + 1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class AyncClient extends Thread {
        private final TickStream stream;

        private AyncClient(TickStream stream) {
            super("Aynchronous Client");
            this.stream = stream;
        }

        @Override
        public void run() {
            try (TickCursor cur = stream.select (TimeKeeper.currentTime, new SelectionOptions(false, true)))
            {
                for (int ii = 0; ii < 5; ii++) {
                    cur.next ();
                    System.out.println (cur.getMessage ());
                }

                System.out.println ("setFilter restricted");
                cur.clearAllEntities();

                Thread.sleep(2000);
                System.out.println("setFilter unrestricted");

                cur.subscribeToAllEntities();

                for (int ii = 0; ii < 5; ii++) {
                    cur.next ();
                    System.out.println (cur.getMessage ());
                }

                stopped = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
