package org.opendaylight.lacp.state;


import static org.junit.Assert.*;

import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpBond;
import org.opendaylight.lacp.core.LacpBpduInfo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class RxInitializeStateTest {
        LacpPort port;
        RxContext context;
        RxInitializeState rxState;
        RxState stateObj;
	LacpBpduInfo bpdu;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	bpdu = Mockito.mock(LacpBpduInfo.class);
        port = Mockito.mock(LacpPort.class);
        context = new RxContext();
        rxState = new RxInitializeState();
        stateObj = new RxState();
	stateObj.setStateFlag(LacpConst.RX_STATES.RX_INITIALIZE);		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.RX_STATES.RX_INITIALIZE, rxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		rxState.setStateFlag(LacpConst.RX_STATES.RX_CURRENT);
		assertEquals(LacpConst.RX_STATES.RX_CURRENT, rxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		context.setState(stateObj);
	//	rxState.executeStateAction(context, port, bpdu);		
	}

	@Test
	public void testRxInitilizeState() throws Exception{
		assertEquals(LacpConst.RX_STATES.RX_INITIALIZE, rxState.getStateFlag());
	}
}
