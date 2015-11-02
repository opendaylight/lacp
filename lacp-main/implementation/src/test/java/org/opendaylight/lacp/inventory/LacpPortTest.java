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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpBpduSysInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort.PortParams;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.timer.TimerExpiryMessage;
import org.opendaylight.lacp.timer.Utils;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnector;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpPortTest {
	
	public LacpPort lacpPort;
	LacpBond bond;
    @MockitoAnnotations.Mock
    private DataBroker dataBroker;
    LacpBpduInfo bpduInfo;

   @Before
    public void initMocks() throws InterruptedException, ExecutionException
    {
        MockitoAnnotations.initMocks(this);
        LacpNodeExtn.setDataBrokerService(dataBroker);
        LacpUtil.setDataBrokerService(dataBroker);
        
        Augmentation<NodeConnector> aug = new Augmentation<NodeConnector>() {
		}; 
        //NodeConnector nc = new NodeConnectorBuilder()
        		//.addAugmentation(LacpNodeConnector.class, aug).build();
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
        
        
        // TODO Cannot able to initialize NodeConnectorRef field in bpduinfo class
        bpduInfo = mock(LacpBpduInfo.class);
        InstanceIdentifier<NodeConnector> iNc = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class).child(NodeConnector.class).build();
        when(bpduInfo.getNCRef()).thenReturn(new NodeConnectorRef(iNc));
        //-----
        NodeId nId = new NodeId("openflow:1");
		InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
        		.child (Node.class, new NodeKey (nId)).build();
		//nodeId.firstKeyOf(Node.class, NodeId.class);
        LacpNodeExtn lacpNode = new LacpNodeExtn(nodeId);
        Long swid = LacpUtil.getNodeSwitchId(nodeId);
        bond = LacpBond.newInstance((short) 1001, lacpNode); 
        //System.out.println(LacpSystem.getLacpSystem().addLacpNode(nodeId, lacpNode));
        lacpPort = LacpPort.newInstance(swid, (short)80, bond, 1,  bpduInfo);
        //System.out.println(LacpSystem.getLacpSystem().getLacpNode(swid));
        //System.out.println(LacpSystem.getLacpSystem().getLacpNode(nodeId));
    }

	@Test
	public void testHashCode() {
		lacpPort.hashCode();
	}
	@Test
	public void testCheckAggSelectionTimer() {
		assertEquals(bond.checkAggSelectTimer(),lacpPort.checkAggSelectionTimer());
		lacpPort.slaveSetBond(null);
		assertEquals(0,lacpPort.checkAggSelectionTimer());
	}


	@Test
	public void testSetIsLacpEnabled() {
		boolean enabled = !lacpPort.isLacpEnabled();
		lacpPort.setIsLacpEnabled(enabled);
		assertEquals(enabled, lacpPort.isLacpEnabled());
		enabled= !enabled;
		lacpPort.setIsLacpEnabled(enabled);
		assertEquals(enabled, lacpPort.isLacpEnabled());
	}


	@Test
	public void testGetDuplex() {
		byte dup = 0xf;
		lacpPort.setDuplex(dup);
		assertEquals((byte) 0 , lacpPort.get_Duplex());
		lacpPort.setLink(LacpConst.BOND_LINK_UP);
		assertEquals(dup, lacpPort.get_Duplex());
	}

	@Test
	public void testCompareTo() {
		assertEquals(0,lacpPort.compareTo(lacpPort));
		LacpPort port = Mockito.mock(LacpPort.class);
		assertEquals(-1,lacpPort.compareTo(port));
		assertEquals(1,port.compareTo(lacpPort));
	}

	@Test
	public void testEqualsObject() {
		LacpPort port = Mockito.mock(LacpPort.class);

        
		assertTrue(lacpPort.equals(lacpPort));
		assertFalse(lacpPort.equals(null));
		assertFalse(lacpPort.equals(bond));
		
		when(port.getInstanceId()).thenReturn((short)1234);
		assertFalse(lacpPort.equals(port));
		
		when(port.getInstanceId()).thenReturn(lacpPort.getInstanceId());
		assertFalse(lacpPort.equals(port));
		
		when(port.getActorAdminPortKey()).thenReturn(lacpPort.getActorAdminPortKey());
		assertFalse(lacpPort.equals(port));

        lacpPort = LacpPort.newInstance(5, (short)0, bond, 1,  bpduInfo);
		when(port.getInstanceId()).thenReturn(lacpPort.getInstanceId());
		when(port.getActorAdminPortKey()).thenReturn(lacpPort.getActorAdminPortKey());
		assertFalse(lacpPort.equals(port));
		
        lacpPort = LacpPort.newInstance(0, (short)0, bond, 1,  bpduInfo);
		when(port.getInstanceId()).thenReturn(lacpPort.getInstanceId());
		when(port.getActorAdminPortKey()).thenReturn(lacpPort.getActorAdminPortKey());
		assertTrue(lacpPort.equals(port));
		
		
		
	}

	@Test
	public void testToString() {
		lacpPort.toString();
	}


	@Test
	public void testGetStmStateString() {
		//TODO
		//Always repeats BEG as seq points to BEG and never being incremented. (used as switch cond)
		//Think it should be i in switch cond
		System.out.println(lacpPort.getStmStateString((short)0x2));
	}

	@Test
	public void testGetSpeedString() {
		assertEquals("10M",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_10MBPS));
		assertEquals("100M",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_100MBPS));
		assertEquals("1G",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_1000MBPS));
		assertEquals("10G",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_10000MBPS));
		assertEquals("40G",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_40000MBPS));
		assertEquals("100G",lacpPort.getSpeedString(LacpConst.LINK_SPEED_BITMASK_100000MBPS));
	}
	@Test
	public void testGet_SetPartnerAdmin() {

		byte[] system={0x1,0x1,0x11,0x12,0x13,0x14};
		short key = 0x11;
		int systemPriority = 5;
		short portNum = 80;
		int portPriority =3;
		short portState = 2;
		lacpPort.setPartnerAdmin(system, key, systemPriority, portNum, portPriority, portState);
		PortParams admin = lacpPort.getPartnerAdmin();
		byte[] Asys = admin.getSystem();
		for(int i=0; i<system.length;i++ ){
			assertEquals(system[i],Asys[i]);			
		}
		assertEquals(key,admin.getKey());
		assertEquals(systemPriority,admin.getSystemPriority());
		assertEquals(portNum,admin.getPortNumber());
		assertEquals(portPriority,admin.getPortPriority());
		assertEquals(portState,admin.getPortState());
	}

	@Test
	public void testGet_SetPartnerOper() {
		byte[] system={0,0,0x1,0x2,0x3,0x4};
		short key = 0x10;
		int systemPriority = 2;
		short portNum = 20;
		int portPriority =1;
		short portState = 1;
		lacpPort.setPartnerOper(system, key, systemPriority, portNum, portPriority, portState);
		PortParams partner = lacpPort.getPartnerOper();
		byte[] Psys = partner.getSystem();
		for(int i=0; i<system.length;i++ ){
			assertEquals(system[i],Psys[i]);			
		}
		assertEquals(key,partner.getKey());
		assertEquals(systemPriority,partner.getSystemPriority());
		assertEquals(portNum,partner.getPortNumber());
		assertEquals(portPriority,partner.getPortPriority());
		assertEquals(portState,partner.getPortState());
	}


	@Test
	public void testLacpInitPort() {
		lacpPort.setIsLacpEnabled(true);
		lacpPort.setActorAdminPortKey((short) 15);
		LacpPort port = LacpPort.newInstance(lacpPort.getLacpSwId(), (short)80, lacpPort.slaveGetBond(), lacpPort.getActorPortPriority(), lacpPort.getLacpBpduInfo());
		port.setInstanceId(lacpPort.getInstanceId());
		port.setActorAdminPortKey(lacpPort.getActorAdminPortKey());
		assertTrue(lacpPort.equals(port));
		lacpPort.lacpInitPort(1);
		assertFalse(lacpPort.equals(port));
	}


	@Test
	public void testSetPortsReady() {
		//TODO Diff b/w PORT_READY and PORT_READY_N
		lacpPort.setPortsReady(1);
		assertEquals(true,(lacpPort.getStateMachineBitSet() & LacpConst.PORT_READY)>0);
		//assertEquals(1,lacpPort.getPortsReady());
		lacpPort.setPortsReady(0);
		assertEquals(true,(lacpPort.getStateMachineBitSet() & LacpConst.PORT_READY) == 0);
		//assertEquals(0,lacpPort.getPortsReady());
		
		//------
		assertEquals(0,lacpPort.getPortsReady());
		lacpPort.setStateMachineBitSet(LacpConst.PORT_READY_N);
		assertEquals(1,lacpPort.getPortsReady());
	}

	@Test
	public void testGetLinkSpeed() {
		int speed = 85;
		lacpPort.setSpeed(speed);
		lacpPort.setLink(LacpConst.BOND_LINK_DOWN);
		assertEquals(0,lacpPort.getLinkSpeed());
		lacpPort.setLink(LacpConst.BOND_LINK_UP);
		assertEquals(speed,lacpPort.getLinkSpeed());
	}

	@Test
	public void testUpdateLacpFromPortToLacpPacketPdu() {
		LacpPacketPdu pdu = lacpPort.updateLacpFromPortToLacpPacketPdu();
		assertNotNull(pdu);
		assertNotNull(pdu.getActorInfo());
		assertNotNull(pdu.getCollectorInfoLen());
		assertNotNull(pdu.getCollectorMaxDelay());
		assertNotNull(pdu.getCollectorReserved());
		assertNotNull(pdu.getCollectorReserved());
		assertNotNull(pdu.getCollectorTlvType());
		assertNotNull(pdu.getDestAddress());
		assertNotNull(pdu.getFCS());
		assertNotNull(pdu.getImplementedInterface());
		assertNotNull(pdu.getIngressPort());
		assertNotNull(pdu.getLenType());
		assertNotNull(pdu.getPartnerInfo());
		assertEquals(lacpPort.getSwitchHardwareAddress(),pdu.getSrcAddress());
		assertNotNull(pdu.getSubtype());
		assertNotNull(pdu.getTerminatorInfoLen());
		assertNotNull(pdu.getTerminatorReserved());
		assertNotNull(pdu.getTerminatorTlvType());
		assertNotNull(pdu.getVersion());
	}


	@Test
	public void testIsPortAttDist() {
		lacpPort.setActorOperPortState(LacpConst.PORT_STATE_COLLECTING);
		assertEquals(true,lacpPort.isPortAttDist());
		lacpPort.setActorOperPortState(LacpConst.PORT_STATE_DISTRIBUTING);
		assertEquals(true,lacpPort.isPortAttDist());
		lacpPort.setActorOperPortState((byte)0x0);
		assertEquals(false,lacpPort.isPortAttDist());
	}

	@Test
	public void testIsPortSelected() {
		lacpPort.setStateMachineBitSet((short)0x111);
		assertEquals(true,lacpPort.isPortSelected());
		lacpPort.setStateMachineBitSet((short)0xff);
		assertEquals(false,lacpPort.isPortSelected());
	}


	@Test
	public void testSlaveSetLacpPortEnabled() {
		lacpPort.slaveSetLacpPortEnabled(false);
		assertEquals(false,lacpPort.isLacpEnabled());
		lacpPort.slaveSetLacpPortEnabled(true);
		assertEquals(true,lacpPort.isLacpEnabled());
	}


	@Test
	public void testLacpDisablePort() {
		LacpAggregator agg = Mockito.mock(LacpAggregator.class);
		LacpBond Mbond = Mockito.mock(LacpBond.class);
		when(agg.aggHasPort(lacpPort)).thenReturn(true);
		when(agg.getNumOfPorts()).thenReturn((short)1);
		when(agg.getIsActive()).thenReturn((short)1);
		doNothing().when(agg).rmPortFromAgg(lacpPort);
		lacpPort.portSetAggregator(agg);
		doNothing().when(Mbond).bondAggSelectionLogic();
		lacpPort.slaveSetBond(Mbond);
		lacpPort.lacpDisablePort();
		assertFalse(lacpPort.isInitialized);
	}

	@Test
	public void testSlavePortPriorityChange() {
		int pri = lacpPort.getActorPortPriority();
		lacpPort.slavePortPriorityChange(pri + 2);
		assertEquals(pri + 2, lacpPort.getActorPortPriority());
		lacpPort.slavePortPriorityChange(lacpPort.getActorPortPriority());
		assertEquals(pri + 2, lacpPort.getActorPortPriority());
	}

	@Test
	public void testSlaveSystemPriorityChange() {
		int pri = lacpPort.getActorSystemPriority();
		lacpPort.slaveSystemPriorityChange(pri + 2);
		assertEquals(pri + 2, lacpPort.getActorSystemPriority());
		lacpPort.slaveSystemPriorityChange(lacpPort.getActorSystemPriority());
		assertEquals(pri + 2, lacpPort.getActorSystemPriority());
	}


	@Test
	public void testSlaveRxLacpBpduReceived() {
		lacpPort.setLink((byte)1);
		assertEquals(LacpConst.RX_HANDLER_DROPPED,lacpPort.slaveRxLacpBpduReceived(bpduInfo));
		lacpPort.setLink(LacpConst.BOND_LINK_UP);
		assertEquals(LacpConst.RX_HANDLER_CONSUMED,lacpPort.slaveRxLacpBpduReceived(bpduInfo));
	}

	@Test
	public void testSlaveUpdateLacpRate() {
		lacpPort.slaveUpdateLacpRate(1);
		assertEquals(LacpConst.PORT_STATE_LACP_TIMEOUT, lacpPort.portGetActorOperPortState() & LacpConst.PORT_STATE_LACP_TIMEOUT);
		lacpPort.slaveUpdateLacpRate(0);
		assertEquals(0, lacpPort.portGetActorOperPortState() & LacpConst.PORT_STATE_LACP_TIMEOUT);
	}

	@Test
	public void testRunProtocolStateMachine() {
		TimerExpiryMessage timer = new TimerExpiryMessage(1, 2, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		lacpPort.runProtocolStateMachine(bpduInfo, timer);
		lacpPort.setStateMachineBitSet((short)0x801);
		lacpPort.runProtocolStateMachine(null, timer);
	}


	@Test
	public void testPortRxStateMachine() {
		LacpBpduInfo lacpbpdu = Mockito.mock(LacpBpduInfo.class);
		TimerExpiryMessage timer = new TimerExpiryMessage(1, 2, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		
		LacpBpduSysInfo act = Mockito.mock(LacpBpduSysInfo.class); 
		when(lacpbpdu.getActorSystemInfo()).thenReturn(act);
		when(lacpbpdu.getPartnerSystemInfo()).thenReturn(act);

		
		when(act.getNodePortNum()).thenReturn((short)0);
		when(act.getNodePortPriority()).thenReturn(0);
		byte[] adr = {0x9,0,0,0,0,0};
		when(act.getNodeSysAddr()).thenReturn(adr);
		when(act.getNodeSysPri()).thenReturn(0);
		when(act.getNodeKey()).thenReturn((short)0);
		when(act.getNodePortState()).thenReturn(adr[2]);
		

		lacpPort.setStateMachineBitSet((short)0);
		lacpPort.setEnabled(false);
		lacpPort.portRxStateMachine(lacpbpdu,timer);//goes to 2. /RX_Port_disabled
		lacpPort.setEnabled(true);
		LacpPort pt = Mockito.mock(LacpPort.class);
		
		lacpPort.setStateMachineBitSet((short)0x200);
		lacpPort.portRxStateMachine(lacpbpdu,null);//goes to 5. /RX_Port_disabled
		
		lacpPort.setStateMachineBitSet((short)0x2);
		lacpPort.portRxStateMachine(lacpbpdu,null);//goes to 5. /Expired
		
		lacpPort.setStateMachineBitSet((short)0x2);
		lacpPort.portRxStateMachine(lacpbpdu,timer);//goes to 3. /Current
		
		

		lacpPort.portRxStateMachine(null,timer);//goes to 4. /Expired
		lacpPort.portRxStateMachine(null,timer);//goes to 4. /default
		lacpPort.portRxStateMachine(lacpbpdu,timer);//goes to 3. /Expired

		lacpPort.setStateMachineBitSet((short)0x1);
		lacpPort.portRxStateMachine(lacpbpdu,timer);//goes to 1. /RX_Port_disabled
		
		lacpPort.setStateMachineBitSet((short)0);
		lacpPort.portRxStateMachine(lacpbpdu,null);
	}

	@Test
	public void testPortPeriodicStateMachine() {
		//TODO
		////this.partnerOper.getPortState() & LacpConst.LONG_TIMEOUT)==0 . 
		//Anything And Long Timeout(0) equals 0.
		//Thus "if" condition is always true and hence 3rd brnch and executeaction(else) doesn't get executed
		TimerExpiryMessage timer = new TimerExpiryMessage(1, 2, Utils.timerWheeltype.PERIODIC_TIMER);

		//dummy->NoPeriodic->fast
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);
		
		lacpPort.setStateMachineBitSet((short)0x2);
		lacpPort.setActorOperPortKey((short) 0xff);
		lacpPort.getPartnerOper().setPortState((short)0);
		
		//Fast->slow
		lacpPort.getPartnerOper().setPortState((short)0);
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);
		
		//slow -> Periodic Tx --opposes to condition. To make condition false so that both side or OR gets executed
		lacpPort.getPartnerOper().setPortState((short)0xff);
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);

		//Periodic Tx -> fast                  //Won't work as commit action method changes per to slow or fast
		lacpPort.getPartnerOper().setPortState((short)0x0);
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);
		
		//fast -> periodic Tx
		lacpPort.getPartnerOper().setPortState((short)0xff);
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);

		
		// periodic Tx -> slow                 //Won't work as commit action method changes per to slow or fast
		lacpPort.getPartnerOper().setPortState((short)0xff);
		lacpPort.portPeriodicStateMachine(bpduInfo, timer);
		
	}

	@Test
	public void testPortSelectionLogic() {
		//LacpBond bond = Mockito.mock(LacpBond.class);
		LacpAggregator lag = Mockito.mock(LacpAggregator.class);
		//when(bond.findLacpAggByFitPort(lacpPort)).thenReturn(lag);
		List<LacpAggregator> aggList = new ArrayList<LacpAggregator>();
		aggList.add(lag);
		bond.setAggregatorList(aggList);
		when(lag.isPortFitToAgg(lacpPort)).thenReturn(false);
		lacpPort.portSelectionLogic();
		when(lag.isPortFitToAgg(lacpPort)).thenReturn(true);
		when(lag.canMoveToSelList(lacpPort)).thenReturn(true);
		when(lag.getIsActive()).thenReturn((short) 1);
		when(lag.isReselect()).thenReturn(true);
		when(lag.aggHasPort(lacpPort)).thenReturn(true).thenReturn(false);///
		when(lag.IsPortReachMaxCount(lacpPort)).thenReturn(true).thenReturn(false);///
		doNothing().when(lag).rmPortFromAgg(lacpPort);
		doNothing().when(lag).rmPortFromAggStandBy(lacpPort);
		when(lag.aggHasStandbyPort(lacpPort)).thenReturn(true);
		lacpPort.slaveSetBond(bond);
		lacpPort.setPortAggregator(lag);
		lacpPort.setStateMachineBitSet((short)0x8080);
		
		lacpPort.portSelectionLogic();
		lacpPort.portSelectionLogic();
		

		lacpPort.setStateMachineBitSet((short)0);
		lacpPort.portSelectionLogic();
		when(lag.IsPortReachMaxCount(lacpPort)).thenReturn(false);
		lacpPort.portSelectionLogic();
	}

	@Test
	public void testPortMuxStateMachine() {
		TimerExpiryMessage timer = new TimerExpiryMessage(1, 2, Utils.timerWheeltype.WAIT_WHILE_TIMER);
		LacpAggregator lag = Mockito.mock(LacpAggregator.class);
		lacpPort.setPortAggregator(lag);
		
		//enters first if branch- State <Dummy -> Detached>
		lacpPort.setStateMachineBitSet((short)0x1);
		lacpPort.portMuxStateMachine(timer);
		
		//else....

		//enters  first if branch- State <Detached  -> Detached>
		lacpPort.setStateMachineBitSet((short)0);
		lacpPort.portMuxStateMachine(timer);
		
		//enters  first if branch- State <Detached  -> Waiting>
		lacpPort.setStateMachineBitSet((short)0x180);
		lacpPort.portMuxStateMachine(timer);

		//enters  else and then if branch- State <Waiting -> Detached>
		lacpPort.setStateMachineBitSet((short)0x0);
		lacpPort.portMuxStateMachine(timer);

		//enters  first if branch- State <Detached  -> Waiting>
		lacpPort.setStateMachineBitSet((short)0x180);
		lacpPort.portMuxStateMachine(timer);

		//enters  second if branch- State <Waiting  -> Attached>
		lacpPort.setStateMachineBitSet((short)0x110);
		lacpPort.portMuxStateMachine(timer);
		

		//enters  third if branch- State <Attached  -> CollectingDistributing>
		lacpPort.setStateMachineBitSet((short)0x100);
		lacpPort.getPartnerOper().setPortState(LacpConst.PORT_STATE_SYNCHRONIZATION);
		lacpPort.portMuxStateMachine(timer);
		
		//enters  fourth if branch- State <CollectingDistributing -> Attached>
		lacpPort.setStateMachineBitSet((short)0x80);
		lacpPort.getPartnerOper().setPortState((byte)0);
		lacpPort.setPortAggregator(null);
		lacpPort.portMuxStateMachine(timer);
		

		//enters  third if branch- State <Attached  -> CollectingDistributing>
		lacpPort.setStateMachineBitSet((short)0x100);
		lacpPort.getPartnerOper().setPortState(LacpConst.PORT_STATE_SYNCHRONIZATION);
		lacpPort.setPortAggregator(lag);
		lacpPort.portMuxStateMachine(timer);

		
		//enters  fourth if branch- State <CollectingDistributing -> Attached>
		lacpPort.setStateMachineBitSet((short)0x80);
		lacpPort.getPartnerOper().setPortState((byte)0);
		lacpPort.portMuxStateMachine(timer);
		
		//enters  third if branch- State <Attached -> Detached>
		lacpPort.setStateMachineBitSet((short)0x180);
		lacpPort.getPartnerOper().setPortState((byte)0);
		lacpPort.portMuxStateMachine(timer);

		//enters  first if branch- State <Detached  -> Waiting>
		lacpPort.setStateMachineBitSet((short)0x180);
		lacpPort.portMuxStateMachine(timer);

		//enters  second if branch- State <Waiting  -> Attached>
		lacpPort.setStateMachineBitSet((short)0x110);
		lacpPort.portMuxStateMachine(null);

		//enters  third if branch- State <Attached -> Detached>
		lacpPort.setStateMachineBitSet((short)0);
		lacpPort.getPartnerOper().setPortState((byte)0);
		lacpPort.portMuxStateMachine(timer);
		
	}

	@Test
	public void testPortTxStateMachine() {
		//TODO - Check whether return slaveSendBpdu or just slaveSendBpdu;
		lacpPort.portTxStateMachine(null);
		lacpPort.setNtt(true);
		lacpPort.setStateMachineBitSet(LacpConst.PORT_LACP_ENABLED);
		TimerExpiryMessage timer = new TimerExpiryMessage(0, 1, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		lacpPort.portTxStateMachine(timer);
		
		timer.setTimerWheelType(Utils.timerWheeltype.PERIODIC_TIMER);
		lacpPort.setLink(LacpConst.BOND_LINK_DOWN);
		lacpPort.portTxStateMachine(timer);   //slaveSendBpdu ret -1

		timer.setTimerWheelType(Utils.timerWheeltype.WAIT_WHILE_TIMER);
		lacpPort.setNtt(true);
		lacpPort.setStateMachineBitSet(LacpConst.PORT_LACP_ENABLED);
		lacpPort.setLink(LacpConst.BOND_LINK_UP);
		lacpPort.portTxStateMachine(timer);        //slaveSendBpdu ret 0
	}

	@Test
	public void testSlavePortHandleLinkChange() {
		lacpPort.setLink((byte)0xf);
		lacpPort.slaveHandleLinkChange((byte)0);
		assertEquals(lacpPort.getLink(),(byte)0);
		
		lacpPort.setActorOperPortState((byte)0x00);
		lacpPort.slaveHandleLinkChange(LacpConst.BOND_LINK_UP);
		assertEquals(lacpPort.getLink(),LacpConst.BOND_LINK_UP);
		

		lacpPort.setActorOperPortState((byte)0x20);
		lacpPort.slaveHandleLinkChange(LacpConst.BOND_LINK_DOWN);
		assertEquals(lacpPort.getLink(),LacpConst.BOND_LINK_DOWN);
		assertEquals(LacpConst.PORT_BEGIN, lacpPort.getStateMachineBitSet() & LacpConst.PORT_BEGIN);
		
	}


	@Test
	public void testNC_Ref() {
		lacpPort.setResetStatus(true);
		WriteTransaction wtTrn = Mockito.mock(WriteTransaction.class);
		CheckedFuture<Void, TransactionCommitFailedException> future = Mockito.mock(CheckedFuture.class);
		when(dataBroker.newWriteOnlyTransaction()).thenReturn(wtTrn );
		when(wtTrn.submit()).thenReturn(future);
		LacpPort.setDataBrokerService(dataBroker);
		
		lacpPort.resetLacpParams();
		lacpPort.setLogicalNCRef(Mockito.mock(NodeConnectorRef.class));
	}

	
	@Test
	public void gettersSetters(){
		//-----------PartnerAdmin-------------
		{
			//system
			byte[] system = {0x1,0x2,0x3,0x4,0x5,0x6};
			lacpPort.portPartnerAdminSetSystem(system);
			byte[] systemRet = lacpPort.portPartnerAdminGetSystem();
			for(int i=0;i<system.length;i++){
				assertEquals(system[i],systemRet[i]);
			}
			//Syspriority
			int pri =5;
			lacpPort.portPartnerAdminsetSystemPriority(pri);
			assertEquals(pri, lacpPort.portPartnerAdminGetSystemPriority());

			//key
			short key = 0x55;
			lacpPort.portPartnerAdminSetKey(key);
			assertEquals(key, lacpPort.portPartnerAdminGetKey());

			//PortNumber
			short Pnum= 0x121;
			lacpPort.portPartnerAdminSetPortNumber(Pnum);
			assertEquals(Pnum, lacpPort.portPartnerAdminGetPortNumber());

			//PortPriority
			pri =8;
			lacpPort.portPartnerAdminSetPortPriority(pri);
			assertEquals(pri, lacpPort.portPartnerAdminGetPortPriority());

			//PortState
			short st=0x77;
			lacpPort.portPartnerAdminSetPortState(st);
			assertEquals(st, lacpPort.portPartnerAdminGetPortState());
		}
		//-------------------------------------------



		//-----------PartnerOper-------------
		{
			//system
			byte[] system = {0x1,0x2,0x3,0x4,0x5,0x6};
			lacpPort.portPartnerOperSetSystem(system);
			byte[] systemRet = lacpPort.portPartnerOperGetSystem();
			for(int i=0;i<system.length;i++){
				assertEquals(system[i],systemRet[i]);
			}
			//Syspriority
			int pri =5;
			lacpPort.portPartnerOperSetSystemPriority(pri);
			assertEquals(pri, lacpPort.portPartnerOperGetSystemPriority());

			//key
			short key = 0x55;
			lacpPort.portPartnerOperSetKey(key);
			assertEquals(key, lacpPort.portPartnerOperGetKey());

			//PortNumber
			short Pnum= 0x121;
			lacpPort.portPartnerOperSetPortNumber(Pnum);
			assertEquals(Pnum, lacpPort.portPartnerOperGetPortNumber());

			//PortPriority
			pri =8;
			lacpPort.portPartnerOperSetPortPriority(pri);
			assertEquals(pri, lacpPort.portPartnerOperGetPortPriority());

			//PortState
			short st=0x77;
			lacpPort.portPartnerOperSetPortState(st);
			assertEquals(st, lacpPort.portPartnerOperGetPortState());
		}
		//-------------------------------------------

		//data Service/ Data broker
		LacpPort.setDataBrokerService(dataBroker);

		
		//LacpPortId
		lacpPort.setLacpPortId((short)0x56);
		assertEquals(0x56,lacpPort.getLacpPortId());

		//ActorPortAggregatorIdentifier
		short id = 0x98;
		lacpPort.setActorPortAggregatorIdentifier(id);
		assertEquals(id,lacpPort.getactorPortAggregatorIdentifier());

		lacpPort.setactorPortAggregatorIdentifier(id);
		assertEquals(id,lacpPort.getActorPortAggregatorIdentifier());
		
		id=0x78;
		lacpPort.portSetActorPortAggregatorIdentifier(id);
		assertEquals(id,lacpPort.getactorPortAggregatorIdentifier());
		assertEquals(id,lacpPort.getActorPortAggregatorIdentifier());
		assertEquals(id,lacpPort.portGetActorPortAggregatorIdentifier());

		//PortPriority
		int pri=9;
		lacpPort.setPortPriority(pri);
		assertEquals(pri,lacpPort.getPortPriority());
		
		//PortAdminPortkey
		short key = 0x23;
		lacpPort.setPortAdminPortKey(key);
		assertEquals(key,lacpPort.getActorAdminPortKey());
		
		//ActorPortPriority
		lacpPort.setActorPortPriority(pri);
		assertEquals(pri,lacpPort.getActorPortPriority());
		
		lacpPort.setPortOperStatus(true);
		assertEquals(true,lacpPort.getPortOperStatus());
		
		assertEquals(true,lacpPort.getEnabled(true));//TODO
		
		lacpPort.portSetActorPortPriority(10);
		assertEquals(10,lacpPort.portGetActorPortPriority());
		
		//others   //getters with no setters
		lacpPort.getActorOperPortKey();
		lacpPort.portGetLagId();
		lacpPort.slaveGetduplex();
		lacpPort.slaveGetSwId();            //Check
		lacpPort.getLacpSwId();            
		lacpPort.getLacpBpduInfo();
		lacpPort.portGetPortStatus();
		lacpPort.getPortOperStatus();
	}

}
