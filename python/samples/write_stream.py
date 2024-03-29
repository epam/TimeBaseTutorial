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
        streamKey = 'bars'
        
        # Get stream from the timebase
        stream = db.getStream(streamKey)
        
        # if stream not found, try to create new
        if stream == None:
            print('Stream ' + streamKey + ' not exitsts, creating new one...')
            stream = createStreamQql(db, streamKey)
        
        # Create a Message Loader for the selected stream and provide loading options
        loader = stream.createLoader(tbapi.LoadingOptions())
        
        # Create BestBidOffer message
        barMessage = tbapi.InstrumentMessage()
        
        # Define message type name according to the Timebase schema type name
        barMessage.typeName = 'com.epam.deltix.timebase.messages.BarMessage'
        
        print('Start loading to ' + streamKey)    
        
        for i in range(100):    
            # Define instrument information
            barMessage.symbol = 'AAPL' if i % 2 == 0 else 'MSFT'
                        
            # Define other message properties
            barMessage.originalTimestamp = 0
            
            # 'undefined' currency code
            barMessage.currencyCode = 999
            barMessage.exchangeId = 'NYSE'
            barMessage.open = 10.0 + i * 2.2
            barMessage.close = 20.0 + i * 3.3
            barMessage.high = 30.0 + i * 4.4
            barMessage.low = 40.0 + i * 5.5
            barMessage.volume = 60.0 + i * 6.6
            
            # Send message
            loader.send(barMessage)
                
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