package com.richardchankiyin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MarketDataProcessorImpl extends MarketDataProcessor {
	private final IllegalStateException INSTANCE_START_STATE_EXCEPTION = new IllegalStateException("the instance is started");
	
	private static Logger logger = Logger.getLogger("com.richardchankiyin");
	private List<Receiver> receivers = new ArrayList<>();
	private Map<String, AtomicReference<MarketData>> marketDataCache = new HashMap<>();
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
				logger.log(Level.INFO, "init symbol: {}",symbol);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void publishAggregatedMarketData(MarketData data) {
		// TODO Auto-generated method stub

	}

}
