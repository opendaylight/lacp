/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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

public class RxDefaultedStateTest {
        LacpPort port;
        RxContext context;
        RxDefaultedState rxState;
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
        rxState = new RxDefaultedState();
        stateObj = new RxState();
	stateObj.setStateFlag(LacpConst.RX_STATES.RX_DEFAULTED);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.RX_STATES.RX_DEFAULTED, rxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		rxState.setStateFlag(LacpConst.RX_STATES.RX_EXPIRED);
		assertEquals(LacpConst.RX_STATES.RX_EXPIRED, rxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		stateObj.setStateFlag(LacpConst.RX_STATES.RX_CURRENT);
		context.setState(stateObj);
//		rxState.executeStateAction(context, port, bpdu);
	}

	@Test
	public void testRxDefaultedState() throws Exception {
		assertEquals(LacpConst.RX_STATES.RX_DEFAULTED, rxState.getStateFlag());
	}

	@Test
	public void testUpdate_default_selected() throws Exception {
		//rxState.updateDefaultSelected(port);
	}

}
