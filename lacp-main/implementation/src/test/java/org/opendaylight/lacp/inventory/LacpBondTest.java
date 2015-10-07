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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
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
	private LacpBpduInfo bpduInfo;
	private LacpFlow LACPFLOW = new LacpFlow();
	private LacpNodeExtn lacpNode; 
	private LacpNodeExtn lacpNodeRef; 
	private NodeId nId;
	InstanceIdentifier<Node> nodeId;
	private InstanceIdentifier ncId;
	
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
    private SalFlowService salFlow;
	
    @MockitoAnnotations.Mock
    private SalGroupService salGroup;
	
	
	@Before
	public void initMocks() throws InterruptedException, ExecutionException
	{
		MockitoAnnotations.initMocks(this);
		date = new Date(2015,1,1);
		aggregatorList=new ArrayList<LacpAggregator>();
		slaveList=new ArrayList<LacpPort>();
		lagPortList=new ArrayList<LacpPort>();
		portSlaveMap =new LinkedHashMap<Short, LacpPort>();
		systemIdMap =new LinkedHashMap<Long, Short>();
		lag = LacpAggregator.newInstance();
		
		LacpNodeExtn.setDataBrokerService(dataBroker);
        LacpUtil.setDataBrokerService(dataBroker);
        
        Augmentation<NodeConnector> aug = new Augmentation<NodeConnector>() {
		};
		
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
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        
        
        LacpBpduInfo bpduInfo = mock(LacpBpduInfo.class);
 		InstanceIdentifier<NodeConnector> iNc = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class).child(NodeConnector.class).build();
        when(bpduInfo.getNCRef()).thenReturn(new NodeConnectorRef(iNc));
        
        nId = new NodeId("openflow:1");
		nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nId)).build();
		lacpNode = new LacpNodeExtn(nodeId);
		lacpNodeRef = new LacpNodeExtn(nodeId);
		lacpBond = LacpBond.newInstance(key, lacpNode);
		lacpPort = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		slave = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		slave1 = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();

        
    		lacpBond.setSlaveList(slaveList);
    		lacpBond.setPortSlaveMap(portSlaveMap);
    		lacpBond.setSysPriority(sysPri);
    		lacpBond.setSystemIdMap(systemIdMap);
    		lacpBond.setAggregatorList(aggregatorList);
    		lacpBond.setActiveSince(date);
    		lacpBond.setMinLinks(a); 
    		lacpBond.setVirtualSysMacAddr(sysid);
    		lacpBond.setAdminKey(key);
    		lacpBond.setDirty(true);
    		lacpBond.setFailed(true);
    		lacpBond.bondSetMaxLink(10);
	}
	
	
	@Test
	public void verify()
	{
		assertNotNull(lacpBond.getBondSystemId());
		assertEquals(slaveList,lacpBond.getSlaveList());
		assertEquals(aggregatorList,lacpBond.getAggregatorList());
		assertEquals(portSlaveMap,lacpBond.getPortSlaveMap());
		assertEquals(sysPri,lacpBond.getSysPriority());
		assertEquals(systemIdMap,lacpBond.getSystemIdMap());
		assertEquals(date,lacpBond.getActiveSince());
		assertEquals(3,lacpBond.getMinLinks());
		assertNotNull(lacpBond.getVirtualSysMacAddr());
		assertEquals(key, lacpBond.getAdminKey());
		assertEquals(true, lacpBond.isDirty());
		assertEquals(true, lacpBond.isFailed());
		assertEquals(10, lacpBond.bondGetMaxLink());
	}
	
	
	@Test
	public void toStringTest()
	{
		lacpBond.toString();
	}
	
	@Test
	public void getFreeAggregatorTest()
	{
		if(aggregatorList == null)
		{
			assertEquals(null,lacpBond.bondGetFreeAgg());
		}
		if(aggregatorList.size() == 0)
		{
			assertEquals(null,lacpBond.bondGetFreeAgg());
		}
		if(lag.getNumOfPorts() == 0)
		{
			assertEquals(null,lacpBond.bondGetFreeAgg());
		}
	}
	
	@Test
	public void getActiveAggTest()
	{
		if(aggregatorList == null)
		{
			assertEquals(null,lacpBond.bondGetFreeAgg());
		}
		if(aggregatorList.size() == 0)
		{
			assertEquals(null,lacpBond.bondGetFreeAgg());
		}
		if (lag.getIsActive() > 0)
		{
			assertEquals(lag,lacpBond.getActiveAgg());
		}
		
	}
	
	@Test
	public void findLacpAggByFitPortTest()
	{
		aggregatorList = lacpBond.getAggregatorList();
		lacpBond.findLacpAggByFitPort(lacpPort);
		lag.addPortToAgg(lacpPort);
		lag.setIsActive((short)1);
		aggregatorList.add(lag);
		lacpPort.portSetLagId();
		lacpBond.findLacpAggByFitPort(lacpPort);
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
		long swId = (long)1;
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
	}
	
	@Test
	public void isLacpEnabledTest()
	{
		assertEquals(false,lacpBond.isLacpEnabled());
	}
	
	@Test
	public void setLacpEnabledTest()
	{
		lacpBond.setLacpEnabled(true);
		assertEquals(true,lacpBond.isLacpEnabled());
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
		short portId = (short)80;
		slaveList = lacpBond.getSlaveList();
		lacpPort.portPartnerOperSetPortNumber(portId);
		lacpPort.portPartnerOperSetSystem(sysid);
		slaveList.add(lacpPort);
		lacpBond.findPortIdByPartnerMacPortId(sysid, portId);
	} 
	
	@Test
	public void getVirtualPortIdTest()
	{
		short portNumber = (short)1;
		long swId = (long)1;
		short systemId;
		systemIdMap.put(swId,(short)1);
		systemId = systemIdMap.get(swId);
		short res = (short) ((portNumber & 0x0fff) | (systemId << 12 & 0xf000));
		assertEquals(res,lacpBond.getVirtualPortId(swId, portNumber));
	}
	
	@Test
	public void bondUpdateLinkUpDownSlaveTest()
	{
		long swId = (long)1;
		byte status;
		portSlaveMap.put(portId, slave);
		lacpBond.bondUpdateLinkUpSlave(swId, portId);
		assertEquals(0,slave.portGetPortStatus());
		lacpBond.bondUpdateLinkDownSlave(swId, portId);
		status  = slave.portGetPortStatus();
	}
	
	@Test
	public void bondDelMembersFrSwTest()
	{
		long swId = (long)1;
		lacpBond.bondDelMembersFrSw(swId);
		assertEquals(false,lacpBond.bondHasMember(swId));
	}
	
	@Test
	public void bondGetAggIdTest()
	{
		long swId = slave.slaveGetSwId();
		short portId = slave.getLacpPortId();
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
		slaveList.add(lacpPort);
		assertEquals(false,lacpBond.bondHasMember());	
		systemIdMap.put(swId,(short)1);
		assertEquals(true,lacpBond.bondHasMember(swId));
	}
	
	@Test
	public void bondNumMembersInSwTest()
	{	
		long swId = (long)1;
		lacpBond.getSystemIdMap();
		systemIdMap.put(swId, (short)1);
		slaveList.add(lacpPort);
		lacpBond.bondNumMembersInSw(swId);
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
		portId = slave.getLacpPortId();
		portPri = slave.getPortPriority();
		bpduInfo = slave.getLacpBpduInfo();
		lacpBond.bondAddSlave(swId, portId, portPri, bpduInfo);
		slaveList = lacpBond.getSlaveList();
		lacpBond.bondAddSlave(swId, portId, portPri, bpduInfo);
		systemIdMap = lacpBond.getSystemIdMap();
		systemIdMap.put(swId, (short)1);
		lacpBond.bondAddSlave(swId, portId, portPri, bpduInfo);
	}
	
	@Test
	public void bondDelSlaveTest()
	{
		long swId = (long)1;
		short portId = (short)80;
		long swId1 = (long)1;
		short portId1 = (short)80;
		systemIdMap = lacpBond.getSystemIdMap();
		portSlaveMap = lacpBond.getPortSlaveMap();
		systemIdMap.put(swId, (short)1);
		portSlaveMap.put(portId, lacpPort);
		lacpBond.bondDelSlave(swId,portId);
		lacpBond.bondDelSlave(swId1, portId1);
	}
	
	@Test
	public void addActivePortTest()
	{
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		
		LacpPort.setDataBrokerService(dataService);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 
		
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));
		
		activePortList = lacpBond.getActivePortList();
		NodeConnectorRef ncRef = new NodeConnectorRef(ncId);
		bpduInfo = Mockito.mock(LacpBpduInfo.class);
		when(bpduInfo.getNCRef()).thenReturn(ncRef);
				
		 activePortList.add(lacpPort);
		 Future<RpcResult<AddGroupOutput>> output = Mockito.mock(Future.class);
		 when(salGroup.addGroup(any(AddGroupInput.class))).thenReturn(output);
		 assertEquals(false, lacpBond.addActivePort(lacpPort));
	}
	
	@Test
	public void removeActivePortTest()			
	{
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		LacpPort.setDataBrokerService(dataService);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 
		
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));	
		activePortList = lacpBond.getActivePortList();
		boolean a,b;
	
		NodeConnectorRef ncRef = new NodeConnectorRef(ncId);
		bpduInfo = Mockito.mock(LacpBpduInfo.class);
		when(bpduInfo.getNCRef()).thenReturn(ncRef);
		
		assertEquals(false, lacpBond.removeActivePort(lacpPort));
		activePortList.add(lacpPort);
	} 
	
	@Test
	public void updateLacpAggregatorDSTest()
	{
		lacpUtil.setDataBrokerService(dataService);
		LacpPort.setDataBrokerService(dataService);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));	
		lacpBond.updateLacpAggregatorsDS();
	}
	
	@Test
	public void deleteLacpAggregatorDSTest()
	{
		lacpUtil.setDataBrokerService(dataService);
		LacpPort.setDataBrokerService(dataService);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));	
		lacpBond.deleteLacpAggregatorDS(ncId);
	}
	
	@Test
	public void lacpBondCleanupTest()
	{
		lacpBond.lacpBondCleanup();
		assertEquals(0,lacpBond.getSlaveCnt());
	}
}
