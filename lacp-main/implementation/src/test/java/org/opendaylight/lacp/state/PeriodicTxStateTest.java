package org.opendaylight.lacp.state;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
public class PeriodicTxStateTest {
    PeriodicTxState stateObj;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		stateObj = new PeriodicTxState();
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.PERIODIC_TX);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPeriodicTxState() throws Exception {
		PeriodicTxState p = new PeriodicTxState();
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY, p.getStateFlag());	
	}

	@Test
	public void testGetStateFlag() throws Exception {	
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_TX, stateObj.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY);
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY, stateObj.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
	}
}
