package com.epam.deltix.samples.timebase.advanced;

import com.epam.deltix.qsrv.hf.blocks.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.samples.timebase.BarMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.TimeKeeper;


public class BacktestWithIndicator {
    public static final long    REPORT_INTERVAL = 1000;
    public static final int     QUEUE_CAPACITY = 1000;
    
    static class MyIndicator {
        private final double []     queue = new double [QUEUE_CAPACITY];
        private int                 size = 0;
        private int                 head = 0;
        private int                 tail = 0;
        private double              sum = 0;
        
        public void         add (BarMessage bar) {
            double              value = bar.getClose();
            //
            //  Pop the head, if the queue is full
            //
            if (size == QUEUE_CAPACITY) {
                double  old = queue [head];
        
                size--;
                head++;

                if (head == QUEUE_CAPACITY)
                    head = 0;
                //
                //  Update the sum
                //
                sum -= old;                
            }
            //
            //  Push the new value on the queue
            //        
            queue [tail] = value;
        
            size++;
            tail++;

            if (tail == QUEUE_CAPACITY)
                tail = 0;             
            //
            //  Update the sum
            //
            sum += value;
        }
        
        public double       getMean () {
            return (sum / size);
        }
    }

    public static void      readData (DXTickDB db, String streamKey) {
        DXTickStream                            stream = 
            db.getStream (streamKey);
        
        //
        //  Create a tabel of indicators, one per instrument
        //
        InstrumentToObjectMap <MyIndicator>     indicators =
            new InstrumentToObjectMap <MyIndicator> ();
        
        //
        //  InstrumentMessageSource is similar to a JDBC ResultSet
        //
        InstrumentMessageSource cursor = stream.createCursor (null);
        
        cursor.reset (Long.MIN_VALUE);          // read all data
        cursor.subscribeToAllEntities ();

        //
        //  Measure read performance
        //
        long                    startTime = TimeKeeper.currentTime;
        long                    count = 0;        
        
        try {
            while (cursor.next ()) {
                InstrumentMessage msg = cursor.getMessage ();

                if (msg instanceof BarMessage) {
                    BarMessage bar = (BarMessage) msg;

                    MyIndicator     indicator = indicators.get (bar);
                    
                    if (indicator == null) {
                        // System.out.println ("Creating a new indicator for " + bar.symbol + " ...");
                        
                        indicator = new MyIndicator ();
                        
                        indicators.put (bar, indicator);
                    }
                    
                    indicator.add (bar);                        
                }
                else
                    throw new RuntimeException ("Unexpected message type");
                
                count++;
                
                long            now = TimeKeeper.currentTime;
                long            elapsed = now - startTime;
                
                if (elapsed >= REPORT_INTERVAL) {
                    System.out.printf ("%,9.0f messages/s\r", count * 1000.0 / elapsed);
                    startTime = now;
                    count = 0;
                }
            }
        } finally {
            cursor.close ();
        }

        System.out.println ("\nDone.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "dxtick://localhost", "Intraday" };

        DXTickDB    db = TickDBFactory.createFromUrl (args [0]);

        db.open (true);

        try {
            readData (db, args [1]);
        } finally {
            db.close ();
        }
    }
}
