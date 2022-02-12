MarketDataThrottler
==================
This is to demonstrate the build of a simple market data throttler

Requirement
------------
There is a MarketDataProcessor class below. This class receive real time market data
from exchange then sending them to other applications.
 - The MarketDataProcessor receives messages from some source via the onMessage
method. There is single thread calling the onMessage in unknown rate per second.
 - Modify the MarketDataProcessor class to,
   - At least fulfill,
     - Ensure that the publishAggregatedMarketData method, which is sending
message, is not called any more than 100 times/sec where this period is a
sliding window.
     -  Ensure each symbol will not have more than one update per second
   - Prefer to fulfill,
     - Ensure each symbol will always have the latest market data when it is
published
     - Ensure the latest market data on each symbol will be published
- The Message class contains market data. Each market data contains Symbol, price and
update time.
