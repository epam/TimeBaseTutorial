import sys, os, inspect, struct
import math
import time
import calendar
from datetime import datetime

import tbapi

class PriceInfo:
    offerSize = 0
    offerPrice = float('nan')

    bidSize = 0
    bidPrice = float('nan')

    def __init__(self, name):
        self.name = name


# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    # db = tbapi.TickDb.createFromUrl(timebase)
    db = tbapi.TickDb.createFromUrl(timebase, None, None)
    
    # Open in read-only mode
    db.open(True)

    print('Connected to ' + timebase)

    # Define name of the stream    
    streamKey = 'trade_bbo'

    # Get stream from the timebase
    stream = db.getStream(streamKey)    
    if stream == None:
        raise Exception('Stream ' + streamKey + ' not found, please, create stream')
        
    options = tbapi.SelectionOptions()
    options.reverse = True

    # Create cursor with empty subscription
    cursor = db.createCursor(None, options)

    # # subscribe to all instruments
    cursor.subscribeToAllEntities()
    
    # 2. Subscribe to the Trade and BestBidOffer Messages
    types = [
        'com.epam.deltix.timebase.messages.BestBidOfferMessage'
    ]
    cursor.addTypes(types)

    # 3. Subscribe to the data stream(s)
    cursor.addStreams([stream])

    # Define subscription start time
    dTime = datetime(2030, 1, 1, 0, 0)
    # Start time is Epoch time in milliseconds
    startTime = calendar.timegm(dTime.timetuple()) * 1000

    # 4. Reset cursor to the subscription time
    cursor.reset(startTime)

    prices = {}
    
    try:
        while cursor.next():
            message = cursor.getMessage()           
            
            # Message time is Epoch time in nanoseconds
            time = message.timestamp / 1e9
            time = datetime.utcfromtimestamp(time)

            latest = prices.get(message.symbol)
            if latest is None:
                latest = PriceInfo(message.symbol)
                prices.update({message.symbol: latest})

            latest.bidPrice = message.bidPrice
            latest.bidSize = message.bidSize

            latest.offerPrice = message.offerPrice
            latest.offerSize = message.offerSize

            cursor.removeEntities([
                message.symbol
            ])

            #if message.typeName == 'deltix.timebase.api.messages.BestBidOfferMessage':
            #    print("BBO (" + str(time) + "," + message.symbol + ") bid price: " + str(message.bidPrice) + ", ask price: " + str(message.offerPrice))

        for key in prices:
            print("Latest bidPrice [" + key + "]" + str(prices[key].bidPrice))
            print("Latest askPrice [" + key + "]" + str(prices[key].offerPrice))

    finally:
        # cursor should be closed anyway
        cursor.close()
        cursor = None



finally:
    # database connection should be closed anyway
    if db.isOpen():
        db.close()
        print("Connection " + timebase + " closed.")
    
