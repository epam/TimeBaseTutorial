import sys, os, struct
import dxapi

# Timebase URL specification, pattern is "dxtick://<host>:<port>"
timebase = 'dxtick://localhost:8011'

try:
    # Create timebase connection
    db = dxapi.TickDb.createFromUrl(timebase)

    # Open in read-only mode
    db.open(True)
    print('Connected to ' + timebase)

    # Get streams
    streams = db.listStreams()

    for stream in streams:
        options = stream.options()
        print(stream.key())
        print('   Name: ' + str(options.name()))
        print('   Scope: ' + str(options.scope))
        print('   DF: ' + str(options.distributionFactor))
        print('   Description: ' + str(options.description()))
finally:
    # database connection should be closed anyway
    if (db.isOpen()):
        db.close()
    print("Connection " + timebase + " closed.")
