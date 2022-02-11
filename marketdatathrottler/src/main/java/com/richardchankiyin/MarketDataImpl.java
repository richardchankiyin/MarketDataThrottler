package com.richardchankiyin;

public class MarketDataImpl implements MarketData {

	private String symbol;
	private double price;
	private long updateTime;
	public MarketDataImpl(String symbol, double price, long updateTime) {
		this.symbol = symbol;
		this.price = price;
		this.updateTime = updateTime;
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

	public String toString() {
		return new StringBuilder(this.symbol).append("|").append(this.price).append("|").append(this.updateTime).toString();
	}
	
}
