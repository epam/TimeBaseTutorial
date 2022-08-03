import sys, os, inspect, struct
import math
import time
import calendar
from datetime import datetime

import tbapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    db = tbapi.TickDb.createFromUrl(timebase)
    
    # Open in read-only mode
    db.open(True)
	
    print('Connected to ' + timebase)

    # Define name of the stream    
    streamKey = 'trade_bbo'

    # Get stream from the timebase
    stream = db.getStream(streamKey)    
    if stream == None:
        raise Exception('Stream ' + streamKey + ' not found, please, create stream')
       	
    # Create cursor with empty subscription
    cursor = db.createCursor(None, tbapi.SelectionOptions())

    # Create subscription dynamically
    # 1. Add entities
    entities = [
        'USGG5YR',
        'AAPL'
    ]
    cursor.addEntities(entities)
    
    # 2. Subscribe to the Trade and BestBidOffer Messages
    types = [
        'com.epam.deltix.timebase.messages.TradeMessage',
        'com.epam.deltix.timebase.messages.BestBidOfferMessage'
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
            
            if message.typeName == 'com.epam.deltix.timebase.messages.TradeMessage':
                print("Trade (" + str(messageTime) + "," + message.symbol + ") price: " + str(message.price))
            elif message.typeName == 'com.epam.deltix.timebase.messages.BestBidOfferMessage':
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
    
