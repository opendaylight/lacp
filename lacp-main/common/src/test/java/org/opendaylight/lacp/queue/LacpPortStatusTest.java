/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpQueue;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;

public class LacpPortStatusTest {
	LacpPortStatus portStatus;
	InstanceIdentifier<NodeConnector> nodeConnectorId;
        LacpQueue<LacpPDUPortStatusContainer> lacpQ;
        LacpPDUPortStatusContainer obj1, obj2, obj3, obj4;

        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
		nodeConnectorId = InstanceIdentifier.builder( Nodes.class ).child( Node.class, new NodeKey( new NodeId("openflow:1"))).child( NodeConnector.class, new NodeConnectorKey( new NodeConnectorId("openflow:1:3" ))).build();
		portStatus = new LacpPortStatus(1L, 20, 1, nodeConnectorId, true);
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testgetSwID() throws Exception {
                assertTrue(portStatus.getSwID() == 1L);
        }


        @Test
        public void testgetPortID() throws Exception {
                assertTrue(portStatus.getPortID() == 20);
        }


        @Test
        public void testgetPortStatus() throws Exception {
                assertTrue(portStatus.getPortStatus() == 1);
        }


        @Test
        public void testgetPortResetStatus() throws Exception {
                assertTrue(portStatus.getPortResetStatus() == true);
        }

        @Test
        public void testgetNodeConnectorInstanceId() throws Exception {
                assertTrue(portStatus.getNodeConnectorInstanceId() == nodeConnectorId);
        }


        @Test
        public void testgetgetMessageType() throws Exception {
                assertTrue(portStatus.getMessageType() == LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG);
        }

}
