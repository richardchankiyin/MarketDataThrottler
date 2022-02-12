package com.richardchankiyin;

import java.util.Objects;

public class MarketDataImpl implements MarketData {

	public static final long PUBLISH_TIME_NO_PUB = -1;
	public static final long UPDATE_TIME_INIT_LOAD = -1;
	private String symbol;
	private double price;
	private long updateTime;
	private long publishTime; // -1 means not published
	public MarketDataImpl(String symbol, double price, long updateTime, long publishTime) {
		Objects.requireNonNull(symbol);
		this.symbol = symbol;
		this.price = price;
		this.updateTime = updateTime;
		this.publishTime = publishTime;
	}
	
	public MarketDataImpl(String symbol, double price) {
		this(symbol, price, UPDATE_TIME_INIT_LOAD, PUBLISH_TIME_NO_PUB);
	}
	
	public MarketDataImpl(String symbol, double price, long updateTime) {
		this(symbol, price, updateTime, PUBLISH_TIME_NO_PUB);
	}
	
	
	@Override
	public String getSymbol() {
		return this.symbol;
	}

	@Override
	public double getPrice() {
		return this.price;
	}

	@Override
	public long getUpdateTime() {
		return this.updateTime;
	}
	
	@Override
	public long getPublishTime() {
		return this.publishTime;
	}	

	public String toString() {
		return new StringBuilder(this.symbol).append("|")
				.append(this.price).append("|")
				.append(this.updateTime).append("|")
				.append(this.publishTime).toString();
	}


	
}
