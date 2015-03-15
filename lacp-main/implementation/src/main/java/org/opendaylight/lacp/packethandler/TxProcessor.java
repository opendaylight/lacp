/*
/ * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.*;
import org.opendaylight.lacp.Utils.*;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;


import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;


import java.nio.ByteBuffer;
import java.util.Arrays;
import org.opendaylight.lacp.queue.*;



public class TxProcessor implements Runnable {

	private int  queueId;
	private final static Logger log = LoggerFactory.getLogger(TxProcessor.class);

	public TxProcessor(int queueId) {
		this.queueId = queueId;
	}


	@Override
	public void run() {
		boolean IsLacpUnloaded=false;
		boolean IsQueueRdy=true;
		LacpPacketPdu lacpPDU = null;
		LacpTxQueue  lacpTxQueue = null;
		byte[] payload ;
		System.out.println ("Spawned TxProcessor Thread");
		log.info("Spawned TxProcessor Thread");

		lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();

		while (IsLacpUnloaded) {
			
			IsQueueRdy=true;
			while (IsQueueRdy)
			{
				lacpPDU = lacpTxQueue.dequeue(queueId);
				if (lacpPDU != null)
				{
					payload = convertLacpPdutoByte(lacpPDU);
					dispatchPacket(payload, lacpPDU.getIngressPort(), lacpPDU.getSrcAddress(), lacpPDU.getDestAddress());
					//Send the Packet Out
				}
				else
				{
					IsQueueRdy=false;
					try {
						Thread.sleep(100);
					} catch ( InterruptedException e ) {
						log.debug("TxProcessor: InterruptedException", e.getMessage());
					}
				}
			}

			
			//Add Condition
			//IsLacpUnloaded=true;
		}
	}


	public byte[] convertLacpPdutoByte( LacpPacketPdu lacpPDU ) {
		byte[] bdata = new byte[128];

		ByteBuffer bb = ByteBuffer.wrap(bdata);
		bb.put((new String ((lacpPDU.getDestAddress()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getSrcAddress()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getLenType()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getSubtype()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getVersion()).toString())).getBytes());

		ActorInfo actorInfo = lacpPDU.getActorInfo();
		bb.put((new String ((actorInfo.getTlvType()).toString())).getBytes());
		bb.put((new String ((actorInfo.getInfoLen()).toString())).getBytes());
		bb.put((new String ((actorInfo.getSystemPriority()).toString())).getBytes());
		bb.put((new String ((actorInfo.getSystemId()).toString())).getBytes());
		bb.put((new String ((actorInfo.getKey()).toString())).getBytes());
		bb.put((new String ((actorInfo.getPortPriority()).toString())).getBytes());
		bb.put((new String ((actorInfo.getPort()).toString())).getBytes());
		bb.put((new String ((actorInfo.getState()).toString())).getBytes());
		bb.put((new String ((actorInfo.getReserved()).toString())).getBytes());
		bb.put((new String ((actorInfo.getReserved1()).toString())).getBytes());

		PartnerInfo partnerInfo = lacpPDU.getPartnerInfo();
		bb.put((new String ((partnerInfo.getTlvType()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getInfoLen()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getSystemPriority()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getKey()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getPortPriority()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getPort()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getState()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getReserved()).toString())).getBytes());
		bb.put((new String ((partnerInfo.getReserved1()).toString())).getBytes());


		bb.put((new String ((lacpPDU.getCollectorTlvType()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getCollectorInfoLen()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getCollectorMaxDelay()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getCollectorReserved()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getCollectorReserved1()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getTerminatorTlvType()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getTerminatorInfoLen()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getTerminatorReserved()).toString())).getBytes());
		bb.put((new String ((lacpPDU.getFCS()).toString())).getBytes());
		

		return(bdata);
	}


	public void dispatchPacket(byte[] payload, NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {

		String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class).getId().getValue();
//		NodeConnectorRef srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);
//		if(srcConnectorRef == null) {
//			srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);
//		}

		//NodeConnectorRef destNodeConnector = inventoryReader.getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), destMac);
		NodeConnectorRef destNodeConnector = ingress;

/*		if(srcConnectorRef != null) {
			 if(destNodeConnector != null) {
				 sendPacketOut(payload, srcConnectorRef, destNodeConnector);
			 } else {
				log.debug("TxProcessor: desNodeConnector is NULL");
			}
		} else {
			log.debug("TxProcessor: SrcNode is not available");
		} */

		if(destNodeConnector != null) {
		 	//sendPacketOut(payload, srcConnectorRef, destNodeConnector);
		 	sendPacketOut(payload, destNodeConnector);
		} else {
			log.debug("TxProcessor: desNodeConnector is NULL");
		}
	}



	private org.opendaylight.controller.sal.binding.api.RpcProviderRegistry rpcRegistryDependency;

	protected final org.opendaylight.controller.sal.binding.api.RpcProviderRegistry getRpcRegistryDependency(){
        	return rpcRegistryDependency;
	}

	//public void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) 
	public void sendPacketOut(byte[] payload, NodeConnectorRef egress) {

			RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();

			PacketProcessingService packetProcessingService = 
				rpcRegistryDependency.<PacketProcessingService>getRpcService(PacketProcessingService.class);
			if(egress == null) return;
			InstanceIdentifier<Node> egressNodePath = getNodePath(egress.getValue());
			TransmitPacketInput input = new TransmitPacketInputBuilder() //
				.setPayload(payload) //
				.setNode(new NodeRef(egressNodePath)) //
				.setEgress(egress) //
			//	.setIngress(ingress) //
				.build();

			packetProcessingService.transmitPacket(input);
	}


	private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
		return nodeChild.firstIdentifierOf(Node.class);
	}
				
		
}
