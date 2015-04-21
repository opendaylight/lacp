package org.opendaylight.lacp.state;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
public class MuxStateTest {
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
		stateObj.setStateFlag(LacpConst.MUX_STATES.MUX_ATTACHED);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMuxState() {
		MuxState m = new MuxState();
		assertEquals(LacpConst.MUX_STATES.MUX_DUMMY, m.getStateFlag());
	}

	@Test
	public void testGetStateFlag() {
		assertEquals(LacpConst.MUX_STATES.MUX_ATTACHED, stateObj.getStateFlag());
	}

	@Test
	public void testSetStateFlag() {
		stateObj.setStateFlag(LacpConst.MUX_STATES.MUX_WAITING);
		assertEquals(LacpConst.MUX_STATES.MUX_WAITING,  stateObj.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() {
	}

}
