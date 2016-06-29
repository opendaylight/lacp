/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
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
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev151125.AggRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev151125.lacpaggregator.LagPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev151125.lacpaggregator.LagPortsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev151125.lacpaggregator.LagPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev151125.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev151125.LacpNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import java.util.concurrent.Future;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import java.util.concurrent.TimeUnit;

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
    @MockitoAnnotations.Mock
    private SalGroupService salGroup;

    @Before
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);
        lacpSystem = LacpSystem.getLacpSystem();
        lacpFlow = new LacpFlow();
        LacpNodeExtn.setDataBrokerService(dataBroker);
        LacpUtil.setDataBrokerService(dataBroker);
        LacpUtil.setSalGroupService(salGroup);

        lacpFlow.setSalFlowService(salFlow);
        lacpFlow.setLacpFlowTableId((short)0);
        lacpFlow.setLacpFlowPriority(50);
        lacpFlow.setLacpFlowIdleTime(0);
        lacpFlow.setLacpFlowHardTime(0);

    }
    @Test
    public void verifyAdd() throws Exception
    {
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId("openflow:1"))).build();
        lacpSystem.addLacpNode(nodeId, lacpNode);
        LacpNodeExtn getLacpNode = lacpSystem.getLacpNode(nodeId);
        assertNotNull(getLacpNode);
    }
    @Test
    public void verifyRemove() throws Exception
    {
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1"))).build();
        lacpSystem.addLacpNode(nodeId, lacpNode);
        LacpNodeExtn lNode = lacpSystem.getLacpNode(nodeId);
        lNode.setLacpNodeDeleteStatus(true);
        lacpSystem.removeLacpNode(nodeId);
        LacpNodeExtn getLacpNode = lacpSystem.getLacpNode(nodeId);
        assertNull(getLacpNode);
    }
    @Test
    public void readDSIncorrectNode() throws Exception {
        Node node = Mockito.mock(Node.class);
        Optional<Node> optionalNodes = Optional.of(node);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("node:10"))).toInstance();

        lacpSystem.readDataStore(nodeId);
        verify(dataBroker, times(1)).newReadOnlyTransaction();
    }
    @Test
    public void readDSWithNodeAndNodeConnector() throws Exception {
        FlowCapableNodeConnector flowCap1 = new FlowCapableNodeConnectorBuilder().setState(new StateBuilder()
                                    .setLinkDown(false).build()).build();
        FlowCapableNodeConnector flowCap2 = new FlowCapableNodeConnectorBuilder().setState(new StateBuilder()
                                    .setLinkDown(true).build()).build();

        NodeConnector nc1 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap1).build();
        NodeConnector nc2 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap2).build();
        List<NodeConnector> nodeConnectors1 = new ArrayList<NodeConnector>();
        nodeConnectors1.add(nc1);
        nodeConnectors1.add(nc2);

        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("openflow:1"))).toInstance();
        NodeId nodeId1 = new NodeId("openflow:1");
        Node node1 = new NodeBuilder().setId(nodeId1)
                        .setKey(new NodeKey(nodeId1))
                        .setNodeConnector(nodeConnectors1).build();
        Optional<Node> optionalNodes = Optional.of(node1);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        CheckedFuture result = Mockito.mock(CheckedFuture.class);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(writeOnlyTransaction.submit()).thenReturn(result);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);
        Future<RpcResult<AddGroupOutput>> output = Mockito.mock(Future.class);
        RpcResult<AddGroupOutput> rpc = Mockito.mock(RpcResult.class);
        when(rpc.isSuccessful()).thenReturn(false);
        when(output.get(any(Long.class), any(TimeUnit.class))).thenReturn(rpc);
        when(salGroup.addGroup(any(AddGroupInput.class))).thenReturn(output);

        lacpSystem.readDataStore(nodeIid);
        verify(dataBroker).newReadOnlyTransaction();
        verify(writeOnlyTransaction, times(2)).submit();
    }
    @Test
    public void readDSWithNode() throws Exception {
        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("openflow:2"))).toInstance();
        List<NodeConnector> nodeConnectors2 = new ArrayList<NodeConnector>();
        NodeId nodeId2 = new NodeId("openflow:2");
        Node node2 = new NodeBuilder().setId(nodeId2)
                        .setKey(new NodeKey(nodeId2))
                        .setNodeConnector(nodeConnectors2).build();

        Optional<Node> optionalNodes = Optional.of(node2);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        CheckedFuture result = Mockito.mock(CheckedFuture.class);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(writeOnlyTransaction.submit()).thenReturn(result);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);
        Future<RpcResult<AddGroupOutput>> output = Mockito.mock(Future.class);
        RpcResult<AddGroupOutput> rpc = Mockito.mock(RpcResult.class);
        when(rpc.isSuccessful()).thenReturn(false);
        when(output.get(any(Long.class), any(TimeUnit.class))).thenReturn(rpc);
        when(salGroup.addGroup(any(AddGroupInput.class))).thenReturn(output);

        lacpSystem.readDataStore(nodeIid);
        verify(dataBroker).newReadOnlyTransaction();
        verify(writeOnlyTransaction, times(0)).submit();
    }
    @Test
    public void readDSWithLacpNode() throws Exception {
        FlowCapableNodeConnector flowCap1 = new FlowCapableNodeConnectorBuilder()
                .setPortNumber(new PortNumberUni(Long.valueOf(1)))
                .setState(new StateBuilder().setLinkDown(false).build()).build();

        NodeConnector nc1 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap1).build();
        List<NodeConnector> nodeConnectors1 = new ArrayList<NodeConnector>();
        nodeConnectors1.add(nc1);

        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("openflow:1"))).toInstance();
        NodeId nodeId1 = new NodeId("openflow:1");
        LacpNode lNode = new LacpNodeBuilder().setSystemId(new MacAddress("00:01:02:03:04:05"))
            .setSystemPriority(32768).setNonLagGroupid(Long.valueOf(12342))
            .setLacpAggregators(new ArrayList<LacpAggregators>()).build();
        Node node1 = new NodeBuilder().setId(nodeId1)
                        .setKey(new NodeKey(nodeId1))
                        .setNodeConnector(nodeConnectors1)
                        .addAugmentation(LacpNode.class, lNode).build();
        Optional<Node> optionalNodes = Optional.of(node1);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);

        lacpSystem.readDataStore(nodeIid);
        verify(dataBroker).newReadOnlyTransaction();
        verify(writeOnlyTransaction, times(0)).submit();
    }
    @Test
    public void readDSWithLagInterface() throws Exception {
        FlowCapableNodeConnector flowCap1 = new FlowCapableNodeConnectorBuilder()
                            .setCurrentFeature(new PortFeatures(true, true, false, true, false, false,
                                false, false, false, false, false, false, false, false, false, false))
                            .setPortNumber(new PortNumberUni(Long.valueOf(1)))
                            .setState(new StateBuilder().setLinkDown(false).build()).build();
        NodeId nId = new NodeId("openflow:1");
        InstanceIdentifier<NodeConnector> id = InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey (nId)).child (NodeConnector.class,
                                    new NodeConnectorKey (new NodeConnectorId("openflow:1:5001")))
                                .toInstance();
        InstanceIdentifier aggId = InstanceIdentifier.builder(Nodes.class)
                        .child (Node.class, new NodeKey (nId)).augmentation(LacpNode.class)
                        .child (LacpAggregators.class, new LacpAggregatorsKey(1)).toInstance();
        LacpNodeConnector lPort = new LacpNodeConnectorBuilder().setPartnerPortNumber(Short.valueOf((short)5))
                                    .setActorPortNumber(Short.valueOf((short)4)).setActorPortPriority(Integer.valueOf(32768))
                                    .setLacpAggRef(new AggRef(aggId))
                                    .setLogicalNodeconnectorRef(new NodeConnectorRef(id))
                                    .setPartnerPortPriority(Integer.valueOf(32768))
                                    .setPeriodicTime(Integer.valueOf(30)).build();
        NodeConnector nc1 = new NodeConnectorBuilder()
              .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
              .addAugmentation(FlowCapableNodeConnector.class, flowCap1)
              .addAugmentation(LacpNodeConnector.class, lPort).build();
        List<NodeConnector> nodeConnectors1 = new ArrayList<NodeConnector>();
        nodeConnectors1.add(nc1);

        InstanceIdentifier<NodeConnector> portId = InstanceIdentifier.builder(Nodes.class)
                                .child(Node.class, new NodeKey (nId)).child (NodeConnector.class,
                                new NodeConnectorKey (new NodeConnectorId("1")))
                                .toInstance();
        LagPorts lagPort = new LagPortsBuilder().setKey(new LagPortsKey(Long.valueOf((long)1)))
                .setLagPortId(Long.valueOf((long)1))
                .setLagPortRef(new NodeConnectorRef(portId)).build();
        List <LagPorts> lagPortList = new ArrayList<LagPorts>();
        lagPortList.add(lagPort);
        LacpAggregators lag = new LacpAggregatorsBuilder().setAggId(Integer.valueOf(1))
                        .setLagGroupid(Long.valueOf(2341)).setActorAggMacAddress(new MacAddress("00:01:02:03:04:06"))
                        .setActorOperAggKey(Integer.valueOf(123))
                        .setPartnerSystemId(new MacAddress("00:01:06:02:04:01"))
                        .setPartnerSystemPriority(Integer.valueOf(32678))
                        .setLagPorts(lagPortList)
                        .setPartnerOperAggKey(Integer.valueOf(234)).build();
        List<LacpAggregators> aggList = new ArrayList<LacpAggregators>();
        aggList.add(lag);

        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("openflow:1"))).toInstance();
        NodeId nodeId1 = new NodeId("openflow:1");
        LacpNode lNode = new LacpNodeBuilder().setSystemId(new MacAddress("00:01:02:03:04:05"))
            .setSystemPriority(32768).setNonLagGroupid(Long.valueOf(12342))
            .setLacpAggregators(aggList).build();
        Node node1 = new NodeBuilder().setId(nodeId1)
                        .setKey(new NodeKey(nodeId1))
                        .setNodeConnector(nodeConnectors1)
                        .addAugmentation(LacpNode.class, lNode).build();
        Optional<Node> optionalNodes = Optional.of(node1);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);

        lacpSystem.readDataStore(nodeIid);
        verify(dataBroker).newReadOnlyTransaction();
        verify(writeOnlyTransaction, times(0)).submit();
    }

    @Test
    public void verifyLacpPort() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class)
                          .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                          .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        LacpPort port = Mockito.mock(LacpPort.class);
        lacpNode.addLacpPort(ncId, port);
        boolean res = lacpNode.deletePort(ncId, false);
    }
    @Test
    public void verifyNonLacpPort() throws Exception
    {
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class)
                          .child(Node.class, new NodeKey(new NodeId("oepnflow:1")))
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
        LacpBond lag = Mockito.mock(LacpBond.class);
        boolean res = lacpNode.addLacpAggregator(lag);
        res = lacpNode.removeLacpAggregator(lag);
        res = lacpNode.removeLacpAggregator(lag);
        assertEquals(false, res);
    }
}
