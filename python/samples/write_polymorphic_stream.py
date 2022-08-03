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
        streamKey = 'trade_bbo'
        
        # Get stream from the timebase
        stream = db.getStream(streamKey)
        
        # if stream not found, try to create new
        if stream == None:
            print('Stream ' + streamKey + ' not exitsts, creating new one...')
            stream = createStreamQql(db, streamKey)
        
        # Create a Message Loader for the selected stream and provide loading options
        loader = stream.createLoader(tbapi.LoadingOptions())
        
        # Create BestBidOffer message
        bboMessage = tbapi.InstrumentMessage()
        
        # Define message type name according to the Timebase schema type name
        # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
        bboMessage.typeName = 'com.epam.deltix.timebase.messages.BestBidOfferMessage'
        
        # Create Trade Message message
        tradeMessage = tbapi.InstrumentMessage()    
        # Define message type according to the Timebase schema type name
        # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
        tradeMessage.typeName = 'com.epam.deltix.timebase.messages.TradeMessage'

        print('Start loading to ' + streamKey)    
        
        # get current time in UTC
        now = datetime.utcnow() - datetime(1970, 1, 1)
        
        # Define message timestamp as Epoch time in nanoseconds 
        ns = now.total_seconds() * 1e9;
            
        for i in range(100):    
            ns = ns + 1000000
            if (i % 2 == 0):
                # Define instrument type and symbol for the message
                tradeMessage.symbol = 'AAPL' if i % 3 == 0 else 'USGG5YR'
                            
                tradeMessage.timestamp = ns;
                
                # Define other message properties
                tradeMessage.originalTimestamp = 0
                # 'undefined' currency code
                tradeMessage.currencyCode = 999
                tradeMessage.exchangeId = 'NYSE'
                tradeMessage.price = 10.0 + i * 2.2
                tradeMessage.size = 20.0 + i * 3.3            
                tradeMessage.aggressorSide = 'BUY'
                tradeMessage.netPriceChange = 30.0 + i * 4.4
                tradeMessage.eventType = 'TRADE'
                
                # Send message
                loader.send(tradeMessage)
            else:
                bboMessage.symbol = 'AAPL' if i % 3 == 0 else 'USGG5YR'

                bboMessage.timestamp = ns;            
                
                bboMessage.originalTimestamp = 0
                bboMessage.currencyCode = 999
                bboMessage.sequenceNumber = 0
                bboMessage.bidPrice = 126.0 + i * 2.2
                bboMessage.bidSize = 127.0 + i * 3.3
                bboMessage.bidExchangeId = 'NYSE'
                bboMessage.offerPrice = 128.0 + i * 4.4
                bboMessage.offerSize = 129.0 + i * 5.5
                bboMessage.offerExchangeId = 'NYSE'
                
                # Send message
                loader.send(bboMessage)
                
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