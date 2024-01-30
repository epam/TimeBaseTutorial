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

package com.epam.deltix.samples.timebase.basics;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  This sample illustrates the process of querying security metadata while
 *  applying QQL (QuantServer Query Language) filters. To prepare for running
 *  this test, start TimeBase with the sample database (available as an
 *  installation package).
 */
public class Step5_QueryData {
    static class QueryResult extends InstrumentMessage {
        public String               name;
    }

    /**
     *  List futures names, whose root symbol equals "ES". The string literal
     *  "ES" is passed inline in the where clause of the select statement.
     *  The result is received using the Bound API, i.e. in the form of native
     *  Java objects.
     */
    public static void listFuturesNamesByRootSymbolInline (DXTickDB db) {
        System.out.println ("listFuturesNamesByRootSymbolInline:");

        SelectionOptions            options = new SelectionOptions ();

        options.typeLoader = new SimpleTypeLoader() {
            @Override
            public Class<?> load(ClassDescriptor cd) throws ClassNotFoundException {
                if (cd.getName().startsWith("QUERY"))
                    return QueryResult.class;
                return super.load(cd);
            }
        };

        InstrumentMessageSource     cursor =
            db.executeQuery (
                "select close from bars where symbol = 'ES'",
                options
            );

        try {
            while (cursor.next ()) {
                QueryResult         msg = (QueryResult) cursor.getMessage ();

                System.out.println ("    symbol: " + msg.getSymbol() + " name: " +  msg.name);
            }
        } finally {
            cursor.close ();
        }
    }

    /**
     *  List futures names, whose root symbol equals "NQ". The string literal
     *  "ES" is passed as a query parameter. This allows cleaner code,
     *  avoids the necessity to escape special characters, such as quotes, and
     *  allows the query engine to cache the statement.
     *  The result is received using the Bound API, i.e. in the form of native
     *  Java objects.
     */
    public static void listFuturesNamesByRootSymbolParam (DXTickDB db, String root) {
        System.out.println ("listFuturesNamesByRootSymbolParam (" + root + "):");

        SelectionOptions            options = new SelectionOptions ();

        options.typeLoader = new SimpleTypeLoader (null, QueryResult.class);

        Parameter                   rootSymbolParam =
            new Parameter ("symbolParam", StandardTypes.CLEAN_VARCHAR);

        rootSymbolParam.value.writeString (root);

        InstrumentMessageSource     cursor =
            db.executeQuery (
                "select close from bars where symbol = symbolParam",
                options,
                rootSymbolParam
            );

        try {
            while (cursor.next ()) {
                QueryResult         msg = (QueryResult) cursor.getMessage ();

                System.out.println ("    symbol: " + msg.getSymbol() + " name: " +  msg.name);
            }
        } finally {
            cursor.close ();
        }
    }

    public static void listFuturesNamesByRollDateParam(DXTickDB db, long rollDate) {
        System.out.println ("listFuturesNamesByRollDateParam (" + rollDate + "):");

        SelectionOptions            options = new SelectionOptions ();

        options.typeLoader = new SimpleTypeLoader (null, QueryResult.class);

        Parameter                   rollDateParam =
                new Parameter ("rollDateParam", StandardTypes.CLEAN_INTEGER);

        rollDateParam.value.writeLong (rollDate);

        InstrumentMessageSource     cursor =
                db.executeQuery (
                        "select name from securities where rollDate > rollDateParam",
                        options,
                        rollDateParam
                );

        try {
            while (cursor.next ()) {
                QueryResult         msg = (QueryResult) cursor.getMessage ();

                System.out.println ("    symbol: " + msg.getSymbol() + " name: " +  msg.name);
            }
        } finally {
            cursor.close ();
        }
    }

    /**
     *  Find a specific security object by symbol and return it in its entirety,
     *  as opposed to a subset of its fields. This is the effect of <tt>select *</tt>.
     *  The result is received using the Bound API, i.e. in the form of a
     *  native Java object.
     */
    public static void listFuturesBySymbol (DXTickDB db, String symbol) {
        System.out.println ("listFuturesBySymbol (" + symbol + "):");
        
        Parameter                   symbolParam =
            new Parameter ("symbolParam", StandardTypes.CLEAN_VARCHAR);

        symbolParam.value.writeString (symbol);

        InstrumentMessageSource     cursor =
            db.executeQuery (
                "select * from bars where symbol = symbolParam",
                symbolParam
            );

        try {
            while (cursor.next ()) {
                InstrumentMessage message = cursor.getMessage();
                System.out.println ("    " + message);
            }
        } finally {
            cursor.close ();
        }
    }

    public static void      main (String [] args) {
        if (args.length == 0)
            args = new String [] { "dxtick://localhost:8012" };

        DXTickDB    db = TickDBFactory.createFromUrl (args [0]);
        
        db.open (true);

        try {
            listFuturesNamesByRootSymbolInline (db);
            listFuturesNamesByRootSymbolParam(db, "NQ");
            listFuturesNamesByRollDateParam(db, System.currentTimeMillis());
            listFuturesBySymbol (db, "ESZ11");
        } finally {
            db.close ();
        }
    }
}
