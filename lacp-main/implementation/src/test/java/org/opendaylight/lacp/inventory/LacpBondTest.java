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
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
import org.opendaylight.lacp.Utils.LacpPortProperties;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LagId;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpBondTest 
{
	public LacpBond lacpBond;
	
	int a=3;
	int b=0;
	short key = (short)1001;
	int sysPri = 1;
	byte[] sysid = { (byte) 00, (byte)00, (byte) 00, (byte) 01 };
	long swId = (long)1;
	short portId = (short)80;
	short portId1 = (short)90;
	short portId2 = (short)60;
	short portId3 = (short)50;
	int portPri = 1;
	Date date;
	
	private LacpAggregator lag;
    private List<LacpAggregator>  aggregatorList;
    private List<LacpPort> slaveList;
    private List<LacpPort> activePortList;
    private List<LacpPort> lagPortList;
    private LinkedHashMap<Short, LacpPort> portSlaveMap;
   	private LinkedHashMap<Long, Short> systemIdMap;
    private LinkedHashMap<Long, Short> maplist;
	private LacpPort lacpPort;
	private LacpPort slave;
	private LacpPort slave1;
	private LacpPort slave2;
	private LacpBpduInfo bpduInfo;
	private LacpFlow LACPFLOW = new LacpFlow();
	private LacpNodeExtn lacpNode; 
	private LacpNodeExtn lacpNodeRef; 
	private NodeId nId;
	InstanceIdentifier<Node> nodeId;
	private InstanceIdentifier ncId;
	
	@MockitoAnnotations.Mock
	private LacpPortProperties lacpPortProperties;
	
    
    @MockitoAnnotations.Mock
    private DataBroker dataService;
    
    @MockitoAnnotations.Mock
    private WriteTransaction write;
    
	@MockitoAnnotations.Mock
	private LacpUtil lacpUtil;
	
	@MockitoAnnotations.Mock
	private NotificationProviderService notify;
	
	@MockitoAnnotations.Mock
    private SalFlowService salFlow;
	
    @MockitoAnnotations.Mock
    private SalGroupService salGroup;
    
    @MockitoAnnotations.Mock
    private LacpGroupTbl lacpGroupTbl;
	
	
	@Before
	public void initMocks() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		date = new Date(2015,1,1);
		aggregatorList=new ArrayList<LacpAggregator>();
		slaveList=new ArrayList<LacpPort>();
		lagPortList=new ArrayList<LacpPort>();
		portSlaveMap =new LinkedHashMap<Short, LacpPort>();
		systemIdMap =new LinkedHashMap<Long, Short>();
		lag = LacpAggregator.newInstance();
		


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
        
        nId = new NodeId("openflow:1");
		nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nId)).build();
		lacpNode = new LacpNodeExtn(nodeId);
		lacpNodeRef = new LacpNodeExtn(nodeId);
		lacpBond = LacpBond.newInstance(key, lacpNode);
		lacpPort = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		slave = LacpPort.newInstance(0, portId1, lacpBond, 1,  bpduInfo);
		slave1 = LacpPort.newInstance(0, portId2, lacpBond, 1,  bpduInfo);
		slave2 = LacpPort.newInstance(0, portId3, lacpBond, 1,  bpduInfo);
		ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
		LacpBond lacpBond_1 = LacpBond.newInstance(key, lacpNode);
		LacpBond lacpBond_2 = LacpBond.newInstance(1, key, lacpNode);
        
    	lacpBond.setSlaveList(slaveList);
    	lacpBond.setPortSlaveMap(portSlaveMap);
    	lacpBond.bondSetSysPriority(sysPri);
    	lacpBond.setSystemIdMap(systemIdMap);
    	lacpBond.setAggregatorList(aggregatorList);
    	lacpBond.setActiveSince(date);
    	lacpBond.setMinLinks(a); 
    	lacpBond.setAdminKey(key);
    	lacpBond.setDirty(true);
    	lacpBond.setBondId(1);
    	lacpBond.bondSetMaxLink(10);
       	lacpBond.setLacpFast(1);	
    }
	
	@Test
	public void verifyFunction()
	{
		assertNotNull(lacpBond.getBondSystemId());
		assertEquals(slaveList,lacpBond.getSlaveList());
		assertEquals(aggregatorList,lacpBond.getAggregatorList());
		assertEquals(portSlaveMap,lacpBond.getPortSlaveMap());
		assertEquals(sysPri,lacpBond.bondGetSysPriority());
		assertEquals(systemIdMap,lacpBond.getSystemIdMap());
		assertEquals(date,lacpBond.getActiveSince());
		assertEquals(3,lacpBond.getMinLinks());
		assertEquals(key, lacpBond.getAdminKey());
		assertEquals(true, lacpBond.isDirty());
		assertEquals(10, lacpBond.bondGetMaxLink());
		assertEquals(1, lacpBond.getBondId());
	}
	
	
	@Test
	public void toStringTest()
	{
		portSlaveMap = lacpBond.getPortSlaveMap();
		portSlaveMap.clear();
		lacpBond.toString();
		portSlaveMap.put((short)1, lacpPort);
		assertNotNull(lacpBond.toString());
	}
	
	@Test
	public void getFreeAggregatorTest()
	{
		assertNull(lacpBond.bondGetFreeAgg());
		aggregatorList = lacpBond.getAggregatorList();
		aggregatorList.add(lag);
		assertNotNull(lacpBond.bondGetFreeAgg());
	}
		
	@Test
	public void getActiveAggTest()
	{
		lag.setIsActive((short)0);
		aggregatorList = lacpBond.getAggregatorList();
		assertEquals(null,lacpBond.bondGetFreeAgg());
		lag.setIsActive((short)1);		
		aggregatorList.add(lag);
		assertEquals(lag,lacpBond.getActiveAgg());
	}
	
	@Test
	public void findLacpAggByFitPortTest()
	{

		byte[] actorSystem = {0x10,0,0,0,0,0x11};
		lacpPort.setActorSystem(actorSystem);
		byte[] system={0x10,0,0,0,0,0x12};
		lacpPort.portPartnerOperSetSystem(system);
		aggregatorList = lacpBond.getAggregatorList();
		assertNull(lacpBond.findLacpAggByFitPort(lacpPort));
		lag.setAggId((short)1);
		
		lag.setIsActive((short)1);
		
		LagId aggLagId = Mockito.mock(LagId.class);
		lag.setAggLagId(aggLagId);
		aggregatorList.add(lag);
		lacpPort.portSetLagId();
		when(aggLagId.compareToPartial(any(LagId.class))).thenReturn(0);
		lacpPort.portGetLagId().isNeighborFound();
		lag.setIsIndiv(false);
		lag.addPortToAgg(lacpPort);
		
		assertNotNull(lacpBond.findLacpAggByFitPort(lacpPort));
		assertEquals(lag, lacpBond.findLacpAggByFitPort(lacpPort));
	}
	
	@Test
	public void bondUpdateSystemPriorityTest()
	{
		int priority = 5;
		int a;
		slaveList = lacpBond.getSlaveList();
		slaveList.add(lacpPort);
		
		lacpBond.bondUpdateSystemPriority(priority);
		assertEquals(priority, lacpBond.bondGetSysPriority());
		
		lacpBond.bondUpdateSystemPriority(priority);		
		assertEquals(priority, lacpBond.bondGetSysPriority());
	}
	
	@Test
	public void bondUpdateLacpRateTest()
	{
		slaveList = lacpBond.getSlaveList();
		slaveList.add(lacpPort);
		lacpBond.bondUpdateLacpRate();
	}
	
	@Test
	public void bondAggSelectionLogicTest()
	{
		byte status;
		short n =0;
		boolean a,c;
		
		/*setting status not null*/	
		/*Only 1 port in portList*/
		long swId = (long)1;
		long swId1 = (long)2;
		long swId2 = (long)3;
		long swId3 = (long)4;
		portSlaveMap.put(portId, lacpPort);
		lacpBond.bondUpdateLinkUpSlave(swId, portId);
		status = lacpPort.portGetPortStatus();
		lag.setBond(lacpBond);
		aggregatorList = lacpBond.getAggregatorList();
		lag.setIsActive((short) 1);
		aggregatorList.add(lag);		
		lacpPort.setActorOperPortState((byte)0x10);
		lag.aggSetActorOperAggKey((short)1);
		lagPortList.add(lacpPort);
		n++;
		lagPortList.add(slave);
		n++;
		lag.setLagPortList(lagPortList);
		lag.setNumOfPorts(n);
		
		
		short b = lag.getNumOfPorts();
		a = (lag.aggDevUp());
		c = (lag != null);
		lacpBond.bondAggSelectionLogic();
		
		/*Many ports in port List*/
		
		portSlaveMap.put(portId, lacpPort);
		portSlaveMap.put(portId1, slave);
		portSlaveMap.put(portId2, slave1);
		portSlaveMap.put(portId3, slave2);
		lacpBond.bondUpdateLinkUpSlave(swId, portId);
		lacpBond.bondUpdateLinkUpSlave(swId1, portId1);
		lacpBond.bondUpdateLinkUpSlave(swId2, portId2);
		lacpBond.bondUpdateLinkUpSlave(swId3, portId3);
		status = lacpPort.portGetPortStatus();
		lag.setBond(lacpBond);
		aggregatorList = lacpBond.getAggregatorList();
		lag.setIsActive((short) 1);
		aggregatorList.add(lag);		
		lacpPort.setActorOperPortState((byte)0x10);
		lag.aggSetActorOperAggKey((short)1);
		lagPortList.add(lacpPort);
		lagPortList.add(slave);
		lagPortList.add(slave1);
		lagPortList.add(slave2);
		lag.setLagPortList(lagPortList);
		lag.setNumOfPorts(n);
		
		lacpBond.bondAggSelectionLogic();
	}
	
	@Test
	public void isLacpEnabledTest()
	{
		assertFalse(lacpBond.isLacpEnabled());
	}
	
	@Test
	public void setLacpEnabledTest()
	{
		lacpBond.setLacpEnabled(true);
		assertTrue(lacpBond.isLacpEnabled());
	}
	
	@Test
	public void getSlavePortObjectTest()
	{
		portSlaveMap = lacpBond.getPortSlaveMap();
		portSlaveMap.put(portId, lacpPort);
		assertEquals(lacpPort, lacpBond.getSlavePortObject(portId));
	}
	
	@Test
	public void newInstanceTest()
	{
		LacpBond lacpBond1;
		lacpBond1 = LacpBond.newInstance((short)1, lacpNode);
		assertNotEquals(lacpBond,lacpBond1);
	}
	
	@Test
	public void checkAggSelectTimeTest()
	{
		lacpBond.checkAggSelectTimer();
	}
	
	@Test
	public void isPartnerExistTest()
	{
		short key = (short)1;
		slaveList = lacpBond.getSlaveList();
		lacpPort.portPartnerOperSetKey(key);
		lacpPort.portPartnerOperSetSystem(sysid);
		slaveList.add(lacpPort);
		assertEquals(false, lacpBond.isPartnerExist(sysid, key));
	}
	
	@Test
	public void findPortIdByPartnerMacPortIdTest()
	{
		short a = lacpBond.findPortIdByPartnerMacPortId(sysid, portId);
		slaveList = lacpBond.getSlaveList();
		lacpPort.portPartnerOperSetPortNumber(portId);
		lacpPort.portPartnerOperSetSystem(sysid);
		sysid = lacpPort.portPartnerOperGetSystem();
		slaveList.add(lacpPort);
		short pId = lacpPort.slaveGetPortId();
		short b = lacpBond.findPortIdByPartnerMacPortId(sysid, portId);
		assertEquals(b, pId);
	} 
	
	@Test
	public void bondUpdateLinkUpDownSlaveTest()
	{
		long swId = lacpPort.slaveGetSwId();
		byte status;
		portSlaveMap.put(portId, slave);
		
		lacpBond.bondUpdateLinkUpSlave(swId, portId);
		//Port status is 0 when bond is up 
		assertEquals(LacpConst.BOND_LINK_UP, slave.portGetPortStatus());
		
		lacpBond.bondUpdateLinkDownSlave(swId, portId);
		//Port status is 0 when bond is up 
		assertEquals(LacpConst.BOND_LINK_DOWN, lacpPort.portGetPortStatus());
	}
	
	@Test
	public void bondDelMembersFrSwTest()
	{
		portSlaveMap = lacpBond.getPortSlaveMap();
		long swId = lacpPort.slaveGetSwId();
		systemIdMap = lacpBond.getSystemIdMap();
		slaveList = lacpBond.getSlaveList();
		slaveList.add(lacpPort);
		portSlaveMap.put(portId, lacpPort);
		systemIdMap.put(swId, (short)1);
		lacpNode.setLacpNodeDeleteStatus(true);
		lacpBond.bondDelMembersFrSw(swId);
		assertTrue(slaveList.isEmpty());
	}
	
	@Test
	public void bondGetAggIdTest()
	{
		long swId = slave.slaveGetSwId();
		short portId = slave.slaveGetPortId();
		slave.setPortAggregator(lag);
		short aggId = (short)101;
		lag.setAggId(aggId);
		portSlaveMap.put(portId, slave);
		assertEquals(aggId, lacpBond.bondGetAggId(swId, portId));
		
		slaveList.add(slave);
		systemIdMap.put(swId, (short)1);
		Set<Short> result = new TreeSet<Short>();
		result = lacpBond.bondGetAggId(swId);
		assertEquals(true, result.contains(aggId));
	}
	
	@Test
	public void bondHasMemberTest()
	{
		long swId = (long)1;
		assertEquals(false, lacpBond.bondHasMember());
		slaveList.add(lacpPort);
		assertEquals(true, lacpBond.bondHasMember());	
		
		assertEquals(false,lacpBond.bondHasMember(swId));
		systemIdMap.put(swId,(short)1);
		assertEquals(true,lacpBond.bondHasMember(swId));
	}
	
	@Test
	public void bondNumMembersInSwTest()
	{	
		long swId = lacpPort.slaveGetSwId();
		long swId_1 = (long)1;
		
		systemIdMap = lacpBond.getSystemIdMap();
		systemIdMap.put(swId, (short)1);
		
		slaveList.add(lacpPort);
		
		assertEquals(1, lacpBond.bondNumMembersInSw(swId));	
		assertEquals(0, lacpBond.bondNumMembersInSw(swId_1));	
	}
	
	@Test
	public void getBondActiveAggIdTest()
	{
		lag.setAggId((short)1);
		lag.setIsActive((short)1);
		List<LacpAggregator> laglist = new ArrayList<LacpAggregator>();
		laglist.add(lag);
		lacpBond.setAggregatorList(laglist);
		assertEquals((short)1,lacpBond.getBondActiveAggId());
	}
	
	@Test
	public void bondAddSlaveTest()
	{
		swId = slave.slaveGetSwId();
		portId = slave.slaveGetPortId();
		portPri = slave.getPortPriority();
		bpduInfo = slave.getLacpBpduInfo();
		systemIdMap = lacpBond.getSystemIdMap();
		
		systemIdMap.put((long)10, (short)1);
		lacpBond.bondAddSlave(swId, portId, portPri, bpduInfo);
		
		systemIdMap.put(swId, (short)1);
		lacpBond.bondAddSlave(swId, portId, portPri, bpduInfo);
		
		slaveList = lacpBond.getSlaveList();
		assertEquals(2, slaveList.size());
	}
	
	@Test
	public void bondDelSlaveTest()
	{
		long swId = slave.slaveGetSwId();
		short portId = (short)80;
		long swId1 = (long)1;
		short portId1 = (short)80;
		systemIdMap = lacpBond.getSystemIdMap();
		portSlaveMap = lacpBond.getPortSlaveMap();
		systemIdMap.put(swId, (short)1);
		portSlaveMap.put(portId, lacpPort);
		lacpNode.setLacpNodeDeleteStatus(true);
		lacpBond.bondDelSlave(swId,portId);
		lacpBond.bondDelSlave(swId1, portId1);
		
		slaveList = lacpBond.getSlaveList();
		assertFalse(slaveList.contains(slave));
	}
	
	@Test
	public void addActivePortTest()
	{
		LacpPort.setDataBrokerService(dataService);
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));	
		
		aggregatorList = lacpBond.getAggregatorList();
		lag.setIsActive((short) 1);
		byte[] mac = {0x10,0x11,0x11,0x10,0x11,0x11};
		lag.setAggMacAddress(mac);
		lag.setActorOperAggKey((short)1);
		lag.setPartnerOperAggKey((short)2);
		lag.setPartnerSystem(sysid);
		int pri = 1;
		lag.setPartnerSystemPriority(pri);
		aggregatorList.add(lag);
		activePortList = lacpBond.getActivePortList();
				
		 // Add a new Port    
		 assertEquals(true, lacpBond.addActivePort(lacpPort));
		 		 		 
		 activePortList.add(slave);
		 // Add already existing port
		 assertEquals(false, lacpBond.addActivePort(slave));
		 lacpBond.addActivePort(slave1);
		 verify(write, times(4)).submit();
	}
	
	@Test
	public void removeActivePortTest()			
	{
		LacpPort.setDataBrokerService(dataService);
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 		
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));
		
		activePortList = lacpBond.getActivePortList();
		boolean a,b;
	
		NodeConnectorRef ncRef = new NodeConnectorRef(ncId);
		bpduInfo = Mockito.mock(LacpBpduInfo.class);
		when(bpduInfo.getNCRef()).thenReturn(ncRef);
	
		//Removing a non existing port
		assertEquals(false, lacpBond.removeActivePort(lacpPort));
		activePortList.add(lacpPort);
		
		//Removing an existing port
		assertEquals(true, lacpBond.removeActivePort(lacpPort));
		activePortList.add(lacpPort);
		activePortList.add(slave);
		assertEquals(true, lacpBond.removeActivePort(lacpPort));
		verify(write, times(4)).submit();
		
	} 
	
	
	@Test
	public void lacpBondCleanupTest()
	{
		lacpBond.lacpBondCleanup();
		assertEquals(0,lacpBond.getSlaveCnt());
		slaveList = lacpBond.getSlaveList();
		slaveList.add(lacpPort);
		lacpBond.setSlaveCnt((short)1);
		lacpBond.lacpBondCleanup();
		assertEquals(0,lacpBond.getSlaveCnt());
	}
}

