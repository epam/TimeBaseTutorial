import sys, os, inspect, struct
import math
import time
import calendar
from datetime import datetime
import threading 

import tbapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

def readCursor(threadName, streamKey, startTime):
    try:
        # Create timebase connection
        db = tbapi.TickDb.createFromUrl(timebase)
        
        # Open in read-only mode
        db.open(False)
        
        print('Connected to ' + timebase)

        # Get stream from the timebase
        stream = db.getStream(streamKey)
        if stream == None:
            raise Exception('Stream ' + streamKey + ' not found, please, create stream')
        
        options = tbapi.SelectionOptions()
        options.live = True
        # Create cursor using defined message types and entities
        cursor = stream.select(startTime, options, None, None)
        ii = 0
        try:
            while True:
                state = cursor.nextIfAvailable()
                if state == tbapi.OK:
                    message = cursor.getMessage()
                    ii += 1
                    if ii % 5 == 0:
                        print("Read " + str(ii) + " messages from " + streamKey)
                        print(str(message))
                elif state == tbapi.END_OF_CURSOR:
                    print("END OF CURSOR")
                    break
        finally:
            # cursor should be closed anyway
            cursor.close()
            cursor = None
    finally:
        # database connection should be closed anyway
        if (db.isOpen()):
            db.close()
            print("Connection " + timebase + " closed.")
        
        
t1 = threading.Thread(target = readCursor, args = ("Thread-1", "trade_bbo", 0, ) )
t2 = threading.Thread(target = readCursor, args = ("Thread-2", "universal", 0, ) )

t1.start()
t2.start()

t1.join()
t2.join()
