import sys, os, struct
import dxapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    db = dxapi.TickDb.createFromUrl(timebase)

    # Open in write mode
    db.open(False)

    print('Connected to ' + timebase)

    # read QQL from file
    with open('qql/bars1min.qql', 'r') as qqlFile:
        barsQQL = qqlFile.read()

    # execute QQL and check result
    cursor = db.executeQuery(barsQQL)
    try:
        if (cursor.next()):
            message = cursor.getMessage()
            print('Query result: ' + message.messageText)
        else:
            print('Unknown result of query.')
    finally:
        if (cursor != None):
            cursor.close()
    
finally:  # database connection should be closed anyway
    if (db.isOpen()):
        db.close()
    print("Connection " + timebase + " closed.")
