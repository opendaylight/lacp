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

public class RxContextTest {
	RxContext context;
	RxState stateObj;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		stateObj = new RxState();
		context = new RxContext();
		stateObj.setStateFlag(LacpConst.RX_STATES.RX_CURRENT);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRxContext() throws Exception {	}

	@Test
	public void testSetState() throws Exception {
		context.setState(stateObj);
	}

	@Test
	public void testGetState() throws Exception {
		context.setState(stateObj);
		assertEquals(LacpConst.RX_STATES.RX_CURRENT, stateObj.getStateFlag());
		assertEquals(LacpConst.RX_STATES.RX_CURRENT,context.getState().getStateFlag());
	}

}
