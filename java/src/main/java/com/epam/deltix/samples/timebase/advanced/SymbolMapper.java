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
package com.epam.deltix.samples.timebase.advanced;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.*;
import com.epam.deltix.util.csvx.*;
import java.io.*;
import java.util.*;

/**
 *  Creates a stream whose schema is an exact copy of an existing stream,
 *  and copies all data from the existing stream into the new one, while
 *  replacing symbols, according to a mapping defined as a two-column CSV file.
 */
public class SymbolMapper {
    public static void                  mapSymbols (
        MessageSource<InstrumentMessage> in,
        Map <Object, String>                symbolMap,
        MessageChannel<InstrumentMessage> out
    )
    {
        //
        //  Allocate buffers
        //
        RawMessage                  outMsg = new RawMessage ();
        CharSequenceKey             symbolKey = new CharSequenceKey ();
        CharSequenceSet             unmatchedSymbols = new CharSequenceSet ();
        long                        numRead = 0;
        long                        numSent = 0;
        long                        numRejected = 0;
        
        while (in.next ()) {
            RawMessage              inMsg = (RawMessage) in.getMessage ();
            
            numRead++;
            
            symbolKey.charSequence = inMsg.getSymbol();
            
            String                  newSymbol = symbolMap.get (symbolKey);
            
            if (newSymbol == null) {
                if (unmatchedSymbols.addCharSequence (inMsg.getSymbol()))
                    System.out.println ("Unmatched: " + inMsg.getSymbol());
                
                numRejected++;
                continue;
            }
            
            outMsg.setSymbol(newSymbol);
            outMsg.setTimeStampMs(inMsg.getTimeStampMs());
            outMsg.setNanoTime(inMsg.getNanoTime());
           // outMsg.setInstrumentType(inMsg.getInstrumentType());
            outMsg.data = inMsg.data;
            outMsg.offset = inMsg.offset;
            outMsg.length = inMsg.length;
            outMsg.type = inMsg.type;   // Requires full schema equiavalence  
            
            out.send (outMsg);
            
            numSent++;
        }
        
        System.out.println (
            "Read: " + numRead + "; Mapped: " + 
            numSent + "; Rejected: " + numRejected
        );
    }
    
    public static Map <Object, String>  readMapFromCSV (String path) 
        throws IOException 
    {
        try (CSVXReader csv = new CSVXReader (path)) {
            Map <Object, String>    map = new HashMap <> ();
            
            while (csv.nextLine ()) {
                String              from = csv.getString (0);
                String              to = csv.getString (1);
                
                if (map.put (from, to) != null)
                    System.out.println (csv.getDiagPrefixWithLineNumber () + "Duplicate key: " + from);            
            }
            
            return (map);
        }        
    }
    
    public static void                  main (String [] args) throws Exception {
        String                      dbUrl = args [0];
        String                      fromKey = args [1];
        String                      toKey = args [2];
        String                      mapFile = args [3];
        
        Map <Object, String>        map = readMapFromCSV (mapFile);
        
        try (DXTickDB db = TickDBFactory.createFromUrl (dbUrl)) {
            db.open (false);
            
            DXTickStream                srcStream = db.getStream (fromKey);
            
            StreamOptions               so = srcStream.getStreamOptions ();
            
            so.name = toKey;
            so.description = "Copied from " + fromKey + " while mapping symbols";
            
            DXTickStream                destStream = db.createStream (toKey, so);
            
            try (
                InstrumentMessageSource src =
                    srcStream.select (
                        Long.MIN_VALUE,                     // no filtering by time
                        new SelectionOptions (true, false), // raw, not live
                        null,                               // all types
                            (CharSequence[]) null                                // all entities (symbols)
                    ); 
                
                TickLoader              dest = 
                    destStream.createLoader (new LoadingOptions (true))
            ) 
            {                
                mapSymbols (src, map, dest);
            }
        }
    }
}
