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

public class MuxAttachedStateTest {
	LacpPort port;
	MuxContext context;
	MuxAttachedState muxState;
	MuxState stateObj;
	MuxState stateObj1;

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
		muxState = new MuxAttachedState();
		stateObj = new MuxState();
		stateObj.setStateFlag(LacpConst.MUX_STATES.MUX_ATTACHED);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStateFlag() throws Exception {
		assertEquals(LacpConst.MUX_STATES.MUX_ATTACHED, muxState.getStateFlag());
	}

	@Test
	public void testSetStateFlag() throws Exception {
		muxState.setStateFlag(LacpConst.MUX_STATES.MUX_WAITING);
		assertEquals(LacpConst.MUX_STATES.MUX_WAITING, muxState.getStateFlag());
	}

	@Test
	public void testExecuteStateAction() throws Exception {
		context.setState(stateObj);
		muxState.executeStateAction(context, port);
	}

	@Test
	public void testMuxAttachedState() throws Exception {
		assertEquals(LacpConst.MUX_STATES.MUX_ATTACHED, muxState.getStateFlag());
	}

}
