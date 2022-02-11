package com.richardchankiyin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;



public class MarketDataProcessImplTest {

	@Mock
	Receiver receiver;
	
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
		MarketData test2 = impl.getCache("TEST2");
		assertEquals("TEST2", test2.getSymbol());
		MarketData test3 = impl.getCache("TEST3");
		assertEquals("TEST3", test3.getSymbol());
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

}
