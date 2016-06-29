/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
 
package org.opendaylight.lacp.core;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.Utils.HexEncode;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpBpduInfoTest {
	LacpBpduInfo lacpBpdu;
	LacpBpduSysInfo actor;
	LacpBpduSysInfo partner;
	@MockitoAnnotations.Mock
	private DataBroker dataBroker;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		byte[] act = {0,0,0,0,0,0x1};
		byte[] part = {0,0,0,0,0,0x2};
		actor = new LacpBpduSysInfo();
		actor.setNodeSysAddr(act);
		partner = new LacpBpduSysInfo(1, part,(short)5, 3, (short) 80, (byte)0x55);
		
		lacpBpdu = new LacpBpduInfo(0, (short)30, actor, partner, (short)6);
		
		LacpUtil.setDataBrokerService(dataBroker);

		NodeConnector nc = mock(NodeConnector.class);
		Optional<NodeConnector> optionalNodes = Optional.of(nc);

		ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
		CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
		when(checkedFuture.get()).thenReturn(optionalNodes);
		when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
		when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
	}


	@Test
	public void testHashCode() {
		assertNotNull(lacpBpdu.hashCode());
		
	}

	@Test
	public void testGetMessageType() {
		assertEquals(LacpPDUPortStatusContainer.MessageType.LACP_PDU_MSG,lacpBpdu.getMessageType());
	}

	@Test
	public void testLacpBpduInfoLacpPacketPdu() {
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
        Integer aSysPri = 1;
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
        Integer pSysPri = 3;
		partnerbuilder.setSystemPriority(pSysPri);
        //----------------------
        
        builder.setActorInfo(actorbuilder.build());
        builder.setPartnerInfo(partnerbuilder.build());
        builder.setCollectorMaxDelay(10);
        
        
        bpduInfo = new LacpBpduInfo(builder.build());
        
        
        //assertEquals(nc,bpduInfo.getNCRef());
        assertTrue(Arrays.equals(HexEncode.bytesFromHexString(aMac.getValue()),bpduInfo.getActorSystemInfo().getNodeSysAddr()));
        assertTrue(Arrays.equals(HexEncode.bytesFromHexString(pMac.getValue()),bpduInfo.getPartnerSystemInfo().getNodeSysAddr()));
        
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
        		PortNumberUni fr = new PortNumberUni((long) 43);
        		when(fnc.getPortNumber()).thenReturn(fr);
        		when(nc.getAugmentation(FlowCapableNodeConnector.class)).thenReturn(fnc);
            }
        }
        catch(Exception e)
        {
            readTx.close();
        }
        readTx.close();
	}
	
	@Test
	public void testEqualsObject() {
		assertTrue(lacpBpdu.equals(lacpBpdu));
		LacpBpduInfo bpdu = new LacpBpduInfo();
		assertFalse(lacpBpdu.equals(bpdu));
		assertFalse(lacpBpdu.equals(new String("Comp fail")));
		assertFalse(lacpBpdu.equals(bpdu));  //swid fails
		
		bpdu.setSwId(lacpBpdu.getSwId());
		assertFalse(lacpBpdu.equals(bpdu));  //PortId fails
		
		bpdu.setPortId(lacpBpdu.getPortId());
		bpdu.setType(5);
		assertFalse(lacpBpdu.equals(bpdu));  //type fails
		
		bpdu.setType(lacpBpdu.getType());
		assertFalse(lacpBpdu.equals(bpdu));  //Actor fails
		
		bpdu.setActorSystemInfo(lacpBpdu.getActorSystemInfo());
		assertFalse(lacpBpdu.equals(bpdu));  //Partner fails
		
		bpdu.setPartnerSystemInfo(lacpBpdu.getPartnerSystemInfo());
		assertFalse(lacpBpdu.equals(bpdu));  //CollectionMaxDelay fails
		
		bpdu.setCollectorMaxDelay(lacpBpdu.getCollectorMaxDelay());
		assertFalse(lacpBpdu.equals(bpdu));  //receivedData fails
		
		
		//assertTrue(lacpBpdu.equals(bpdu));
	}

	@Test
	public void testToString() {
		assertNotNull(lacpBpdu.toString());
	}

}
