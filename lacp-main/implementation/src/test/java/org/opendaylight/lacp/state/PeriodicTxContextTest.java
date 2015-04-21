package org.opendaylight.lacp.state;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;

public class PeriodicTxContextTest {
	PeriodicTxContext context;
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
		context = new PeriodicTxContext();	
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPeriodicContext() {	}

	@Test
	public void testSetState() throws Exception {
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.NO_PERIODIC);
		context.setState(stateObj);	
	}

	@Test
	public void testGetState() throws Exception {
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY);
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY, stateObj.getStateFlag());
		context.setState(stateObj);		
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY,context.getState().getStateFlag());
	}

}
