package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.io.Home;
import org.junit.Before;
import org.junit.Test;

public class Test_QQL extends TDBTestBase {


    @Before
    public void start() throws Introspector.IntrospectionException {
        Home.set(System.getProperty("user.dir"));

        DXTickStream stream = getTickDb().getStream("bars");
        if (stream != null)
            stream.delete();
    }

    @Test
    public void Test_Query() throws Introspector.IntrospectionException {
        DXTickDB db = getTickDb();

        DXTickStream bars = Test_CursorSubscription.createBarsStream(db, "bars");

        try (InstrumentMessageSource query = db.executeQuery("SELECT * from bars where symbol == $symbol",
                new SelectionOptions(),
                Parameter.VARCHAR("$symbol", "IBM")) ) {

            if (query.next()) {
                System.out.println(query.getMessage());
            }
        }

        try (InstrumentMessageSource query = db.executeQuery("SELECT * from bars where symbol in ($name1, $name2)",
                new SelectionOptions(),
                Parameter.VARCHAR("$name1", "ORCL"),
                Parameter.VARCHAR("$name2", "GOOG")) ) {

            if (query.next())
                System.out.println(query.getMessage());

            if (query.next())
                System.out.println(query.getMessage());

        }

    }
}
