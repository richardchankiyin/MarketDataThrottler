package com.richardchankiyin;

import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < 150; i++) {
			symbols.add("TEST" + i);
		}
		impl.loadSymbols(symbols);
		impl.registerReceiver(new ConsoleOutputReceiver());
		impl.start();
		
		final long duration = 1000 * 10;
		long startTime = impl.getCurrentTimeInMilliseconds();
		long currentTime = startTime;
		while (duration >= (currentTime - startTime)) {
		
			List<MarketData> datas = new ArrayList<>();
			for (int i = 0; i < 150; i++) {
				datas.add(new MarketDataImpl("TEST" + i, 10 + Math.random(), impl.getCurrentTimeInMilliseconds()));
			}
			for (MarketData data: datas) {
				impl.onMessage(data);
				long sleepTime = (long)(100 * Math.random());
				Thread.sleep(sleepTime);
			}
			currentTime = impl.getCurrentTimeInMilliseconds();
		}
		
		impl.stop();
    }
}
