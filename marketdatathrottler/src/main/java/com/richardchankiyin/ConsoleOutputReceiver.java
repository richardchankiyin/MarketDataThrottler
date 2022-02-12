package com.richardchankiyin;

public class ConsoleOutputReceiver implements Receiver {

	@Override
	public void onReceive(MarketData data) {
		System.out.println("received: " + data);
	}

}
