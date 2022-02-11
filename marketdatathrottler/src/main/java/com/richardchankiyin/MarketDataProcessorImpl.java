package com.richardchankiyin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MarketDataProcessorImpl extends MarketDataProcessor {
	private final IllegalStateException INSTANCE_START_STATE_EXCEPTION = new IllegalStateException("the instance is started");
	private final IllegalStateException INSTANCE_NOT_START_STATE_EXCEPTION = new IllegalStateException("the instance is not started");
	
	private static final Logger logger = Logger.getLogger("com.richardchankiyin");
	private static final int QUEUE_CAPACITY = 3000;
	private final List<Receiver> receivers = new ArrayList<>();
	private final Map<String, AtomicReference<MarketData>> marketDataCache = new HashMap<>();
	private final ArrayBlockingQueue<String> publishQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY, true);
	private boolean isStarted = false;
	
	public long getCurrentTimeInMilliseconds() {
		return System.currentTimeMillis();
	}
	
	public boolean isStarted() {
		return this.isStarted;
	}
	
	public void start() {
		isStarted = true;
	}
	
	public void stop() {
		//TODO handling of stopping process
		isStarted = false;		
	}
	
	public MarketData getCache(String symbol) {
		AtomicReference<MarketData> ref = marketDataCache.get(symbol);
		if (ref != null) {
			return ref.get();
		} else {
			return null;
		}
	}
	
	public int getNoOfRegisteredReceivers() {
		return receivers.size();
	}
	
	
	/********* Pre-start calls **************/
	/* can only perform loadSymbols when isStarted = false */
	public void loadSymbols(List<String> symbols) {
		if (!isStarted()) {
			for (String symbol: symbols) {
				logger.log(Level.INFO, "init symbol: {0}",symbol);
				MarketData data = new MarketDataImpl(symbol, Double.NaN, getCurrentTimeInMilliseconds());
				AtomicReference<MarketData> ref = new AtomicReference<>(data);
				marketDataCache.put(symbol, ref);
			}
		} else {
			logger.warning("the instance isStarted!");
			throw INSTANCE_START_STATE_EXCEPTION;
		}
	}
	
	
	/* can only perform loadSymbols when isStarted = false */
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
					//TODO perform compare and swap. And retry only once
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

	@Override
	public void publishAggregatedMarketData(MarketData data) {
		
		// TODO Auto-generated method stub

	}

}
