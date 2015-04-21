package org.opendaylight.lacp.state;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
public class MuxContextTest {
	MuxContext context;
	MuxState stateObj;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		stateObj = new MuxState();
		context = new MuxContext();
		stateObj.setStateFlag(LacpConst.MUX_STATES.MUX_ATTACHED);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMuxContext() {
		MuxContext m = new MuxContext();
	}

	@Test
	public void testSetState() throws Exception {
		context.setState(stateObj);		
	}

	@Test
	public void testGetState() throws Exception {
		assertEquals(LacpConst.MUX_STATES.MUX_ATTACHED, stateObj.getStateFlag());
		context.setState(stateObj);		
		assertEquals(LacpConst.MUX_STATES.MUX_ATTACHED, context.getState().getStateFlag());
	}

}
