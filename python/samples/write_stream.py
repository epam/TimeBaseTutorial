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
    
    # Open in read-write mode
    db.open(False)
	
    print('Connected to ' + timebase)

    # Define name of the stream    
    streamKey = 'ticks'
    
    # Get stream from the timebase
    stream = db.getStream(streamKey)
    
    # Create a Message Loader for the selected stream and provide loading options
    loader = stream.createLoader(dxapi.LoadingOptions())
    
    # Create BestBidOffer message
    bboMessage = dxapi.InstrumentMessage()
    
    # Define message type name according to the Timebase schema type name
    # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
    bboMessage.typeName = 'deltix.timebase.api.messages.BestBidOfferMessage'
    
    # Create Trade Message message
    tradeMessage = dxapi.InstrumentMessage()    
    # Define message type according to the Timebase schema type name
    # For the polymorphic streams, each message should have defined typeName to distinct messages on Timebase Server level.
    tradeMessage.typeName = 'deltix.timebase.api.messages.TradeMessage'

    print('Start loading to ' + streamKey)    
    
    for i in range(100):    
        # get current time in UTC
        now = datetime.utcnow() - datetime(1970, 1, 1)
        
        # Define message timestamp as Epoch time in nanoseconds 
        ns = now.total_seconds() * 1e9 + now.microseconds * 1000;
        
        if (i % 2 == 0):
            # Define instrument type and symbol for the message
            tradeMessage.instrumentType = 'EQUITY'
            tradeMessage.symbol = 'AAA'
                        
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
            bboMessage.instrumentType = 'BOND'
            bboMessage.symbol = 'USGG5YR'   

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




