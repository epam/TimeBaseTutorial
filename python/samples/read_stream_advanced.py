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
    streamKey = 'sample_l2'

    # Get stream from the timebase
    stream = db.getStream(streamKey)   
    if stream == None:
        raise Exception('Stream ' + streamKey + ' not found, please, create stream')    
       	
    # Create cursor with empty subscription
    cursor = db.createCursor(None, tbapi.SelectionOptions())

    # Create subscription dynamically
    # 1. Add entities
    entities = ['BTC/USD', 'BTC/EUR']
    cursor.addEntities(entities)
    
    # 2. Subscribe to the Trade and BestBidOffer Messages
    types = [
        'com.epam.deltix.timebase.messages.universal.PackageHeader',
        'com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage'
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
            
            if message.typeName == 'com.epam.deltix.timebase.messages.universal.PackageHeader':
                print("================================================")
                print("PackageHeader timestamp: " + str(messageTime) + ", symbol: " + message.symbol + ", package type: " + message.packageType)
                for entry in message.entries:
                    if entry.typeName == 'com.epam.deltix.timebase.messages.universal.L2EntryNew':
                        print("NEW: " + str(entry.level) + ": " + str(entry.side) + " " + str(entry.size) + " @ " + str(entry.price) + " (" + str(entry.exchangeId) + ")")
                    elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.L2EntryUpdate':
                        print("UPDATE [" + entry.action + "]: " + str(entry.level) + ": " + str(entry.side) + " " + str(entry.size) + " @ " + str(entry.price) + " (" + str(entry.exchangeId) + ")")
                    elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.L1Entry':
                        print("L1Entry: " + str(entry.side) + " " + str(entry.size) + " @ " + str(entry.price) + " (" + str(entry.exchangeId) + ")")
                    elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.TradeEntry':
                        print("Trade: " + str(entry.side) + " " + str(entry.size) + " @ " + str(entry.price) + " (" + str(entry.exchangeId) + ")")
            elif message.typeName == 'com.epam.deltix.timebase.messages.service.SecurityFeedStatusMessage':
                print("FeedStatus (" + str(messageTime) + "): " + str(message.status))               
    finally:
        # cursor should be closed anyway
        cursor.close()
        cursor = None
		
finally:
    # database connection should be closed anyway
    if db.isOpen():
        db.close()
        print("Connection " + timebase + " closed.")
    
