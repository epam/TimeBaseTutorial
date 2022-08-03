import sys, os, struct

import tbapi
from pprint import pprint
            
from collections import defaultdict

def main():
    try:
        db = tbapi.TickDb.createFromUrl("dxtick://localhost:8011")
        db.open(True)
        stream = db.getStream("trade_bbo")
        if stream == None:
            raise Exception('Stream trade_bbo not found, please, create stream')
            
        messages = df_from_stream(db, stream, ['timestamp', 'symbol'])
        pprint(messages.head())
    finally:
        if (db.isOpen()):
            db.close()

def stream_to_dict(db, stream, fields=None, ts_from=0, ts_to=tbapi.JAVA_LONG_MAX_VALUE):
    if ts_to > tbapi.JAVA_LONG_MAX_VALUE:
        ts_to = tbapi.JAVA_LONG_MAX_VALUE
    if not db.isOpen():
        raise Exception('Database is not opened.')
    options = tbapi.SelectionOptions()
    options.to = ts_to
    messages = []
    table = defaultdict(list)
    with stream.trySelect(ts_from, options, None, None) as cursor:
        counter = 0
        while cursor.next():
            message = vars(cursor.getMessage())
            messages.append(message)
            if fields is None:
                def to_write(x):
                    return True
            else:
                def to_write(x):
                    return x in fields
            for key in table.keys():
                if key in message:
                    table[key].append(message[key])
                    del message[key]
                else:
                    table[key].append(None)
            for key in message:
                if to_write(key):
                    table[key] = [None] * counter
                    table[key].append(message[key])
            counter += 1
    return table

def df_from_stream(db, stream, fields=None, ts_from=0, ts_to=922337203685477580):
    if 'pandas' not in sys.modules:
        import pandas as pd
    else:
        pd = sys.modules['pandas']
    table = stream_to_dict(db, stream, fields, ts_from, ts_to)
    table = pd.DataFrame(table)
    table.timestamp = pd.to_datetime(table.timestamp)
    return table
    
if __name__ == '__main__':
    main()