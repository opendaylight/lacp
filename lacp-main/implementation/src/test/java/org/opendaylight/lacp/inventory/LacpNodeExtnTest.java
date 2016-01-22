/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.escape.Escapers.Builder;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;

public class LacpNodeExtnTest 
{
	public LacpNodeExtn lacpNode;
	public LacpNodeExtn lacpNode1;
	
	short key = (short)1001;
	short portId = (short)80;
	
	private NodeId nId;
	InstanceIdentifier<Node> nodeId;
	private InstanceIdentifier<NodeConnector> ncId;
	private List<InstanceIdentifier<NodeConnector>> nonLacpPortList;
	private LacpBond lacpBond;
	private LacpPort lacpPort;
	
	@MockitoAnnotations.Mock
	private DataBroker dataBroker;
	
	@MockitoAnnotations.Mock
    private DataBroker dataService;
	
	 @MockitoAnnotations.Mock
	 private WriteTransaction write;
	 
	 @MockitoAnnotations.Mock
	 private LacpUtil lacpUtil;
		
	@MockitoAnnotations.Mock
	private NotificationProviderService notify;
	
	@MockitoAnnotations.Mock
    private SalGroupService salGroup;
	
	@MockitoAnnotations.Mock
	private SalFlowService salFlow;
		
	@Before
	public void initMocks() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		nId = new NodeId("openflow:1");
		nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nId)).build();
		lacpNode = new LacpNodeExtn(nodeId);
		lacpBond = LacpBond.newInstance(key, lacpNode);
		nonLacpPortList = new ArrayList<InstanceIdentifier<NodeConnector>>();
		
		
		Future<RpcResult<AddGroupOutput>> output = Mockito.mock(Future.class);
        	RpcResult<AddGroupOutput> rpc = Mockito.mock(RpcResult.class);
	        when(rpc.isSuccessful()).thenReturn(false);
	        when(output.get(any(Long.class), any(TimeUnit.class))).thenReturn(rpc);
	        when(salGroup.addGroup(any(AddGroupInput.class))).thenReturn(output);
        

	        Future<RpcResult<UpdateGroupOutput>> Up_output = Mockito.mock(Future.class);
        	RpcResult<UpdateGroupOutput> Up_rpc = Mockito.mock(RpcResult.class);
	        when(Up_rpc.isSuccessful()).thenReturn(false);
	        when(Up_output.get(any(Long.class), any(TimeUnit.class))).thenReturn(Up_rpc);
	        when(salGroup.updateGroup(any(UpdateGroupInput.class))).thenReturn(Up_output);
        
	        Future<RpcResult<RemoveGroupOutput>> Rm_output = Mockito.mock(Future.class);
        	RpcResult<RemoveGroupOutput> Rm_rpc = Mockito.mock(RpcResult.class);
	        when(Rm_rpc.isSuccessful()).thenReturn(false);
        	when(Rm_output.get(any(Long.class), any(TimeUnit.class))).thenReturn(Rm_rpc);
	        when(salGroup.removeGroup(any(RemoveGroupInput.class))).thenReturn(Rm_output);
        
       
        
        	LacpUtil.setSalGroupService(salGroup);
        
        
		NodeConnector nc = mock(NodeConnector.class);
		FlowCapableNodeConnector fnc = mock(FlowCapableNodeConnector.class);
		PortFeatures pf = new PortFeatures(false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false);
		when(fnc.getCurrentFeature()).thenReturn(pf);
		when(nc.getAugmentation(FlowCapableNodeConnector.class)).thenReturn(fnc);
	        Optional<NodeConnector> optionalNodes = Optional.of(nc);
        
	        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        	CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
	        when(checkedFuture.get()).thenReturn(optionalNodes);
	        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
	        when(dataService.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        

		LacpNodeExtn.setDataBrokerService(dataService);
	        LacpUtil.setDataBrokerService(dataService);
        
        
        	LacpBpduInfo bpduInfo = mock(LacpBpduInfo.class);
 		InstanceIdentifier<NodeConnector> iNc = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class,new NodeKey(new NodeId("Openflow:2")))
        		.child(NodeConnector.class,new NodeConnectorKey(new NodeConnectorId("NodeCon:2"))).build();
        	when(bpduInfo.getNCRef()).thenReturn(new NodeConnectorRef(iNc));
        
       		lacpPort = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		
		ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
		
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 		
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));
	
	}
	
	@Test 
	public void updateLacpNodeInfoTest() throws Exception
	{
		Future<RpcResult<AddFlowOutput>> add_flow_output = Mockito.mock(Future.class);
 	    	RpcResult<AddFlowOutput> add_flow_rpc = Mockito.mock(RpcResult.class);
		when(add_flow_rpc.isSuccessful()).thenReturn(false);
	        when(add_flow_output.get(any(Long.class), any(TimeUnit.class))).thenReturn(add_flow_rpc);
	        when(salFlow.addFlow(any(AddFlowInput.class))).thenReturn(add_flow_output);
		
		Future<RpcResult<RemoveFlowOutput>> Rm_flow_output = Mockito.mock(Future.class);
	        RpcResult<RemoveFlowOutput> Rm_flow_rpc = Mockito.mock(RpcResult.class);
	        when(Rm_flow_rpc.isSuccessful()).thenReturn(false);
	    	when(Rm_flow_output.get(any(Long.class), any(TimeUnit.class))).thenReturn(Rm_flow_rpc);
	    	when(salFlow.removeFlow(any(RemoveFlowInput.class))).thenReturn(Rm_flow_output);
		
		new LacpFlow().setSalFlowService(salFlow);

		lacpNode.updateLacpNodeInfo();
		verify(salFlow).addFlow(any(AddFlowInput.class));
		
	}
	
	@Test
	public void addLacpAggregatorTest()
	{
		assertEquals(true, lacpNode.addLacpAggregator(lacpBond));
		assertEquals(false, lacpNode.addLacpAggregator(lacpBond));
	}
	
	@Test
	public void removeLacpAggregatorTest()
	{
		assertEquals(false, lacpNode.removeLacpAggregator(lacpBond));
		lacpNode.addLacpAggregator(lacpBond);
		assertEquals(true, lacpNode.removeLacpAggregator(lacpBond));
				
	}
	
	@Test
	public void updateNodeConnectorStatusInfoTest()
	{
		lacpNode.updateNodeConnectorLacpInfo(nodeId);
	}
	
	@Test
	public void addNonLacpPortTest()
	{
		assertEquals(true, lacpNode.addNonLacpPort(ncId));
		assertNotNull(lacpNode.containsPort(ncId));
		assertEquals(false, lacpNode.addNonLacpPort(ncId));	
	}	
	
	@Test
	public void removeNonLacpPortTest()
	{
		assertEquals(false, lacpNode.removeNonLacpPort(ncId));
		lacpNode.addNonLacpPort(ncId);
		assertEquals(true, lacpNode.removeNonLacpPort(ncId));
	}
	
	@Test
	public void addLacpPortTest()
	{
		assertEquals(true, lacpNode.addLacpPort(ncId, lacpPort));
		assertNotNull(lacpNode.containsPort(ncId));
		assertEquals(false, lacpNode.addLacpPort(ncId, lacpPort));		
	}
	
	@Test
	public void removeLacpPortTest()
	{
		ncId = lacpPort.getNodeConnectorId();
		lacpNode.addLacpPort(ncId, lacpPort);
		lacpPort = lacpNode.removeLacpPort(ncId, true);
		assertNotNull(lacpPort);
		assertEquals(null, lacpNode.removeLacpPort(ncId, true));
		
		lacpNode.addLacpPort(ncId, lacpPort);
		lacpPort = lacpNode.removeLacpPort(ncId, false);
		assertNotNull(lacpPort);
		assertEquals(null, lacpNode.removeLacpPort(ncId, true));
	}
	
	@Test
	public void deleteLacpNodeTest() throws Exception
	{
		Future<RpcResult<RemoveFlowOutput>> Rm_flow_output = Mockito.mock(Future.class);
		RpcResult<RemoveFlowOutput> Rm_flow_rpc = Mockito.mock(RpcResult.class);
		when(Rm_flow_rpc.isSuccessful()).thenReturn(false);
		when(Rm_flow_output.get(any(Long.class), any(TimeUnit.class))).thenReturn(Rm_flow_rpc);
		when(salFlow.removeFlow(any(RemoveFlowInput.class))).thenReturn(Rm_flow_output);
		
		new LacpFlow().setSalFlowService(salFlow);
		
		Long flowId = (long)1;
		lacpNode.setFlowId(flowId);
		lacpNode.setLacpNodeDeleteStatus(false);
		lacpNode.addLacpPort(ncId, lacpPort);
		lacpNode.deleteLacpNode();
		
		verify(salFlow).removeFlow(any(RemoveFlowInput.class));
	}
	
	@Test
	public void deletePortTest()
	{			
		assertEquals(false, lacpNode.deletePort(ncId, true));
		assertEquals(false, lacpNode.deletePort(ncId, false));
		ncId = lacpPort.getNodeConnectorId();
		lacpNode.addLacpPort(ncId, lacpPort);
		assertEquals(true, lacpNode.deletePort(ncId, true));
		
		lacpNode.addLacpPort(ncId, lacpPort);
		assertEquals(true, lacpNode.deletePort(ncId, false));		
	}
	
	@Test
	public void updateNodeBcastGroupIdTest()
	{
		Long groupId = (long)1;
		lacpNode.updateNodeBcastGroupId(groupId);
		lacpNode.getNodeBcastGroupId();
	}
	
	@Test
	public void flowIdTest()
	{
		Long flowId = (long)1;
		lacpNode.setFlowId(flowId);
		lacpNode.getFlowId();
	}
	
	@Test
	public void getValueTest()
	{
		lacpNode.getSwitchId();
		lacpNode.getLacpPort(ncId);
	}

	@Test
	public void getLacpPortForPortIdTest() throws Exception {
		assertNull(lacpNode.getLacpPortForPortId((short)5));
	}
}
