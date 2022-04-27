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
    
    # Open in read-write mode
    db.open(False)
    
    print('Connected to ' + timebase)

    # Define name of the stream    
    streamKey = 'bitfinex'
    
    # Get stream from the timebase
    stream = db.getStream(streamKey)
    
    # Create a Message Loader for the selected stream and provide loading options
    loader = stream.createLoader(tbapi.LoadingOptions())
    
    # Create PackageHeader message
    packageHeader = tbapi.InstrumentMessage()
    
    # Define message type name according to the Timebase schema type name
    # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
    packageHeader.typeName = 'com.epam.deltix.timebase.messages.universal.PackageHeader'
    packageHeader.symbol = 'BTC/USD'
    packageHeader.currencyCode = 999
    packageHeader.packageType = 'VENDOR_SNAPSHOT'
    
    print('Start loading to ' + streamKey)    
    
    count = 0
    currentValue = 1.1
    for i in range(10):    
        # get current time in UTC
        now = datetime.utcnow() - datetime(1970, 1, 1)
        
        # Define message timestamp as Epoch time in nanoseconds 
        ns = now.total_seconds() * 1e9 + now.microseconds * 1000;
        
        currentValue = currentValue + 1.1
        packageHeader.entries = []
        for j in range(5):
            entry = tbapi.InstrumentMessage()
            entry.typeName = 'com.epam.deltix.timebase.messages.universal.L2EntryNew'
            entry.exchangeId = 'GDAX'
            entry.price = currentValue
            entry.size = currentValue
            entry.level = j
            entry.side = 'BID'
            packageHeader.entries.append(entry)
        
        # Send message
        loader.send(packageHeader)
        count = count + 1
            
    print("Sent " + str(count) + " messages")
    # close Message Loader
    loader.close()
    loader = None
    
finally:
    # database connection should be closed anyway
    if db.isOpen():
        db.close()
        print("Connection " + timebase + " closed.")




