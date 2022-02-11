package com.richardchankiyin;

public abstract class MarketDataProcessor {
	public abstract void onMessage(MarketData data);
	public abstract void publishAggregatedMarketData(MarketData data);
}
