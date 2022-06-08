import sys, os, struct

import tbapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    db = tbapi.TickDb.createFromUrl(timebase)

    # Open in write mode
    db.open(False)

    print('Connected to ' + timebase)

    # read QQL from file
    with open('qql/sample_l2.qql', 'r') as qqlFile:
        qql = qqlFile.read()

    # execute QQL and check result
    cursor = db.executeQuery(qql)
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
