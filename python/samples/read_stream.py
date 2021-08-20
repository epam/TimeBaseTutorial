import sys, os, inspect, struct
import math
import time
import calendar
from datetime import datetime
import dxapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    db = dxapi.TickDb.createFromUrl(timebase)
    
    # Open in read-only mode
    db.open(True)
	
    print('Connected to ' + timebase)

    # Define name of the stream
    # streamKey = 'daily.stream'
    streamKey = 'bars1min'

    # Get stream from the timebase
    stream = db.getStream(streamKey)

    # List of message types to subscribe (if None, all stream types will be used)
    types = ['deltix.timebase.api.messages.BarMessage']

    # List of entities to subscribe (if None, all stream entities will be used)
    entities = [
        dxapi.InstrumentIdentity(dxapi.InstrumentType.EQUITY, 'MSFT'),
        dxapi.InstrumentIdentity(dxapi.InstrumentType.EQUITY, 'AAPL')
    ]

    # Define subscription start time
    time = datetime(2010, 1, 1, 0, 0)
	
    # Start time is Epoch time in milliseconds
    startTime = calendar.timegm(time.timetuple()) * 1000
	
    # Create cursor using defined message types and entities
    cursor = stream.select(startTime, dxapi.SelectionOptions(), types, entities)
    try:
        while cursor.next():
            message = cursor.getMessage()

            # Message time is Epoch time in nanoseconds
            time = message.timestamp/1e9
            messageTime = datetime.utcfromtimestamp(time)
            
            if message.typeName == 'deltix.timebase.api.messages.BarMessage':
                print("Bar (" + str(messageTime) + ") close price: " + str(message.close))
    finally:
        # cursor should be closed anyway
        cursor.close()
        cursor = None
		
finally:
    # database connection should be closed anyway
    if (db.isOpen()):
        db.close()
        print("Connection " + timebase + " closed.")
