/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.List;
import java.util.ArrayList;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class LacpSystemTest
{
    private LacpSystem lacpSystem;
    private LacpFlow lacpFlow;
    @MockitoAnnotations.Mock
    private InstanceIdentifier instId;
    @MockitoAnnotations.Mock
    private LacpNodeExtn lacpNode;
    @MockitoAnnotations.Mock
    private InstanceIdentifier instId2;
    @MockitoAnnotations.Mock
    private LacpNodeExtn lacpNode2;
    @MockitoAnnotations.Mock
    private DataBroker dataBroker;
    @MockitoAnnotations.Mock
    private SalFlowService salFlow;

    @Before
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);
        lacpSystem = LacpSystem.getLacpSystem();
        lacpFlow = new LacpFlow();
        LacpNodeExtn.setDataBrokerService(dataBroker);
        
        lacpFlow.setSalFlowService(salFlow);
        lacpFlow.setLacpFlowTableId((short)0);
        lacpFlow.setLacpFlowPriority(50);
        lacpFlow.setLacpFlowIdleTime(0);
        lacpFlow.setLacpFlowHardTime(0);

    }
    @Test
    public void verifyAdd() throws Exception
    {
        lacpSystem.addLacpNode(instId, lacpNode);
        LacpNodeExtn getLacpNode = lacpSystem.getLacpNode(instId);
        assertNotNull(getLacpNode);
    }
    @Test
    public void verifyRemove() throws Exception
    {
        lacpSystem.addLacpNode(instId, lacpNode);
        lacpSystem.removeLacpNode(instId);
        LacpNodeExtn getLacpNode = lacpSystem.getLacpNode(instId);
        assertNull(getLacpNode);
    }
    @Test
    public void readDSNullNode() throws Exception
    {
        List<Node> nodeList = new ArrayList<Node>();
        Nodes nodes = new NodesBuilder().setNode(nodeList).build();
        Optional<Nodes> optionalNodes = Optional.of(nodes);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        lacpSystem.readDataStore(dataBroker);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
    }
    @Test
    public void readDSWithNode() throws Exception
    {
        FlowCapableNodeConnector flowCap1 = new FlowCapableNodeConnectorBuilder().setState(new StateBuilder().setLinkDown(false).build()).build();
        FlowCapableNodeConnector flowCap2 = new FlowCapableNodeConnectorBuilder().setState(new StateBuilder().setLinkDown(true).build()).build();

        NodeConnector nc1 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap1).build();
        NodeConnector nc2 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap2).build();
        List<NodeConnector> nodeConnectors1 = new ArrayList<NodeConnector>();
        nodeConnectors1.add(nc1);
        nodeConnectors1.add(nc2);

        NodeId nodeId1 = new NodeId("1");
        Node node1 = new NodeBuilder().setId(nodeId1)
                        .setKey(new NodeKey(nodeId1))
                        .setNodeConnector(nodeConnectors1).build();
                  
        List<NodeConnector> nodeConnectors2 = new ArrayList<NodeConnector>();
        NodeId nodeId2 = new NodeId("2");
        Node node2 = new NodeBuilder().setId(nodeId2)
                        .setKey(new NodeKey(nodeId2))
                        .setNodeConnector(nodeConnectors2).build();

        List<Node> nodeList = new ArrayList<Node>();
        nodeList.add(node1);
        nodeList.add(node2);
        Nodes nodes = new NodesBuilder().setNode(nodeList).build();
        Optional<Nodes> optionalNodes = Optional.of(nodes);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
       
        CheckedFuture result = Mockito.mock(CheckedFuture.class);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(writeOnlyTransaction.submit()).thenReturn(result);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);
        
        lacpSystem.readDataStore(dataBroker);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
        verify(writeOnlyTransaction, times(3)).submit();
    }
    @Test
    public void verifyLacpPort() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class)
                          .child(Node.class, new NodeKey(new NodeId("n1")))
                          .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        LacpPort port = Mockito.mock(LacpPort.class);
        lacpNode.addLacpPort(ncId, port);
        boolean res = lacpNode.deletePort(ncId, false);
    }
    @Test
    public void verifyNonLacpPort() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class)
                          .child(Node.class, new NodeKey(new NodeId("n1")))
                          .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        
        CheckedFuture result = Mockito.mock(CheckedFuture.class);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(writeOnlyTransaction.submit()).thenReturn(result);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);
        
        lacpNode.addNonLacpPort(ncId);
        boolean res = lacpNode.deletePort(ncId, false);
    }
    @Test
    public void verifyFlowId() throws Exception
    {
        long flowId = 1;
        lacpNode.setFlowId(flowId);
        flowId = lacpNode.getFlowId();
    }
    @Test
    public void verifyBGroupId() throws Exception
    {
        long groupId = 1;
        doNothing().when(lacpNode).updateLacpNodeDS(any(InstanceIdentifier.class));
        lacpNode.updateNodeBcastGroupId(groupId);
        groupId = lacpNode.getNodeBcastGroupId();
    }
    @Test
    public void verifyLacpAgg() throws Exception
    {
        LacpAgg lag = Mockito.mock(LacpAgg.class);
        boolean res = lacpNode.addLacpAggregator(lag);
        res = lacpNode.removeLacpAggregator(lag);
        res = lacpNode.removeLacpAggregator(lag);
        assertEquals(false, res);
    }
}
