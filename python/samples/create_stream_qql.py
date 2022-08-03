import sys, os, struct

import tbapi

def main():
    # Timebase URL specification, pattern is "dxtick://<host>:<port>"
    timebase = 'dxtick://localhost:8011'
    try:
        # Create timebase connection
        db = tbapi.TickDb.createFromUrl(timebase)

        # Open in write mode
        db.open(False)

        print('Connected to ' + timebase)
        
        createStreamQql(db, 'universal')
        createStreamQql(db, 'bars')
        createStreamQql(db, 'trade_bbo')
    finally:  # database connection should be closed anyway
        if (db.isOpen()):
            db.close()
        print("Connection " + timebase + " closed.")

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

if __name__ == '__main__':
    main()