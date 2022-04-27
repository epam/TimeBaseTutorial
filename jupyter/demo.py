# deltix TimeBase client
import tbapi
from tbapi import InstrumentMessage

# FINOS Perspective
import perspective
from perspective import Table

# OrderBook
from orderbook import Book, to_dict

# Other libs
import threading
import asyncio
from datetime import datetime
import time
from datetime import datetime
from typing import List, Iterable
from sortedcollections import SortedDict, ItemSortedDict
import ipywidgets as widgets
import logging
logger = logging.getLogger()
logger.setLevel(logging.DEBUG)


def current_milli_time():
    return round(time.time() * 1000)


class Demo:
    
    def __init__(self, tb_url: str, stream_key: str, symbol: str, record_type: str, time_widget: widgets.Text, booksize=20):
        self.tb_url = tb_url
        self.stream_key = stream_key
        self.symbol = symbol
        self.record_type = record_type
        self.time_widget = time_widget
        self.booksize = booksize
        self.schema = {
            'key': str,
            'symbol': str,
            'side': str,
            'size': float,
            'price': float,
            'numberOfOrders': int
        }
        self.table = Table(self.schema, limit=booksize * 3, index='key')
        self.book = Book(symbol)
        self.last_updated = 0
        self.stop_reading = False
        self.init_book()
        
        
    def process_entry_update(self, entry: InstrumentMessage) -> None:
        if entry.action == 'DELETE':
            self.book.remove(entry.side, entry.price)
            t = time.time()
            if t - self.last_updated >= 0.5:
                self.last_updated = t
                self.table.update(self.book.get_bids(size=self.booksize))
                self.table.update(self.book.get_asks(size=self.booksize))
        elif entry.action == 'UPDATE':
            e = to_dict(self.symbol, entry)
            self.book.update(e)
            t = time.time()
            if t - self.last_updated >= 0.5:
                self.last_updated = t
                self.table.update(self.book.get_bids(size=self.booksize))
                self.table.update(self.book.get_asks(size=self.booksize))
        else:
            raise Exception(f'Unknown action type: {entry.action}')
    
    
    def process_entry_new(self, entry: InstrumentMessage) -> None:
        e = to_dict(self.symbol, entry)
        self.book.update(e)
        t = time.time()
        if t - self.last_updated >= 0.5:
            self.last_updated = t
            self.table.update(self.book.get_bids(size=self.booksize))
            self.table.update(self.book.get_asks(size=self.booksize))
            
            
    def process_snapshot(self, entries) -> None:
        self.book.clear()
        self.book.update(*map(lambda e: to_dict(self.symbol, e), entries))
        t = time.time()
        if t - self.last_updated >= 0.5:
            self.last_updated = t
            self.table.update(self.book.get_bids(size=self.booksize))
            self.table.update(self.book.get_asks(size=self.booksize))
        

    def init_book(self):
        db = tbapi.TickDb_createFromUrl(self.tb_url)
        try:
            db.open(True)
            stream = db.getStream(self.stream_key)
            options = tbapi.SelectionOptions()
            try:
                cursor = db.select(current_milli_time() - 10000, [stream], options, 
                                   [self.record_type], 
                                   [self.symbol])
                while cursor.next():
                    msg = cursor.getMessage()
                    if msg.packageType == 'PERIODICAL_SNAPSHOT':
                        self.process_snapshot(msg.entries)
                        break
            finally:
                cursor.close()
        finally:
            db.close()
            
    
    async def read_cursor(self):
        db = tbapi.TickDb_createFromUrl(self.tb_url)
        try:
            db.open(True)
            stream = db.getStream(self.stream_key)
            options = tbapi.SelectionOptions()
            options.live = True
            try:
                cursor = db.select(current_milli_time(), [stream], options, 
                                   [self.record_type], 
                                   [self.symbol])
                initialized = False
                while cursor.next() and not self.stop_reading and not initialized:
                    msg = cursor.getMessage()
                    if msg.packageType == 'PERIODICAL_SNAPSHOT' or msg.packageType == 'VENDOR_SNAPSHOT':
                        logging.info('received snapshot')
                        self.process_snapshot(msg.entries)
                        initialized = True
                        self.time_widget.value = str(datetime.fromtimestamp(msg.timestamp / 10 ** 9))
                while cursor.next() and not self.stop_reading:
                    msg = cursor.getMessage()
                    if msg.packageType == 'INCREMENTAL_UPDATE':
                        for entry in msg.entries:
                            if entry.typeName.endswith('L2EntryUpdate'):
                                self.process_entry_update(entry)
                            elif entry.typeName.endswith('L2EntryNew'):
                                self.process_entry_new(entry)
                    elif msg.packageType == 'PERIODICAL_SNAPSHOT' or msg.packageType == 'VENDOR_SNAPSHOT':
                        self.process_snapshot(msg.entries)
                    self.time_widget.value = str(datetime.fromtimestamp(msg.timestamp / 10 ** 9))
            finally:
                cursor.close()
        finally:
            db.close()
            
    
    def update_table(self):
        logging.info('Started streaming!')
        loop = asyncio.new_event_loop()
        task = loop.create_task(self.read_cursor())
        loop.call_later(60, task.cancel)

        try:
            loop.run_until_complete(task)
        except asyncio.CancelledError:
            logging.info("Stopped streaming!")
            pass
        
        
    def start(self):
        self.stop_reading = False
        self.thread = threading.Thread(target=self.update_table)
        self.thread.start()
        
        
    def stop(self):
        self.stop_reading = True
        self.thread.join()
        
        
    def clear(self):
        self.table.clear()
        self.book.clear()
    