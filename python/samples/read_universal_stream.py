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
    streamKey = 'universal'

    # Get stream from the timebase
    stream = db.getStream(streamKey)
    if stream == None:
        raise Exception('Stream ' + streamKey + ' not found, please, create stream')

    options = tbapi.SelectionOptions()

    # Create cursor using defined message types and entities
    cursor = stream.select(0, options, None, None)
    try:
        #while cursor.next():
        for i in range(0, 20):
            if (cursor.next()):
                message = cursor.getMessage()
                # Message time is Epoch time in nanoseconds
                messageTime = datetime.utcfromtimestamp(message.timestamp/1e9)
                if message.typeName == 'com.epam.deltix.timebase.messages.universal.PackageHeader':
                    print("================================================")
                    print("PackageHeader timestamp: " + str(messageTime) + ", symbol: " + message.symbol + ", package type: " + message.packageType)
                    for entry in message.entries:
                        if entry.typeName == 'com.epam.deltix.timebase.messages.universal.L2EntryNew':
                            print("NEW: " + str(entry.level) + ": " + entry.side + " " + str(entry.size) + " @ " + str(entry.price) + " (" + entry.exchangeId + ")")
                        elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.L2EntryUpdate':
                            print("UPDATE [" + entry.action + "]: " + str(entry.level) + ": " + entry.side + " " + str(entry.size) + " @ " + str(entry.price) + " (" + entry.exchangeId + ")")
                        elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.L1Entry':
                            print("L1Entry: " + entry.side + " " + str(entry.size) + " @ " + str(entry.price) + " (" + entry.exchangeId + ")")
                        elif entry.typeName == 'com.epam.deltix.timebase.messages.universal.TradeEntry':
                            print("Trade: " + entry.side + " " + str(entry.size) + " @ " + str(entry.price) + " (" + entry.exchangeId + ")")
            
    finally:
        # cursor should be closed anyway
        cursor.close()
        cursor = None
        
finally:
    # database connection should be closed anyway
    if (db.isOpen()):
        db.close()
        print("Connection " + timebase + " closed.")
