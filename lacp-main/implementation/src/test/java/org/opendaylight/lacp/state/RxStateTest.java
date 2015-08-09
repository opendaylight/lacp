/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.state;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class RxStateTest {
    RxState stateObj;
    LacpPort port;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		port = Mockito.mock(LacpPort.class);
		stateObj = new RxState();
		stateObj.setStateFlag(LacpConst.RX_STATES.RX_CURRENT);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRxState() {
		RxState r  = new RxState();
		assertEquals(LacpConst.RX_STATES.RX_DUMMY, r.getStateFlag());
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.RX_STATES.RX_CURRENT, stateObj.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		stateObj.setStateFlag(LacpConst.RX_STATES.RX_EXPIRED);
		assertEquals(LacpConst.RX_STATES.RX_EXPIRED, stateObj.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
	}

	@Test
	public void testRecordDefault() throws Exception {
		//stateObj.recordDefault(port);
	}
}
