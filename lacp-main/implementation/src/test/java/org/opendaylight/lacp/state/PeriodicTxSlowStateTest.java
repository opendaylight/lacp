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

public class PeriodicTxSlowStateTest {
	LacpPort port;
	PeriodicTxContext context;
	PeriodicTxSlowState periodicTxState;
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
		periodicTxState = new PeriodicTxSlowState();
		stateObj = new PeriodicTxState();
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.SLOW_PERIODIC);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.SLOW_PERIODIC, periodicTxState.getStateFlag());
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
	public void testPeriodicTxSlowState() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.SLOW_PERIODIC, periodicTxState.getStateFlag());
	}
}
