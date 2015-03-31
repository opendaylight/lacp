/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventorylistener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventorylistener.LacpNodeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;

public class LacpNodeListenerTest {
    private LacpNodeListener listener;
    @MockitoAnnotations.Mock 
    private LacpSystem lacpSystem;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        listener = new LacpNodeListener(lacpSystem);
    }
    @Test
    public void onNodeUpdatedNull() throws Exception
    {
        listener.onNodeUpdated(null);
        verify(lacpSystem, times(0)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeUpdatedValid() throws Exception
    {
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId("1"))).build();
        NodeUpdated nodeUpdated = new NodeUpdatedBuilder().setNodeRef(new NodeRef(nodeId)).build();
        listener.onNodeUpdated(nodeUpdated);
        Thread.sleep(2500);
        verify(lacpSystem, times(1)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeRemovedNull() throws Exception
    {
        listener.onNodeRemoved(null);
        verify(lacpSystem, times(0)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeRemovedValid() throws Exception
    {
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId("1"))).build();
        NodeRemoved nodeRemoved = new NodeRemovedBuilder().setNodeRef(new NodeRef(nodeId)).build();
        listener.onNodeRemoved(nodeRemoved);
        Thread.sleep(250);
        verify(lacpSystem, times(1)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeConnectorUpdatedNull() throws Exception
    {
        listener.onNodeConnectorUpdated(null);
        verify(lacpSystem, times(0)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeConnectorUpdatedValid() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                          .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        FlowCapableNodeConnectorUpdated flowCap = new FlowCapableNodeConnectorUpdatedBuilder().setState(new StateBuilder().setLinkDown(false).build()).build();
        NodeConnectorUpdated nodeConnectorUpdated = new NodeConnectorUpdatedBuilder().setNodeConnectorRef(new NodeConnectorRef(ncId))
                                                        .addAugmentation(FlowCapableNodeConnectorUpdated.class, flowCap).build();
        listener.onNodeConnectorUpdated(nodeConnectorUpdated);
        Thread.sleep(250);
        verify(lacpSystem, times(1)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeConnectorRemovedNull() throws Exception
    {
        listener.onNodeConnectorRemoved(null);
        verify(lacpSystem, times(0)).getLacpNode(any(InstanceIdentifier.class));
    }
    @Test
    public void onNodeConnectorRemovedValid() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                          .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        NodeConnectorRemoved nodeConnectorRemoved = new NodeConnectorRemovedBuilder().setNodeConnectorRef(new NodeConnectorRef(ncId)).build();
        listener.onNodeConnectorRemoved(nodeConnectorRemoved);
        Thread.sleep(250);
        verify(lacpSystem, times(1)).getLacpNode(any(InstanceIdentifier.class));
    }
}

/*
 * @BeforeClass
 * @AfterClass 
 *  - public static without args
 *  @Before
 *  @After
 *   - public without args
 *  @Test
 */
