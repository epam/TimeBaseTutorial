package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.junit.After;
import org.junit.Before;


public abstract class TDBTestBase {

    protected final String               user;
    protected final String               pass;
    protected final String               url;

    private DXTickDB      db;

    public TDBTestBase(String url, String user, String pass) {
        this.user = user;
        this.pass = pass;
        this.url = url;
    }

    public TDBTestBase(String url) {
        this(url, null, null);
    }

    public TDBTestBase() {
        this("dxtick://localhost:8011");
    }

    @Before
    public void startup() throws Exception {
        db = createClient();
    }

    @After
    public void shutdown() throws Exception {
        if (db != null)
            db.close();
    }

    public DXTickDB getTickDb() {
        return db;
    }


    /*
        Create new stream and delete previous with given key
    */
    public DXTickStream createStream(String key, StreamOptions options) {
        return createStream(getTickDb(), key, options);
    }

    public static DXTickStream createStream(DXTickDB db, String key, StreamOptions options) {
        deleteIfExists(db, key);

        return db.createStream(key, options);
    }

    public static DXTickStream createStream(DXTickDB db, String key, RecordClassDescriptor rcd) {
        deleteIfExists(db, key);

        return db.createStream(key, StreamOptions.fixedType(StreamScope.DURABLE, key, key, 0, rcd));
    }

    public  void         deleteIfExists(String key) {
        deleteIfExists(getTickDb(), key);
    }

    /*
        Deletes stream if exists
     */
    public static void         deleteIfExists(DXTickDB db, String key) {
        DXTickStream stream = db.getStream(key);
        if (stream != null)
            stream.delete();
    }

    public DXTickDB             createClient() {
        DXTickDB result = TickDBFactory.createFromUrl(url, user, pass);
        TickDBFactory.setApplicationName(result, "Test Runner");
        result.open(false);
        return result;
    }
}
