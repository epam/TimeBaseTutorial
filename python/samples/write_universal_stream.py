import sys, os, inspect, struct
import math
import time
import calendar
from datetime import datetime

import tbapi

def main():
    # Timebase URL specification, pattern is "dxtick://<host>:<port>"
    timebase = 'dxtick://localhost:8011'

    try:
        # Create timebase connection
        db = tbapi.TickDb.createFromUrl(timebase)
        
        # Open in read-write mode
        db.open(False)
        
        print('Connected to ' + timebase)

        # Define name of the stream    
        streamKey = 'universal'
        
        # Get stream from the timebase
        stream = db.getStream(streamKey)
        
        # if stream not found, try to create new
        if stream == None:
            print('Stream ' + streamKey + ' not exitsts, creating new one...')
            stream = createStreamQql(db, streamKey)
        
        # Create a Message Loader for the selected stream and provide loading options
        loader = stream.createLoader(tbapi.LoadingOptions())
        
        # Create PackageHeader message
        packageHeader = tbapi.InstrumentMessage()
        
        # Define message type name according to the Timebase schema type name
        # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
        packageHeader.typeName = 'com.epam.deltix.timebase.messages.universal.PackageHeader'
        packageHeader.symbol = 'BTCUSD'
        packageHeader.currencyCode = 999
        packageHeader.packageType = 'VENDOR_SNAPSHOT'
        
        print('Start loading to ' + streamKey)    
        
        count = 0
        currentValue = 100.1
        for i in range(10):
            currentValue = currentValue + 1.1
            packageHeader.entries = []
            for j in range(5):
                entry = tbapi.InstrumentMessage()
                entry.typeName = 'com.epam.deltix.timebase.messages.universal.L2EntryNew'
                entry.exchangeId = 'GDAX'
                entry.price = currentValue - j * 1.1
                entry.size = currentValue - j * 0.1
                entry.level = j
                entry.side = 'BID'
                packageHeader.entries.append(entry)
            
            for j in range(5):
                entry = tbapi.InstrumentMessage()
                entry.typeName = 'com.epam.deltix.timebase.messages.universal.L2EntryNew'
                entry.exchangeId = 'GDAX'
                entry.price = currentValue + j * 1.1
                entry.size = currentValue + j * 0.1
                entry.level = j
                entry.side = 'ASK'
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

# create stream function
def createStreamQql(db, streamKey):
    # read QQL from file
    with open('qql/' + streamKey + '.qql', 'r') as qqlFile:
        qql = qqlFile.read()

    # execute QQL and check result
    with db.tryExecuteQuery(qql) as cursor:
        if (cursor.next()):
            print('Query result: ' + cursor.getMessage().messageText)
    
    # request newly created stream
    stream = db.getStream(streamKey)
    if stream == None:
        raise Exception('Failed to create stream')
    else:
        print("Stream " + streamKey + " created")
        
    return stream

if __name__ == '__main__':
    main()
