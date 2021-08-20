package com.epam.deltix.samples.timebase.basics;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.time.Periodicity;

/*
   Third step - how to read stream Meta-Information
 */
public class Step3_StreamInformation {

    public static void main(String[] args) {

        String connection = "dxtick://localhost:8011";

        // 1. Create Timebase connection using connection string
        try (DXTickDB db = TickDBFactory.createFromUrl(connection)) {

            // It require to open database connection.
            db.open(false);

            // 2. List timebase streams
            for (DXTickStream stream : db.listStreams()) {
                System.out.printf(
                        "STREAM  key: %s; name: %s; description: %s\n",
                        stream.getKey(),
                        stream.getName(),
                        stream.getDescription()
                );

                StreamOptions streamOptions = stream.getStreamOptions();

                // 3. Print stream options
                Periodicity periodicity = streamOptions.periodicity;

                System.out.print("    Periodicity: ");

                if (periodicity.getType() != Periodicity.Type.REGULAR)
                    System.out.println(periodicity.toString());
                else
                    System.out.println(periodicity.getInterval().getNumUnits() + " " + periodicity.getInterval().getUnit());

                // 4. Get stream instruments and according time ranges

                long[] range = stream.getTimeRange();

                if (range != null)
                    System.out.printf("    TIME RANGE: %tF .. %tF\n", range[0], range[1]);

                for (IdentityKey id : stream.listEntities()) {
                    System.out.printf(
                            "    ENTITY symbol: %s\n",
                            id.getSymbol().toString()
                    );
                }
            }
        }
    }
}
