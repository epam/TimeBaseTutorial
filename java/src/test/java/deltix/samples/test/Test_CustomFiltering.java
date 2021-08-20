package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.io.Home;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Custom filtering test
 */
public class Test_CustomFiltering extends TDBTestBase {

    public static class MyBarMessage extends InstrumentMessage {
        public MyBarMessage() {
        }

        @Override
        public String toString() {
            return "MyMessage";
        }

        @SchemaElement(
                title = "Original Time"
        )
        @SchemaType(
                dataType = SchemaDataType.TIMESTAMP
        )
        public long                 originalTimestamp = TIMESTAMP_UNKNOWN;


        @SchemaElement(
                title = "Currency Code"
        )
        public short                currencyCode = 999;

        @SchemaElement(
                title = "Exchange Code"
        )
        @SchemaType(
                encoding = "ALPHANUMERIC(10)",
                dataType = SchemaDataType.VARCHAR
        )
        public long                 exchangeCode = ExchangeCodec.NULL;

        @SchemaElement(
                title = "Close"
        )
        public double               close;

        @RelativeTo("close")
        @SchemaElement(
                title = "Open"
        )
        public double               open;

        @RelativeTo ("close")
        @SchemaElement(
                title = "High"
        )
        public double               high;

        @RelativeTo ("close")
        @SchemaElement(
                title = "Low"
        )
        public double               low;

        @SchemaElement(
                title = "Volume"
        )
        public double               volume;
    }

    @Test
    public void testCustomFiltering() throws Introspector.IntrospectionException {
        Home.set(System.getProperty("user.dir"));

        DXTickDB tdb = getTickDb();

        deleteIfExists("filters");


        DXTickStream stream = Test_CursorSubscription.createBarsStream (tdb, "filters");
        
        SelectionOptions options = new SelectionOptions();
        options.typeLoader = new TypeLoaderImpl(MyBarMessage.class.getClassLoader()) {
            @Override
            public Class load(ClassDescriptor cd) throws ClassNotFoundException {
                if (cd.getName().contains("BarMessage"))
                    return MyBarMessage.class;
                
                return super.load(cd);
            }
        };

        try (TickCursor cursor = TickCursorFactory.create(stream, 0, options, "AAPL")) {

            for (int i = 0; i < 100; i++)
                cursor.next();

            cursor.setTypes(BarMessage.class.getName());
            cursor.addEntity(new ConstantIdentityKey("ORCL"));
            assertTrue(cursor.next());
        }
    }
}
