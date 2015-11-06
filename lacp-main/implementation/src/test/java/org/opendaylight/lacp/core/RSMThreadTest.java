/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.timer.TimerExpiryMessage;
import org.opendaylight.lacp.timer.Utils.timerWheeltype;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;

public class RSMThreadTest {

	RSMThread rsmThread;
	int id=0;
	LacpNodeExtn lacpNode;
	

	@Before
	public void setUp() throws Exception {
		rsmThread = new RSMThread();
		id=0;
		NodeId nId = new NodeId("openflow:"+(id+1));
		NodeConnectorId ncId = new NodeConnectorId(""+(id+1));
		InstanceIdentifier<NodeConnector> nodeId = InstanceIdentifier.builder(Nodes.class)
        		.child (Node.class, new NodeKey (nId))
        		.child(NodeConnector.class, new NodeConnectorKey(ncId)).build();
		//nodeId.firstKeyOf(Node.class, NodeId.class);
		lacpNode = new LacpNodeExtn(nodeId);
        //Long swid = LacpUtil.getNodeSwitchId(nodeId);
       // bond = LacpBond.newInstance((short) 1001, lacpNode);
        rsmThread.setLacpNode(lacpNode);
	}


	@Test
	public void testHandleLacpBpduNodeCleanup() {
		LacpBpduInfo bpduInfo = LacpBpduInfoLacpPacketPdu(6); 
		rsmThread.handleLacpBpdu(bpduInfo);
		bpduInfo = LacpBpduInfoLacpPacketPdu(2); 
		rsmThread.handleLacpBpdu(bpduInfo);
		bpduInfo = LacpBpduInfoLacpPacketPdu(4); 
		rsmThread.handleLacpBpdu(bpduInfo);
		rsmThread.nodeCleanup();
	}
	
	
	private LacpBpduInfo LacpBpduInfoLacpPacketPdu(int Syspri) {
		LacpBpduInfo bpduInfo;
        LacpPacketPduBuilder builder = new LacpPacketPduBuilder();
        ActorInfoBuilder actorbuilder = new ActorInfoBuilder();
        PartnerInfoBuilder partnerbuilder = new PartnerInfoBuilder();
        NodeId nId = new NodeId("openflow:1");
        NodeConnector nd;
        InstanceIdentifier<NodeConnector> iNc = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, new NodeKey (nId)).child(NodeConnector.class).build();
        NodeConnectorRef ncRef = new NodeConnectorRef(iNc);
        initFnc(ncRef);
        builder.setIngressPort(ncRef);
        
        //------------Actor------
        Integer aPort = 50;
		actorbuilder.setPort(aPort);
        Short aState = (short) 11;
		actorbuilder.setState(aState);
        Integer aPortPri = 2;
		actorbuilder.setPortPriority(aPortPri);
        Integer aKey = 1001;
		actorbuilder.setKey(aKey);
        MacAddress aMac = new MacAddress("10:10:10:55:15:65");
		actorbuilder.setSystemId(aMac);
        Integer aSysPri = Syspri;
		actorbuilder.setSystemPriority(aSysPri);
        //----------------------
		
		//------------Partner-----
        Integer pPort = 10;
		partnerbuilder.setPort(pPort);
        Short pState = (short) 51;
		partnerbuilder.setState(pState);
        Integer pPortPri = 5;
		partnerbuilder.setPortPriority(pPortPri);
        Integer pKey = 5645;
		partnerbuilder.setKey(pKey);
        MacAddress pMac = new MacAddress("10:10:10:55:67:87");
		partnerbuilder.setSystemId(pMac);
        Integer pSysPri = Syspri-1;
		partnerbuilder.setSystemPriority(pSysPri);
        //----------------------
        
        builder.setActorInfo(actorbuilder.build());
        builder.setPartnerInfo(partnerbuilder.build());
        builder.setCollectorMaxDelay(10);
        
        
        bpduInfo = new LacpBpduInfo(builder.build());
        
        bpduInfo.getActorSystemInfo().getNodeSysPri();
        //assertEquals(nc,bpduInfo.getNCRef());
        return bpduInfo;
	}
	
	private void initFnc(NodeConnectorRef ncRef){
		NodeConnector nc = null;
        ReadOnlyTransaction readTx = LacpUtil.getDataBrokerService().newReadOnlyTransaction();
        try
        {
            Optional<NodeConnector> dataObject = readTx.read(LogicalDatastoreType.OPERATIONAL,
                                                             (InstanceIdentifier<NodeConnector>) ncRef.getValue()).get();
            if (dataObject.isPresent())
            {
                nc = (NodeConnector) dataObject.get();
        		FlowCapableNodeConnector fnc = mock(FlowCapableNodeConnector.class);
        		PortFeatures pf = new PortFeatures(false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false);
        		PortNumberUni fr = new PortNumberUni((long) 10*id + id);
        		when(fnc.getCurrentFeature()).thenReturn(pf);
        		when(fnc.getPortNumber()).thenReturn(fr);
        		when(nc.getAugmentation(FlowCapableNodeConnector.class)).thenReturn(fnc);
        		id++;
            }
        }
        catch(Exception e)
        {
            readTx.close();
        }
        readTx.close();
	}
	
	
	

	@Test
	public void testHandlePortTimeout() {
		LacpBpduInfo bpduInfo = LacpBpduInfoLacpPacketPdu(6); 
		rsmThread.handleLacpBpdu(bpduInfo);
		//bpduInfo = LacpBpduInfoLacpPacketPdu(2); 
		//rsmThread.handleLacpBpdu(bpduInfo);
		int pid=0;
		long swid=1;
		TimerExpiryMessage timerMsg = new TimerExpiryMessage(swid, pid, timerWheeltype.CURRENT_WHILE_TIMER);
		rsmThread.handlePortTimeout(timerMsg);
	}

	@Test
	public void testHandleLacpPortState() {

        NodeId nId = new NodeId("openflow:1");
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class, new NodeKey (nId)).child(NodeConnector.class).build();
		int i=11;
		long swid=1;
		int portStatus=0;
		boolean rFlag=true;
		ncId = lacpNode.getNodeId();
		LacpPortStatus status = new LacpPortStatus(swid, i, portStatus, ncId, rFlag);
		rsmThread.handleLacpPortState(status);
		

		LacpBpduInfo bpduInfo = LacpBpduInfoLacpPacketPdu(6); 
		rsmThread.handleLacpBpdu(bpduInfo);
		i = 0;
		portStatus = 1;
		status = new LacpPortStatus(swid, i, portStatus, ncId, rFlag);
		rsmThread.handleLacpPortState(status);

		portStatus = 0;
		status = new LacpPortStatus(swid, i, portStatus, ncId, rFlag);
		rsmThread.handleLacpPortState(status);

	}


	@Test
	public void testHandleLacpNodeDeletion() {
		rsmThread = new RSMThread();
		NodeId nId = new NodeId("openflow:"+(id+1));
	NodeConnectorId ncId = new NodeConnectorId(""+(id+1));
	InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
    		.child (Node.class, new NodeKey (nId)).build();
	lacpNode = Mockito.mock(LacpNodeExtn.class);
	when(lacpNode.getFlowId()).thenReturn((long)123);
	when(lacpNode.getNodeId()).thenReturn(nodeId);
	when(lacpNode.getSwitchId()).thenReturn(LacpUtil.getNodeSwitchId(nodeId));
	//lacpNode.updateLacpNodeInfo();
	doNothing().when(lacpNode).deleteLacpNode();
	
	LacpSystem.getLacpSystem().addLacpNode(nodeId, lacpNode);
	//lacpNode = LacpSystem.getLacpSystem().getLacpNode((long)2);
    rsmThread.setLacpNode(lacpNode);
		rsmThread.handleLacpNodeDeletion();
		
	}

	@Test
	public void testRun() {
		//TODO - No enqueue
		rsmThread.startRSM();
		int i=0;
		while(i++ < 1000);
		rsmThread.interruptRSM();
	}


}
