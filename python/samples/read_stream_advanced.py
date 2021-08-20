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
    streamKey = 'ticks'

    # Get stream from the timebase
    stream = db.getStream(streamKey)    
       	
    # Create cursor with empty subscription
    cursor = db.createCursor(None, dxapi.SelectionOptions())

    # Create subscription dynamically
    # 1. Add entities
    entities = [
        dxapi.InstrumentIdentity(dxapi.InstrumentType.EQUITY, 'AAA'),
        dxapi.InstrumentIdentity(dxapi.InstrumentType.EQUITY, 'AAPL')
    ]
    cursor.addEntities(entities)
    
    # 2. Subscribe to the Trade and BestBidOffer Messages
    types = [
        'deltix.timebase.api.messages.TradeMessage',
        'deltix.timebase.api.messages.BestBidOfferMessage'
    ]
    cursor.addTypes(types)

    # 3. Subscribe to the data stream(s)
    cursor.addStreams([stream])

    # Define subscription start time
    time = datetime(2010, 1, 1, 0, 0)
    # Start time is Epoch time in milliseconds
    startTime = calendar.timegm(time.timetuple()) * 1000

    # 4. Reset cursor to the subscription time
    cursor.reset(startTime)
    
    try:
        while cursor.next():
            message = cursor.getMessage()           
            
            # Message time is Epoch time in nanoseconds
            time = message.timestamp/1e9
            messageTime = datetime.utcfromtimestamp(time)
            
            if message.typeName == 'deltix.timebase.api.messages.TradeMessage':
                print("Trade (" + str(messageTime) + "," + message.symbol + ") price: " + str(message.price))
            elif message.typeName == 'deltix.timebase.api.messages.BestBidOfferMessage':
                print("BBO (" + str(messageTime) + "," + message.symbol + ") bid price: " + str(message.bidPrice) + ", ask price: " + str(message.offerPrice))                    
        
        
    finally:
        # cursor should be closed anyway
        cursor.close()
        cursor = None
		
finally:
    # database connection should be closed anyway
    if db.isOpen():
        db.close()
        print("Connection " + timebase + " closed.")
    
