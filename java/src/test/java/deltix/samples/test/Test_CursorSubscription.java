package deltix.samples.test;

import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.samples.timebase.BarsGenerator;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.csvx.CSVXReader;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;
import org.junit.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class Test_CursorSubscription extends TDBTestBase {

    @Before
    public void start() throws Introspector.IntrospectionException {
        Home.set(System.getProperty("user.dir"));

        DXTickStream stream = getTickDb().getStream("bars");
        if (stream != null)
            stream.delete();

        stream = createBarsStream(getTickDb(), "bars");
        stream.clear(new InstrumentKey("ORCL"),
                new InstrumentKey("AAPL"));

        stream = getTickDb().getStream("bars1");
        if (stream != null)
            stream.delete();

        DXTickStream stream2 = createBarsStream(getTickDb(), "bars1");
        stream2.clear(new InstrumentKey("IBM"),
                new InstrumentKey("GOOG"));
    }

    public static DXTickStream        createBarsStream (DXTickDB tdb, String name) throws Introspector.IntrospectionException {
        DXTickStream stream = tdb.createStream(name, name, name, 0);
        stream.setFixedType ((RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class));
        stream.setPeriodicity(Periodicity.mkRegular(Interval.MINUTE));

        LoadingOptions options = new LoadingOptions();
        //options.writeMode = LoadingOptions.WriteMode.INSERT;

        try (TickLoader      loader = stream.createLoader (options)) {
            loadBarsFromZipResource("src/test/java/deltix/samples/test/TestBars.zip", loader);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException (iox);
        }

        if (!Util.QUIET)
            System.out.println("Done.");

        return stream;
    }

    public static void          loadBarsFromZipResource (String path, TickLoader loader)
            throws IOException
    {
        if (!Util.QUIET)
            System.out.println ("Loading " + path + " ...");

        ZipInputStream zis =
                new ZipInputStream (new FileInputStream(path));

        try {
            loadBarsFromZip (zis, loader);
        } finally {
            Util.close (zis);
        }
    }

    public static void          loadBarsFromZip (ZipInputStream zis, TickLoader loader)
            throws IOException
    {
        for (;;) {
            ZipEntry zentry = zis.getNextEntry ();

            if (zentry == null)
                break;

            String          name = zentry.getName ();
            int             dot = name.indexOf ('.');

            if (dot > 0)
                name = name.substring (0, dot);

            if (!Util.QUIET)
                System.out.println ("    " + name + " ...");
            loadBars (name, zis, loader);
        }
    }

    public static void          loadBars (
            String                      symbol,
            InputStream is,
            TickLoader                  loader
    )
            throws IOException
    {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        CSVXReader in =
                new CSVXReader (new InputStreamReader(is), ',', true, symbol);

        StringBuilder               sb = new StringBuilder ();
        BarMessage bar = new BarMessage();

        bar.setSymbol(symbol);

        int                         row = 0;

        while (in.nextLine ()) {
            sb.setLength (0);
            sb.append (in.getCell (0));
            sb.append (' ');
            sb.append (in.getCell (1));

            try {
                bar.setTimeStampMs(df.parse (sb.toString ()).getTime ());
            } catch (ParseException px) {
                throw new IOException (in.getDiagPrefixWithLineNumber (), px);
            }

            bar.setOpen(in.getDouble (2));
            bar.setHigh(in.getDouble (3));
            bar.setLow(in.getDouble (4));
            bar.setClose(in.getDouble (5));
            bar.setVolume(in.getDouble (6));

            loader.send (bar);
            row++;
        }
    }

    @Test
    public void Test_Subscribe() {
        RunTest_Subscribe(getTickDb());
    }

    @Test
    public void Test_Unsubscribe() {
        RunTest_Unsubscribe(getTickDb());
    }

    @Test
    public void Test_UnsubscribeAll() {
        RunTest_UnsubscribeAll(getTickDb());
    }

    @Test
    public void Test_SubscribeRemote() {
        for (int i = 0; i < 10; i++)
            RunTest_Subscribe(getTickDb());
    }

    @Test
    public void Test_UnsubscribeRemote() {
        for (int i = 0; i < 10; i++)
            RunTest_Unsubscribe(getTickDb());
    }

    @Test
    public void Test_UnsubscribeALLRemote() {
        RunTest_UnsubscribeAll(getTickDb());
    }

    private SelectionOptions createOptions () {
        SelectionOptions    o = new SelectionOptions ();
        o.raw = true;
        return (o);
    }

    @Test
    public void Test_SubscribeRemote1() {
        for (int i = 0; i < 10; i++)
            RunTest_Subscribe1(getTickDb());
    }

    @Test
    public void Test_ResetWithInstruments() {
        RunTest_ResetWithInstruments(getTickDb());
    }

    public void RunTest_Subscribe1(DXTickDB db) {

        DXTickStream stream = db.getStream("bars");
        long[] range = stream.getTimeRange();

        String[] keys = new String[] {
                "IBM", "AAPL"
        };

        TickCursor cursor = null;
        try {
            cursor = stream.select(0, createOptions (), null, keys);

            for (int i = 0; i < 100; i++)
                cursor.next();

            cursor.removeStream(stream);
            cursor.addStream(stream);

            assertTrue(cursor.next());

            RawMessage message = (RawMessage) cursor.getMessage();

            while (cursor.next()) {
                message = (RawMessage) cursor.getMessage();
            }

            assertEquals(range[1], message.getTimeStampMs());

            cursor.close();
            cursor = null;
        } finally {
            Util.close(cursor);
        }
    }

    public void RunTest_Subscribe(DXTickDB db) {

        DXTickStream stream1 = db.getStream("bars");
        DXTickStream stream2 = db.getStream("bars1");

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("IBM"),
                new ConstantIdentityKey("AAPL")
        };

        TickCursor cursor = null;
        try {
            cursor = db.select(0, createOptions (), null, keys, stream1);

            for (int i = 0; i < 100; i++) {
                cursor.next();
            }

            cursor.addStream(stream2, stream2);
            cursor.addStream(stream2);
            assertTrue(cursor.next());
            String symbol = cursor.getMessage().getSymbol().toString();
            assertTrue(symbol.equals("AAPL") || symbol.equals("IBM"));

            assertTrue(cursor.next());

            if (symbol.equals("AAPL")) {
                symbol = cursor.getMessage().getSymbol().toString();
                assertEquals("IBM", symbol);
                assertEquals("bars", cursor.getCurrentStreamKey());
            } else {
                symbol = cursor.getMessage().getSymbol().toString();
                assertEquals("AAPL", symbol);
                assertEquals("bars1", cursor.getCurrentStreamKey());
            }

            while (cursor.next()) {
                RawMessage message = (RawMessage) cursor.getMessage();
                assertEquals(message.type, cursor.getCurrentType());
            }

            cursor.close();
            cursor = null;
        } finally {
            Util.close(cursor);
        }
    }

    @Test
    public void testEarlier() {
        runTestEarlier(getTickDb());

        runEOF(getTickDb());
    }

    @Test
    public void testEarlier1() throws Introspector.IntrospectionException {
        runTestEarlier1(getTickDb());
    }

    public void runTestEarlier1(DXTickDB db) throws Introspector.IntrospectionException {

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("GOOG")
        };
        RecordClassDescriptor descriptor = (RecordClassDescriptor) Introspector.introspectSingleClass(BarMessage.class);
        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "large", null, 0);
        options.setFixedType(descriptor);

        DXTickStream stream = createStream(options.name, options);

        GregorianCalendar calendar = new GregorianCalendar(2016, 1, 1);
        long start = calendar.getTimeInMillis();

        int total = 5000000;

        BarsGenerator gn =
                new BarsGenerator(calendar, (int) BarMessage.BAR_SECOND, total, "ORCL", "GOOG");

        try (TickLoader loader = stream.createLoader()) {
            while (gn.next())
                loader.send(gn.getMessage());
        }

        int count = 0;

        TickCursor cursor = null;
        try {

            cursor = db.select(start + total * BarMessage.BAR_SECOND / 4, createOptions (), null, keys, stream);

            for (int i = 0; i < 100; i++) {
                if (cursor.next())
                    count++;
            }

            cursor.setTimeForNewSubscriptions(start);
            cursor.addEntity(new ConstantIdentityKey("ORCL"));

            while (cursor.next()) {
                count++;
            }

        } finally {
            Util.close(cursor);
        }

        //stream.delete();

        Assert.assertEquals(total * 3/4, count);
    }

    public void runTestEarlier(DXTickDB db) {

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("GOOG")
        };

        DXTickStream bars = db.getStream("bars");
        long[] range = bars.getTimeRange();

        int count = 0;

        TickCursor cursor = null;
        try {

            cursor = db.select((range[0] + range[1]) / 2, createOptions (), null, keys, bars);

            for (int i = 0; i < 100; i++) {
                if (cursor.next())
                    count++;
            }

            cursor.setTimeForNewSubscriptions(range[0]);
            cursor.addEntity(new ConstantIdentityKey("IBM"));

            while (cursor.next()) {
                count++;
            }

        } finally {
            Util.close(cursor);
        }

        Assert.assertEquals(35349, count);
    }

    public void runEOF(DXTickDB db) {

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("GOOG")
        };

        DXTickStream bars = db.getStream("bars");
        long[] range = bars.getTimeRange();

        int count = 0;

        TickCursor cursor = null;
        try {

            cursor = db.select((range[0] + range[1]) / 2, createOptions (), null, keys, bars);

            while (cursor.next()) {
                count++;
            }

            cursor.setTimeForNewSubscriptions(range[0]);
            cursor.addEntity(new ConstantIdentityKey("IBM"));

            while (cursor.next()) {
                count++;
            }

        } finally {
            Util.close(cursor);
        }

        Assert.assertEquals(35349, count);
    }

    public void RunTest_Unsubscribe(DXTickDB db) {

        DXTickStream stream1 = db.getStream("bars");
//        if (stream1 == null) {
//            stream1 = TickDBCreator.createBarsStream(db);
//            stream1.clear(new InstrumentKey(InstrumentType.EQUITY, "ORCL"),
//                    new InstrumentKey(InstrumentType.EQUITY, "AAPL"));
//        }

        DXTickStream stream2 = db.getStream("bars1");
//        if (stream2 == null) {
//            stream2 = TickDBCreator.createBarsStream(db, "bars1");
//            stream2.clear(new InstrumentKey(InstrumentType.EQUITY, "IBM"),
//                    new InstrumentKey(InstrumentType.EQUITY, "GOOG"));
//        }

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey( "IBM"),
                new ConstantIdentityKey("AAPL")
        };

        TickCursor cursor = null;
        try {
            cursor = db.select(0, createOptions (), null, keys, stream1, stream2);
            for (int i = 0; i < 100; i++)
                cursor.next();

            cursor.removeStream(stream2);
            assertTrue(cursor.next());
            while (cursor.next()) {
                String symbol = cursor.getMessage().getSymbol().toString();
                assertTrue(symbol.equals("IBM"));
                assertEquals("bars", cursor.getCurrentStreamKey());
            }

        } finally {
            Util.close(cursor);
        }
    }

    public void RunTest_UnsubscribeAll(DXTickDB db) {

        DXTickStream stream1 = db.getStream("bars");
        DXTickStream stream2 = db.getStream("bars1");

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("IBM"),
                new ConstantIdentityKey("AAPL")
        };

        TickCursor cursor = null;
        try {
            cursor = db.select(0, new SelectionOptions(), null, keys, stream1, stream2);
            for (int i = 0; i < 100; i++)
                cursor.next();

            cursor.removeStream(stream2);
            cursor.removeStream(stream1);
            assertTrue(!cursor.next());
        } finally {
            Util.close(cursor);
        }
    }

    @Test
    public void         testResubscribe() {
        testResubscribe(getTickDb());
    }

    public void         testResubscribe(DXTickDB db) {
        DXTickStream stream = db.getStream("bars");

        SelectionOptions options = new SelectionOptions();

        try (TickCursor cursor = stream.select(Long.MIN_VALUE, options, null, (CharSequence[]) null) ) {

            long time = Long.MIN_VALUE;
            int count = 0;

            while (cursor.next()) {
                InstrumentMessage message = cursor.getMessage();

                if (time == Long.MIN_VALUE)
                    time = message.getTimeStampMs();

                long timestamp = message.getTimeStampMs();

                if (count++ == 1) {
                    cursor.addEntity(new ConstantIdentityKey( "GOOG"));
                    assertTrue("gap detected: " + (timestamp - time), timestamp == time || timestamp - time == 1000); // 1 sec
                }

                time = timestamp;

//                if (count < 3 || count > 23660)
//                    System.out.println(message);
            }

            assertEquals(23662, count);
        }

    }

    public void RunTest_ResetWithInstruments(DXTickDB db) {
        DXTickStream stream = db.getStream("bars");

        IdentityKey[] instruments = new IdentityKey[] {
                new InstrumentKey("IBM"),
                new InstrumentKey( "GOOG")
        };

        MessageSourceMultiplexer<InstrumentMessage> msm = new
                MessageSourceMultiplexer<InstrumentMessage>(true, false);

        SelectionOptions options = new SelectionOptions();

        try (TickCursor cursor = stream.createCursor(options)) {
            cursor.reset(Long.MIN_VALUE);
            cursor.addEntities(instruments, 0, instruments.length);

            msm.add(cursor);

            InstrumentMessage message = null;
            int count = 0;
            while (msm.next()) {
                InstrumentMessage newMessage = msm.getMessage().clone();

                if (message != null) {
                    if (message.getSymbol().equals(newMessage.getSymbol()) &&
                            message.getNanoTime() == newMessage.getNanoTime())
                        assert false;
                }

                message = newMessage;

                //System.out.println("MSG " + count + ": " + message);

                if (++count == 100) {
                    msm.remove(cursor);
                    cursor.reset(Long.MIN_VALUE);
                    msm.add(cursor);
                    msm.remove(cursor);
                    cursor.reset(Long.MIN_VALUE);
                    msm.add(cursor);
                }

                if (count > 150)
                    break;
            }

            System.out.println("Count = " + count);
        }

    }

}
