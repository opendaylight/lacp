/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.inventorylistener;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpNodeDataListenerTest {

	private LacpDataListener dataListener;
	private LacpNodeListener nodeListener;
    @MockitoAnnotations.Mock
    private DataBroker dataBroker;
    private LacpSystem lacpSys;
    
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		lacpSys = LacpSystem.getLacpSystem();
		LacpNodeListener.setLacpSystem(lacpSys);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        Topology topology = new TopologyBuilder().setLink(null).build();
        Optional<Topology> optional = Optional.of(topology);
        when(checkedFuture.get()).thenReturn(optional);
        dataListener = new LacpDataListener(dataBroker);
        nodeListener = LacpNodeListener.getNodeListenerInstance();
		
	}
	
	
	@Test
	public void testAddRemoveNodeNodeconnector()throws Exception{
		testOnDataChanged();
	}
    public void testOnDataChanged() throws Exception
    {	
    	
        NodeConnectorId ncId1 = new NodeConnectorId("Openflow_Con:1");
        NodeConnectorId ncId2 = new NodeConnectorId("LOCAL_Con:2");
        NodeConnectorKey nck1 = new NodeConnectorKey(ncId1);
        NodeConnectorKey nck2 = new NodeConnectorKey(ncId2);

        
		FlowCapableNodeConnector fnc1 = mock(FlowCapableNodeConnector.class);
		PortNumberUni fr = new PortNumberUni((long) 51);
		when(fnc1.getPortNumber()).thenReturn(fr);
		State portState = Mockito.mock(State.class);
		when(fnc1.getState()).thenReturn(portState);
		
		FlowCapableNodeConnector fnc2 = mock(FlowCapableNodeConnector.class);
		PortNumberUni fr2 = new PortNumberUni((long) 52);
		when(fnc2.getPortNumber()).thenReturn(fr2);
		State portState2 = Mockito.mock(State.class);
		when(fnc2.getState()).thenReturn(portState2);
		
		
		NodeConnector nodeC1 = new NodeConnectorBuilder().setId(ncId1)
        		.setKey(nck1)
        		.addAugmentation(FlowCapableNodeConnector.class, fnc1).build();
        NodeConnector nodeC2 = new NodeConnectorBuilder().setId(ncId2)
        		.setKey(nck2)
        		.addAugmentation(FlowCapableNodeConnector.class, fnc2).build();
        
        List<NodeConnector> nodeConnectors = new ArrayList<NodeConnector>();
        nodeConnectors.add(nodeC1);
		NodeId nId1 = new NodeId("openflow:11");
		NodeId nId2 = new NodeId("openflow:22");
		NodeKey nk1 = new NodeKey(nId1);
		NodeKey nk2 = new NodeKey(nId2);
        Node node1 = new NodeBuilder().setId(nId1)
        		.setKey(nk1)
        		.setNodeConnector(nodeConnectors).build();
        Node node2 = new NodeBuilder().setId(nId2)
        		.setKey(nk2).build();
        InstanceIdentifier<Node> nIId1 = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, nk1).build();
        InstanceIdentifier<Node> nIId2 = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, nk2).build();
        
        
        InstanceIdentifier<NodeConnector> ncIId1 = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, nk1)
        		.child(NodeConnector.class, nck1).build();
        InstanceIdentifier<NodeConnector> ncIId2 = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, nk2)
        		.child(NodeConnector.class, nck2).build();
        
        
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChange = Mockito.mock(AsyncDataChangeEvent.class);
        
        //Updating Node 
        Map<InstanceIdentifier<?>, DataObject> updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        
        updatedData.put(nIId1, node1);
        updatedData.put(nIId2, node2);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getUpdatedData()).thenReturn(updatedData);
        when(dataChange.getRemovedPaths()).thenReturn(null);
        when(dataChange.getOriginalData()).thenReturn(null);
        dataListener.onDataChanged(dataChange);
        
        //Updating NodeConnector
        Map<InstanceIdentifier<?>, DataObject> updatedDataNC = new HashMap<InstanceIdentifier<?>, DataObject>();
        
        updatedDataNC.put(ncIId1, nodeC1);
        updatedDataNC.put(ncIId2, nodeC2);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getUpdatedData()).thenReturn(updatedDataNC);
        when(dataChange.getRemovedPaths()).thenReturn(null);
        when(dataChange.getOriginalData()).thenReturn(null);
        dataListener.onDataChanged(dataChange);

        testOnNodeConnectorUpdated(ncIId1);
        testOnNodeConnectorUpdated(ncIId2);
        
        //Removing NodeConnector
        Map<InstanceIdentifier<?>, DataObject> originalNC = new HashMap<InstanceIdentifier<?>, DataObject>();
        originalNC.put(ncIId1, nodeC1);
        originalNC.put(ncIId2, nodeC2);
        Set<InstanceIdentifier<?>> removeNC = new HashSet<InstanceIdentifier<?>>();
        removeNC.add(ncIId1);
        removeNC.add(ncIId2);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getUpdatedData()).thenReturn(null);
        when(dataChange.getRemovedPaths()).thenReturn(removeNC);
        when(dataChange.getOriginalData()).thenReturn(originalNC);
        dataListener.onDataChanged(dataChange);
        

        //Removing a node
        Map<InstanceIdentifier<?>, DataObject> original = new HashMap<InstanceIdentifier<?>, DataObject>();
        original.put(nIId1, node1);
        original.put(nIId2, node2);
        Set<InstanceIdentifier<?>> remove = new HashSet<InstanceIdentifier<?>>();
        remove.add(nIId1);
        remove.add(nIId2);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getUpdatedData()).thenReturn(null);
        when(dataChange.getRemovedPaths()).thenReturn(remove);
        when(dataChange.getOriginalData()).thenReturn(original);
        dataListener.onDataChanged(dataChange);
    }

	private void testOnNodeConnectorUpdated(InstanceIdentifier<NodeConnector> nodeId) {	
		NodeConnectorRef ncRef = new NodeConnectorRef(nodeId);
		FlowCapableNodeConnectorUpdated augmentation = Mockito.mock(FlowCapableNodeConnectorUpdated.class);
		PortNumberUni fr = new PortNumberUni((long) 55);
		when(augmentation.getPortNumber()).thenReturn(fr);
		State portState = Mockito.mock(State.class);
		when(augmentation.getState()).thenReturn(portState);
		NodeConnectorUpdated nodeConnectorUpdated = new NodeConnectorUpdatedBuilder()
				.addAugmentation(FlowCapableNodeConnectorUpdated.class, augmentation)
				.setNodeConnectorRef(ncRef).build();
		
		
		nodeListener.onNodeConnectorUpdated(nodeConnectorUpdated);
	}
	
	@Test
	public void DummyFunctions(){
		nodeListener.onNodeConnectorRemoved(null);
		nodeListener.onNodeRemoved(null);
		nodeListener.onNodeUpdated(null);
	}

}
