/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.grouptbl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.Groups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpGroupTblTest {

	LacpGroupTbl lacpGroupTbl;
    @MockitoAnnotations.Mock
	SalGroupService salGroupService;
    @MockitoAnnotations.Mock
	DataBroker dataService;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        SalService();
		lacpGroupTbl = new LacpGroupTbl(salGroupService, dataService);
	}

	@After
	public void tearDown() throws Exception {
	}

	private void SalService() throws Exception{
		 CheckedFuture result = Mockito.mock(CheckedFuture.class);
	        WriteTransaction writeOnlyTransaction = Mockito.mock(WriteTransaction.class);
	        when(writeOnlyTransaction.submit()).thenReturn(result);
	        when(dataService.newWriteOnlyTransaction()).thenReturn(writeOnlyTransaction);
	        
	        Future<RpcResult<AddGroupOutput>> Add_output = Mockito.mock(Future.class);
	        RpcResult<AddGroupOutput> Add_rpc = Mockito.mock(RpcResult.class);
	        when(Add_rpc.isSuccessful()).thenReturn(true);
	        when(Add_output.get(any(Long.class), any(TimeUnit.class))).thenThrow(ExecutionException.class).thenReturn(Add_rpc);
	        when(salGroupService.addGroup(any(AddGroupInput.class))).thenReturn(Add_output);
	        

	        Future<RpcResult<UpdateGroupOutput>> Update_output = Mockito.mock(Future.class);
	        RpcResult<UpdateGroupOutput> Update_rpc = Mockito.mock(RpcResult.class);
	        when(Update_rpc.isSuccessful()).thenReturn(false).thenReturn(true);
	        when(Update_output.get(any(Long.class), any(TimeUnit.class))).thenThrow(ExecutionException.class).thenReturn(Update_rpc);
	        when(salGroupService.updateGroup(any(UpdateGroupInput.class))).thenReturn(Update_output);

	        Future<RpcResult<RemoveGroupOutput>> Remove_output = Mockito.mock(Future.class);
	        RpcResult<RemoveGroupOutput> Remove_rpc = Mockito.mock(RpcResult.class);
	        when(Remove_rpc.isSuccessful()).thenReturn(false).thenReturn(true);
	        when(Remove_output.get(any(Long.class), any(TimeUnit.class))).thenThrow(ExecutionException.class).thenReturn(Remove_rpc);
	        when(salGroupService.removeGroup(any(RemoveGroupInput.class))).thenReturn(Remove_output);
	}
	
	@Test
	public void testLacp__Add_Update_Remove__Group_Port() {
        InstanceIdentifier<NodeConnector> nodeId = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, new NodeKey(new NodeId("openflow:1")))
        		.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("OpenFlow_Con:1"))).build();
		NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(nodeId);
		Boolean isUnicastGrp = true;
		GroupId groupId = new GroupId(LacpUtil.getNextGroupId());
		assertNotNull(lacpGroupTbl);
		assertNull(lacpGroupTbl.lacpAddGroup(isUnicastGrp, null, groupId));
		Group origGroup = lacpGroupTbl.lacpAddGroup(isUnicastGrp, nodeConnectorRef, groupId);
		
		
		lacpGroupTbl.lacpAddPort(isUnicastGrp, nodeConnectorRef, origGroup);
		lacpGroupTbl.lacpRemPort(origGroup, nodeConnectorRef, isUnicastGrp);
		
		
		
		InstanceIdentifier<NodeConnector> nodeId1 = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, new NodeKey(new NodeId("openflow:2")))
        		.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("OpenFlow_Con:2"))).build();
		NodeConnectorRef nodeConnectorRef1 = new NodeConnectorRef(nodeId);
		
		lacpGroupTbl.lacpAddPort(isUnicastGrp, nodeConnectorRef, origGroup);
		lacpGroupTbl.lacpRemPort(origGroup, nodeConnectorRef1, isUnicastGrp);
		
		
		lacpGroupTbl.lacpRemGroup(isUnicastGrp, nodeConnectorRef, groupId);
		lacpGroupTbl.lacpRemGroup(isUnicastGrp, nodeConnectorRef, groupId);
		lacpGroupTbl.lacpRemGroup(isUnicastGrp, nodeConnectorRef, groupId);
	}
	
	@Test
	public void testGetGroup() throws Exception {
		 CheckedFuture result = Mockito.mock(CheckedFuture.class);

	        GroupKey groupkey = new GroupKey(new GroupId((long)7837));
			Group group = new GroupBuilder().setKey(groupkey).build();
			Optional<Group> optionalGroups = Optional.of(group);
	        when(result.get()).thenReturn(optionalGroups);
	        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
	        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(result);
	        when(dataService.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
	        assertEquals(group, lacpGroupTbl.getGroup(groupkey, new NodeKey(new NodeId("NodeId:1"))));
	}


}
