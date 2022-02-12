package com.richardchankiyin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MarketDataProcessorImpl extends MarketDataProcessor {
	private final IllegalStateException INSTANCE_START_STATE_EXCEPTION = new IllegalStateException("the instance is started");
	private final IllegalStateException INSTANCE_NOT_START_STATE_EXCEPTION = new IllegalStateException("the instance is not started");
	private final IllegalStateException DATA_NOT_UPDATED_EXCEPTION = new IllegalStateException("data not updated");
	
	private static final Logger logger = Logger.getLogger("com.richardchankiyin");
	private static final int MAXSENDPERTIMESLOT = 100;
	private static final int TIMESLOTMILLISEC = 1000;
	private final List<Receiver> receivers = new ArrayList<>();
	private ThreadPoolExecutor executorPool = null;
	
	private final Map<String, AtomicReference<MarketData>> marketDataCache = new HashMap<>();
	private ArrayBlockingQueue<String> publishQueue;
	private boolean isStarted = false;
	private Thread throttleControllerThread = null;
	
	/**
	 * To return current timestamp
	 * @return
	 */
	public long getCurrentTimeInMilliseconds() {
		return System.currentTimeMillis();
	}
	
	/**
	 * Check whether the instance is started
	 * @return
	 */
	public boolean isStarted() {
		return this.isStarted;
	}
	
	/**
	 * Start the instance
	 */
	public void start() {
		isStarted = true;
		// start publishing here
		throttleControllerThread = new Thread(()-> {
			int publishedCount = 0;
			long timestampStartCounting = this.getCurrentTimeInMilliseconds();
			while (isStarted) {
				if (publishQueue != null) {
					String symbol = publishQueue.peek();
					if (symbol != null)	{
						AtomicReference<MarketData> ref = marketDataCache.get(symbol);
						logger.log(Level.INFO, "symbol to be published: {0}", symbol);
						int attempts = 0;
						final int maxattempts = 2;
						boolean updateAttempt = true;
						boolean updated = false;
						MarketData dataPub = null;
						do {
							MarketData dataExp = ref.get();
							dataPub = new MarketDataImpl(dataExp.getSymbol(), dataExp.getPrice(), dataExp.getUpdateTime(), this.getCurrentTimeInMilliseconds());
							
							updated = ref.compareAndSet(dataExp, dataPub);
							attempts++;
							if (updated == true || attempts == maxattempts) {
								updateAttempt = false;
							}
						} while(updateAttempt);
						if (updated) {
							publishQueue.poll();
							publishAggregatedMarketData(dataPub);
							publishedCount++;
						}
						if (publishedCount >= MAXSENDPERTIMESLOT) {
							long currentTime = this.getCurrentTimeInMilliseconds();
							long sleepTime = currentTime - timestampStartCounting;
							try { Thread.sleep(sleepTime); } catch (InterruptedException ie) {}
						}
						long currentTime = this.getCurrentTimeInMilliseconds();
						long timeDiff = currentTime - timestampStartCounting;
						if (timeDiff >= TIMESLOTMILLISEC) {
							// reset count
							publishedCount = 0;
						}
					}					
				}
			}
		});
		throttleControllerThread.start();
	}
	
	/**
	 * Stop the instance
	 */
	public void stop() {
		isStarted = false;
		if (executorPool != null) executorPool.shutdown();
	}
	
	/**
	 * Get cached market data value based on Symbol
	 * @param symbol
	 * @return
	 */
	public MarketData getCache(String symbol) {
		AtomicReference<MarketData> ref = marketDataCache.get(symbol);
		if (ref != null) {
			return ref.get();
		} else {
			return null;
		}
	}
	
	/**
	 * Get no of registered receivers
	 * @return
	 */
	public int getNoOfRegisteredReceivers() {
		return receivers.size();
	}
	
	
	/********* Pre-start calls **************/
	/**
	 * load symbols into the instance. Can only do that
	 * before the instance is started
	 * @param symbols
	 */
	public void loadSymbols(List<String> symbols) {
		if (!isStarted()) {
			//init queue with size of symbols
			publishQueue = new ArrayBlockingQueue<>(symbols.size(), true);
			for (String symbol: symbols) {
				logger.log(Level.INFO, "init symbol: {0}",symbol);
				MarketData data = new MarketDataImpl(symbol, Double.NaN);
				AtomicReference<MarketData> ref = new AtomicReference<>(data);
				marketDataCache.put(symbol, ref);
			}
		} else {
			logger.warning("the instance isStarted!");
			throw INSTANCE_START_STATE_EXCEPTION;
		}
	}
	
	
	/**
	 * register receiver. Can only do that
	 * before the instance is started
	 * @param receiver
	 */
	public void registerReceiver(Receiver receiver) {
		if (!isStarted()) {
			receivers.add(receiver);
		} else {
			logger.warning("the instance isStarted!");
			throw INSTANCE_START_STATE_EXCEPTION;
		}
	}
	
	
	/********* Post-start calls **************/
	@Override
	public void onMessage(MarketData data) {
		if (!isStarted()) {
			throw INSTANCE_NOT_START_STATE_EXCEPTION;
		}
		if (data != null) {
			String symbol = data.getSymbol();
			if (symbol != null) {
				AtomicReference<MarketData> ref = marketDataCache.get(symbol);
				if (ref != null) {
					int attempts = 0;
					final int maxattempts = 2;
					boolean updateAttempt = true;
					boolean updated = false;
					do {
						double price = data.getPrice();
						long updateTime = data.getUpdateTime();
						MarketData dataUp = new MarketDataImpl(symbol, price, updateTime);
						MarketData dataExp = ref.get();
						// check update time of incoming entry is later then the one cached. If not skip
						if (dataExp.getUpdateTime() <= dataUp.getUpdateTime()) {
							// check whether data updated without published.
							boolean dataNotPub = dataExp.getPublishTime() == MarketDataImpl.PUBLISH_TIME_NO_PUB;
							boolean dataJustInit = dataExp.getUpdateTime() == MarketDataImpl.UPDATE_TIME_INIT_LOAD;
							updated = ref.compareAndSet(dataExp, dataUp);
							if (!dataNotPub || dataJustInit) {
								// if data publish, that means queue has no symbol
								boolean inserted = pushSymbolToQueue(symbol);
								logger.log(Level.INFO, "symbol {0} inserted into cache", symbol);
								logger.log(Level.INFO, "inserted? {0}", inserted);
							} else {
								logger.log(Level.INFO, "symbol {0} not published before this update", symbol);
							}
							
							attempts++;
							if (updated == true || attempts == maxattempts) {
								updateAttempt = false;
							}	
						} else {
							// do not update
							updateAttempt = false;
						}
					} while (updateAttempt);					
					if (!updated) {
						throw DATA_NOT_UPDATED_EXCEPTION;
					}
					
				} else {
					logger.log(Level.WARNING, "symbol {0} not loaded before", symbol);
				}
			} else {
				logger.warning("symbol is null");
			}
		} else {
			logger.warning("Market Data is null");
		}
		
	}
	
	protected boolean pushSymbolToQueue(String symbol) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "queue contains symbol: {0}", this.publishQueue.contains(symbol));
		}
		return this.publishQueue.offer(symbol);
	}
	

	@Override
	public void publishAggregatedMarketData(MarketData data) {
		int noOfReceivers = receivers.size();
		
		if (noOfReceivers > 0) {
			if (executorPool == null) {
				executorPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(noOfReceivers);
			}
			for (int i = 0; i < noOfReceivers; i++) {
				final Receiver receiver = receivers.get(i);
				executorPool.execute(()->{receiver.onReceive(data);});
			}
		}	
	}

	
}
