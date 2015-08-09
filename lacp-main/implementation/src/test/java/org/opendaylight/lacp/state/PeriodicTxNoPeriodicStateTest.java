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

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.lacp.util.LacpUtil;

public class PeriodicTxNoPeriodicStateTest {
	LacpPort port;
	PeriodicTxContext context;
	PeriodicTxNoPeriodicState periodicTxState;
	PeriodicTxState stateObj;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		context = new PeriodicTxContext();
		periodicTxState = new PeriodicTxNoPeriodicState();
		stateObj = new PeriodicTxState();
		stateObj.setStateFlag(LacpConst.PERIODIC_STATES.NO_PERIODIC);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.NO_PERIODIC, periodicTxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		periodicTxState.setStateFlag(LacpConst.PERIODIC_STATES.PERIODIC_TX);
		assertEquals(LacpConst.PERIODIC_STATES.PERIODIC_TX, periodicTxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		context.setState(stateObj);
//		port.setPeriodicWhileTimer(20);
//		periodicTxState.executeStateAction(context, port, bpdu);
	}

	@Test
	public void testPeriodicTxNoPeriodicState() throws Exception {
		assertEquals(LacpConst.PERIODIC_STATES.NO_PERIODIC, periodicTxState.getStateFlag());
	}
}
