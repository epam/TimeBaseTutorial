from sortedcollections import SortedDict
from typing import Iterable, List

    
def add_key(t: tuple):
    t[1]['key'] = t[1]['symbol'] + t[1]['side'] + str(t[0])
    return t[1]


def to_dict(symbol, msg):
    d = {}
    d['symbol'] = symbol
    d['price'] = msg.price
    d['size'] = msg.size
    d['numberOfOrders'] = msg.numberOfOrders
    d['side'] = msg.side
    d['_level'] = msg.level
    d['key'] = None
    return d


class Book:
    def __init__(self, symbol: str):
        self.asks = SortedDict()
        self.bids = SortedDict()
        
    def update(self, *entries: Iterable[dict]) -> None:
        for entry in entries:
            if entry['side'] == 'BID':
                self.bids[entry['price']] = entry
            else:
                self.asks[entry['price']] = entry
    
    def remove(self, side: str, price: float) -> None:
        if side == 'BID':
            self.bids.pop(price, None)
        else:
            self.asks.pop(price, None)
    
    def get_bids(self, size=-1) -> List[dict]:
        if size == -1 or size >= len(self.bids):
            return list(map(add_key, enumerate(reversed(self.bids.values()))))
        result = list(self.bids.values())[len(self.bids) - size:]
        result.reverse()
        return list(map(add_key, enumerate(result)))
    
    def get_asks(self, size=-1) -> List[dict]:
        if size == -1 or size >= len(self.asks):
            return list(map(add_key, enumerate(self.asks.values())))
        return list(map(add_key, enumerate(self.asks.values()[:size])))
    
    def clear(self) -> None:
        self.asks.clear()
        self.bids.clear()
