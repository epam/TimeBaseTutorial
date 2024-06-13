package com.epam.deltix.samples.timebase.performance;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.stream.MessageReader2;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class Runner extends DefaultApplication {

    public static int TOTAL_MESSAGES_PER_SPACE = 40_000_000;

    protected Runner(String[] args) {
        super(args);
    }

    public static void populateStream(DXTickDB db, String name, File file, int spaces) throws IOException {

        DXTickStream stream = createStream(db, name, file);

        MessageReader2 reader = MessageReader2.createRaw(file);

        ArrayList<RawMessage> messages = new ArrayList<>();

        while (reader.next()) {
            RawMessage message = (RawMessage)reader.getMessage();

            RawMessage clone = new RawMessage(message.type);
            clone.setTimeStampMs(message.getTimeStampMs());
            clone.setSymbol(message.getSymbol());
            clone.setBytes(message.data.clone(), message.offset, message.length);

            messages.add(clone);
        }

        Random rnd = new Random(2024);

        for (int i = 0; i < spaces; i++) {

            String space = i + "-S";

            LoadingOptions options = new LoadingOptions(true);
            options.space = space;

            System.out.println("Writing space: " + space);
            long t0 = System.currentTimeMillis();

            try (TickLoader loader = stream.createLoader(options)) {

                int count = 0;
                long startTime = messages.get(0).getTimeStampMs();

                while (count < TOTAL_MESSAGES_PER_SPACE) {

                    for (int j = 0, messagesSize = messages.size(); j < messagesSize; j++) {
                        RawMessage msg = messages.get(j);

                        msg.setTimeStampMs(startTime + count);
                        msg.setSymbol(space + ":ABS-" + (rnd.nextInt(100) % messages.size()));
                        loader.send(msg);
                        count++;
                    }
                }
            }

            long                            t1 = System.currentTimeMillis ();
            double                          s = (t1 - t0) * 0.001;
            System.out.printf (
                    "Write %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                    TOTAL_MESSAGES_PER_SPACE,
                    s,
                    TOTAL_MESSAGES_PER_SPACE / s
            );
        }
    }

    private static DXTickStream createStream(DXTickDB db, String streamKey, File file) throws IOException {
        DXTickStream stream = db.getStream(streamKey);

        if (stream != null)
            stream.delete();

        MessageReader2 reader = MessageReader2.createRaw(file);
        StreamOptions options = new StreamOptions();
        if (reader.getTypes().length > 1)
            options.setPolymorphic(reader.getTypes());
        else
            options.setFixedType(reader.getTypes()[0]);

        return db.createStream(streamKey, options);
    }

    @Override
    public void                 printUsage (OutputStream os) {
        PrintStream out = os instanceof PrintStream ? (PrintStream)os : new PrintStream(os);

        out.println("Usage: ");
        out.println("      -timebase dxtick://localhost:8011 -dataFile order_book_depth-ce.qsmsg.gz -stream order_book_depth -spaces 10 -qqlFile qql.txt ");
        out.println("Parameters Definition: ");
        out.println("      -timebase <value>  - timebase connection url");
        out.println("      -dataFile <path>   - path to qsmsg file contains sample data. if file is not provided, then only tests will be run");
        out.println("      -stream <name>     - stream to create. default is 'order-book-test'");
        out.println("      -spaces <value>    - number of spaces in the stream to create. default = 10");
        out.println("      -qqlFile <path>    - file contains qql queries to test performance");
    }

    public static void main(String[] args){


//        try (DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8013")) {
//            db.open(false);
//            File file = new File("E:\\Projects\\Clients\\DB\\order_book_depth-ce.qsmsg.gz");
//
//            populateStream(db, "order-book", file, 20);
//        }

        new Runner(args).start();
    }

    @Override
    protected void run() throws Throwable {
        String timebaseUrl = getArgValue("-timebase", "dxtick://localhost:8013");
        String dataFile = getArgValue("-dataFile", "order_book_depth-ce.qsmsg.gz");

        String stream = getArgValue("-stream", "order-book-test");
        int spaces = getIntArgValue("-spaces", 10);

        String qFile = getArgValue("-qqlFile", "qql.txt");

        try (DXTickDB db = TickDBFactory.createFromUrl(timebaseUrl)) {
            db.open(false);

            File dfile = new File(dataFile);
            if (dfile.exists()) {
                System.out.println("1. Writing sample data into TimeBase using " + spaces + " spaces having " + TOTAL_MESSAGES_PER_SPACE + " messages per space.");
                populateStream(db, stream, dfile, spaces);
            }

            System.out.println("2. Test queries performance: ");
            File queryfile = new File(qFile);

            String[] queries;

            if (!queryfile.exists()) {
                System.out.println("No input query file provided. Running simple queries ...");
                queries = new String[] {
                        "select * from \"" + stream + "\"",
                        "select count{}() from \"" + stream + "\""
                };
            } else {
                queries = IOUtil.readLinesFromTextFile(qFile);
            }

            for (String query : queries) {

                long t0 = System.currentTimeMillis();
                long t1 = Long.MIN_VALUE;

                System.out.println("Running query: " + query);
                InstrumentMessageSource source = db.executeQuery(query, new SelectionOptions(true, false));

                int count = 0;
                if (source.next()) {
                    t1 = System.currentTimeMillis();
                    count++;
                    System.out.printf("Query time to first message: %,.3fs \n", (t1 - t0) * 0.001);
                } else {
                    t1 = System.currentTimeMillis();
                    System.out.printf("No messages returned in %,.3fs \n", (t1 - t0) * 0.001);
                }

                while (source.next()) {
                    count++;
                }

                long t2 = System.currentTimeMillis ();

                double                          s = (t2 - t0) * 0.001;
                System.out.printf (
                        "Reading %,d messages in %,.3fs; speed: %,.0f msg/s\n", count, s, count / s);

            }


        }


    }
}
