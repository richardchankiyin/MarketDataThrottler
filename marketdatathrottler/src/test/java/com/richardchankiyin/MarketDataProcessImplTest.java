package com.richardchankiyin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;



public class MarketDataProcessImplTest {

	@Mock
	Receiver receiver;
	
	@Mock
	MarketData marketData;
	
	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}
	
	
	@Test
	public void testStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.start();
		assertTrue(impl.isStarted());
	}

	@Test
	public void testLoadSymbolsBeforeStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		
		MarketData test1 = impl.getCache("TEST1");
		assertEquals("TEST1", test1.getSymbol());
		assertEquals(-1, test1.getUpdateTime());
		assertEquals(-1, test1.getPublishTime());
		MarketData test2 = impl.getCache("TEST2");
		assertEquals("TEST2", test2.getSymbol());
		assertEquals(-1, test2.getUpdateTime());
		assertEquals(-1, test2.getPublishTime());
		MarketData test3 = impl.getCache("TEST3");
		assertEquals("TEST3", test3.getSymbol());
		assertEquals(-1, test3.getUpdateTime());
		assertEquals(-1, test3.getPublishTime());
		MarketData test4 = impl.getCache("TEST4");
		assertNull(test4);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testLoadSymbolsAfterStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.start();
		impl.loadSymbols(Arrays.asList("TEST"));
	}

	@Test
	public void testRegisterReceiverBeforeStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		Receiver receiver1 = Mockito.mock(Receiver.class);
		Mockito.doNothing().when(receiver1).onReceive(Mockito.any());
		Receiver receiver2 = Mockito.mock(Receiver.class);
		Mockito.doNothing().when(receiver2).onReceive(Mockito.any());
		
		impl.registerReceiver(receiver1);
		impl.registerReceiver(receiver2);
		assertTrue(2 == impl.getNoOfRegisteredReceivers());
	}
	
	@Test(expected=IllegalStateException.class)
	public void testRegisterReceiverAfterStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.start();
		Mockito.doNothing().when(receiver).onReceive(Mockito.any());				
		impl.registerReceiver(receiver);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testOnMessageBeforeStart() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.onMessage(marketData);
	}
	
	@Test
	public void testOnMessageAfterStartDataNotCachedDueToNotLoadedBefore() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		impl.start();
		MarketData data = new MarketDataImpl("TEST4", 10.2, impl.getCurrentTimeInMilliseconds());
		impl.onMessage(data);
		assertNull(impl.getCache("TEST4"));
	}
	
	@Test
	public void testOnMessageAfterStartDataCached() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		impl.start();
		MarketData data = new MarketDataImpl("TEST1", 10.2, impl.getCurrentTimeInMilliseconds());
		impl.onMessage(data);
		MarketData cache = impl.getCache("TEST1");
		assertTrue(10.2 == cache.getPrice());
	}
	
	@Test
	public void testOnMessageAfterStartLatestDataCached() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		impl.start();
		MarketData data = new MarketDataImpl("TEST1", 10.2, impl.getCurrentTimeInMilliseconds());
		MarketData data2 = new MarketDataImpl("TEST1", 10.4, impl.getCurrentTimeInMilliseconds());
		impl.onMessage(data);
		impl.onMessage(data2);
		MarketData cache = impl.getCache("TEST1");
		assertTrue(10.4 == cache.getPrice());
	}
	
	@Test
	public void testOnMessageAfterStartStaleDataNotCached() {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		impl.start();
		MarketData data = new MarketDataImpl("TEST1", 10.2, impl.getCurrentTimeInMilliseconds());
		MarketData data2 = new MarketDataImpl("TEST1", 10.4, impl.getCurrentTimeInMilliseconds() + 10);
		impl.onMessage(data2);
		try {
			impl.onMessage(data);
		} catch (Exception e) {
			assertEquals(IllegalStateException.class, e.getClass());
			assertEquals("data not updated", e.getMessage());
		}
		
		MarketData cache = impl.getCache("TEST1");
		assertTrue(10.4 == cache.getPrice());
	}
	
	
	@Test
	public void testPublishAggregatedMarketData() throws Exception{
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		List<String> symbolsReceived = new ArrayList<>();
		List<Double> priceReceived = new ArrayList<>();
		List<Long> updateTimeReceived = new ArrayList<>();
		final long updateTime = impl.getCurrentTimeInMilliseconds();
		Receiver receiver = new Receiver() {
			@Override
			public void onReceive(MarketData data) {
				symbolsReceived.add(data.getSymbol());
				priceReceived.add(data.getPrice());
				updateTimeReceived.add(data.getUpdateTime());
			}
		};
		impl.registerReceiver(receiver);
		impl.start();
		MarketData data = new MarketDataImpl("TEST1", 10.2, updateTime, impl.getCurrentTimeInMilliseconds());
		impl.publishAggregatedMarketData(data);
		// we want to wait for a while to have receiver received
		Thread.sleep(100);
		assertEquals(1, symbolsReceived.size());
		assertEquals(1, priceReceived.size());
		assertEquals(1, updateTimeReceived.size());
		assertEquals("TEST1", symbolsReceived.get(0));
		assertTrue(10.2 == priceReceived.get(0));
		assertTrue(updateTime == updateTimeReceived.get(0));
	}
	
	@Test
	public void testOnMessageAndPublishAutomaticallyUnderThreshold() throws Exception{
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		impl.loadSymbols(Arrays.asList("TEST1","TEST2","TEST3"));
		List<String> symbolsReceived = new ArrayList<>();
		List<Double> priceReceived = new ArrayList<>();
		List<Long> updateTimeReceived = new ArrayList<>();
		final long updateTime = impl.getCurrentTimeInMilliseconds();
		Receiver receiver = new Receiver() {
			@Override
			public void onReceive(MarketData data) {
				symbolsReceived.add(data.getSymbol());
				priceReceived.add(data.getPrice());
				updateTimeReceived.add(data.getUpdateTime());
			}
		};
		impl.registerReceiver(receiver);
		impl.start();
		MarketData data1 = new MarketDataImpl("TEST1", 10.2, updateTime, impl.getCurrentTimeInMilliseconds());
		MarketData data2 = new MarketDataImpl("TEST2", 10.3, updateTime, impl.getCurrentTimeInMilliseconds());
		MarketData data3 = new MarketDataImpl("TEST3", 10.4, updateTime, impl.getCurrentTimeInMilliseconds());
		impl.onMessage(data1);
		impl.onMessage(data2);
		impl.onMessage(data3);		
		// we want to wait for a while to have receiver received
		Thread.sleep(100);
		assertEquals(3, symbolsReceived.size());
		assertEquals(3, priceReceived.size());
		assertEquals(3, updateTimeReceived.size());
		assertEquals("TEST1", symbolsReceived.get(0));
		assertTrue(10.2 == priceReceived.get(0));
		assertEquals("TEST2", symbolsReceived.get(1));
		assertTrue(10.3 == priceReceived.get(1));
		assertEquals("TEST3", symbolsReceived.get(2));
		assertTrue(10.4 == priceReceived.get(2));
		
		impl.stop();
	}
	
	@Test
	public void testOnMessageAndPublishAutomaticallyAtThreshold() throws Exception {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			symbols.add("TEST" + i);
		}
		impl.loadSymbols(symbols);
		List<String> symbolsReceived = new ArrayList<>();
		List<Double> priceReceived = new ArrayList<>();
		List<Long> updateTimeReceived = new ArrayList<>();
		final long updateTime = impl.getCurrentTimeInMilliseconds();
		Receiver receiver = new Receiver() {
			@Override
			public void onReceive(MarketData data) {
				symbolsReceived.add(data.getSymbol());
				priceReceived.add(data.getPrice());
				updateTimeReceived.add(data.getUpdateTime());
			}
		};
		impl.registerReceiver(receiver);
		impl.start();
		
		
		List<MarketData> datas = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			datas.add(new MarketDataImpl("TEST" + i, 10, updateTime));
		}
		for (MarketData data: datas) {
			impl.onMessage(data);
		}
		// we want to wait for a while to have receiver received
		Thread.sleep(100);
		assertEquals(100, symbolsReceived.size());
		assertEquals(100, priceReceived.size());
		assertEquals(100, updateTimeReceived.size());
	}

	@Test
	public void testOnMessageAndPublishAutomaticallyOverThreshold() throws Exception {
		MarketDataProcessorImpl impl = new MarketDataProcessorImpl();
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < 150; i++) {
			symbols.add("TEST" + i);
		}
		impl.loadSymbols(symbols);
		List<String> symbolsReceived = new ArrayList<>();
		List<Double> priceReceived = new ArrayList<>();
		List<Long> updateTimeReceived = new ArrayList<>();
		final long updateTime = impl.getCurrentTimeInMilliseconds();
		Receiver receiver = new Receiver() {
			@Override
			public void onReceive(MarketData data) {
				symbolsReceived.add(data.getSymbol());
				priceReceived.add(data.getPrice());
				updateTimeReceived.add(data.getUpdateTime());
			}
		};
		impl.registerReceiver(receiver);
		impl.start();
		
		
		List<MarketData> datas = new ArrayList<>();
		for (int i = 0; i < 150; i++) {
			datas.add(new MarketDataImpl("TEST" + i, 10, updateTime));
		}
		for (MarketData data: datas) {
			impl.onMessage(data);
		}
		// we want to wait for a while to have receiver received
		Thread.sleep(100);
		assertEquals(100, symbolsReceived.size());
		assertEquals(100, priceReceived.size());
		assertEquals(100, updateTimeReceived.size());
		// we want to wait for a while to have receiver received
		Thread.sleep(1100);
		assertEquals(150, symbolsReceived.size());
		assertEquals(150, priceReceived.size());
		assertEquals(150, updateTimeReceived.size());
	}
}
