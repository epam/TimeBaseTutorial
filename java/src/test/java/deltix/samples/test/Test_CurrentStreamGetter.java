/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package deltix.samples.test;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.samples.timebase.TradeMessage;
import com.epam.deltix.util.lang.Util;
import org.junit.*;
import static org.junit.Assert.*;


/**
 *  Test transient stream features.
 */
public class Test_CurrentStreamGetter extends TDBTestBase {
    private static final long       T = 1262106445000L;
    private static final int        NS = 10;
    private static final int        NM = 100;

    public final void               prepare() throws Introspector.IntrospectionException {
        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(TradeMessage.class);

        TradeMessage msg = new TradeMessage();

        msg.setSymbol("DLTX");


        //  Create NS streams
        for (int ii = 0; ii < NS; ii++) {
            long                tt = T + ii;
            StreamOptions       options =
                new StreamOptions (StreamScope.DURABLE, "Stream #" + ii, null, 1);

            options.setFixedType (rcd);

            DXTickStream        stream = getTickDb().getStream ("S" + ii);
            if (stream != null)
                stream.delete();

            stream = getTickDb().createStream ("S" + ii, options);
            TickLoader          loader = stream.createLoader ();

            //  Load NM messages starting at T + ii, one per second.
            for (int jj = 0; jj < NM; jj++) {
                msg.setTimeStampMs(tt + jj * 1000);
                msg.setSize(ii);
                msg.setPrice(jj);
                
                loader.send (msg);
            }

            loader.close ();
        }
    }

    static class CurrentStreamGetterTester {
        DXTickStream []             streams = new DXTickStream [NS];
        TickCursor                  cursor;

        public CurrentStreamGetterTester () {
        }

        private void                check (
            int                         sidx,
            int                         midx
        )
        {
            assertTrue (cursor.next ());

            TradeMessage m = (TradeMessage) cursor.getMessage ();

            assertNotNull (m);

            assertEquals (sidx, (int) m.getSize());
            assertEquals (midx, (int) m.getPrice());
            assertEquals (T + midx * 1000 + sidx, m.getTimeStampMs());

            int                         csidx = cursor.getCurrentStreamIndex ();

            assertTrue (csidx >= 0);

            TickStream                  s = streams [sidx];

            assertEquals (s.getKey (), cursor.getCurrentStreamKey ());
            assertEquals (s, cursor.getCurrentStream ());
        }

        public void                 run (DXTickDB db) throws Exception {

            SelectionOptions            options = new SelectionOptions ();
            //
            //  Without this, the remote test will fail beacuse of the client
            //  optimization (output locally stored messages).
            //
            for (int ii = 0; ii < NS; ii++)
                streams [ii] = db.getStream ("S" + ii);

            try {
                cursor = db.createCursor (options, streams [0], streams [3], streams [9]);

                cursor.subscribeToAllEntities();
                cursor.reset (T);

                check (0, 0);
                check (3, 0);
                check (9, 0);
                check (0, 1);
                check (3, 1);
                check (9, 1);

                cursor.addStream (streams [7]);

                check (0, 2);
                check (3, 2);
                check (7, 2);
                check (9, 2);

                cursor.removeStream (streams [0]);

                check (3, 3);
                check (7, 3);
                check (9, 3);

                cursor.removeAllStreams ();

                //Commented out due to BUG 6433
                assertFalse (cursor.next ());

                cursor.close ();
            } finally {
                Util.close(cursor);
            }
        }
    }

    @Test
    public void             getterTestRemote () throws Exception {
        prepare();
        new CurrentStreamGetterTester().run(getTickDb());
    }
    

}
