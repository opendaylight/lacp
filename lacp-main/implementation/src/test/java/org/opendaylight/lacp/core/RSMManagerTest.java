/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RSMManagerTest {

	private RSMManager rsmManager ;
	private LacpNodeExtn lacpNode;
	int id;

	@Before
	public void setUp() throws Exception {
		rsmManager = RSMManager.getRSMManagerInstance();
		
		id=0;
		NodeId nId = new NodeId("openflow:"+(id+1));
		NodeConnectorId ncId = new NodeConnectorId(""+(id+1));
		InstanceIdentifier<NodeConnector> nodeId = InstanceIdentifier.builder(Nodes.class)
        		.child (Node.class, new NodeKey (nId))
        		.child(NodeConnector.class, new NodeConnectorKey(ncId)).build();
		//nodeId.firstKeyOf(Node.class, NodeId.class);
		lacpNode = new LacpNodeExtn(nodeId);
	}

	@Test
	public void testCreate_WakeUp_DeleteRSM() {
		assertTrue(rsmManager.createRSM(lacpNode));
		assertFalse(rsmManager.createRSM(lacpNode));
		
		//Matches the switchId. But returns null as the LacpList in RSMThread in empty
		assertNull(rsmManager.getLacpPortFromBond(lacpNode.getSwitchId(), (short)5));
		
		assertFalse(rsmManager.wakeupRSM(Mockito.mock(LacpNodeExtn.class)));
		assertTrue(rsmManager.wakeupRSM(lacpNode));

		assertFalse(rsmManager.deleteRSM(Mockito.mock(LacpNodeExtn.class)));
		assertTrue(rsmManager.deleteRSM(lacpNode));
		assertFalse(rsmManager.deleteRSM(lacpNode));
	}

	@Test
	public void testGetLacpPortFromBond() {
		//No Matches for switchId hence returns null
		assertNull(rsmManager.getLacpPortFromBond((long)3, (short)5));
	}

}
