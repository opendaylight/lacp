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

public class PeriodicTxFastStateTest {
	LacpPort port;
	PeriodicTxContext context;
	PeriodicTxFastState periodicTxState;
	PeriodicTxState stateObj;
	LacpBpduInfo bpdu;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
                port = Mockito.mock(LacpPort.class);
		context = new PeriodicTxContext();
		periodicTxState = new PeriodicTxFastState();
		stateObj = new PeriodicTxState();
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.FAST_PERIODIC);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.FAST_PERIODIC, periodicTxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		periodicTxState.setStateFlag(LacpConst.PERIODIC_STATES.NO_PERIODIC);
		assertEquals(LacpConst.PERIODIC_STATES.NO_PERIODIC, periodicTxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		context.setState(stateObj);
		periodicTxState.executeStateAction(context, port, bpdu);
	}

	@Test
	public void testPeriodicTxFastState() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.FAST_PERIODIC, periodicTxState.getStateFlag());
	}
}
