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
import org.opendaylight.lacp.inventory.LacpBond;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class MuxWaitingStateTest {

	LacpPort port;
	MuxContext context;
	MuxWaitingState muxState;
	MuxState stateObj;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
                port = Mockito.mock(LacpPort.class);
		context = new MuxContext();
		muxState = new MuxWaitingState();
		stateObj = new MuxState();
		stateObj.setStateFlag(LacpConst.MUX_STATES.MUX_WAITING);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception  {
		assertEquals(LacpConst.MUX_STATES.MUX_WAITING, muxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		muxState.setStateFlag(LacpConst.MUX_STATES.MUX_DETACHED);
		assertEquals(LacpConst.MUX_STATES.MUX_DETACHED, muxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		context.setState(stateObj);
		muxState.executeStateAction(context, port);
	}

	@Test
	public void testMuxWaitingState() throws Exception {
		assertEquals(LacpConst.MUX_STATES.MUX_WAITING, muxState.getStateFlag());
	}

}
