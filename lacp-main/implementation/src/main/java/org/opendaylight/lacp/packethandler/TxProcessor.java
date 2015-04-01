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
	private static boolean IsLacploaded=true;
//	private int execType=0;

	

	public TxProcessor(int queueId) {
		this.queueId = queueId;
	}

	public static void setLacploaded(boolean load) {
                IsLacploaded = load;
        }


	@Override
	public void run() {
		boolean IsQueueRdy=true;
		LacpPacketPdu lacpPDU = null;
		LacpTxQueue  lacpTxQueue = null;
		byte[] payload ;
		log.info("Spawned TxProcessor Thread");

		lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();

		while (IsLacploaded) {
			
			IsQueueRdy=true;
			while (IsQueueRdy)
			{
				lacpPDU = lacpTxQueue.dequeue(queueId);
				if (lacpPDU != null)
				{
					payload = TxUtils.convertLacpPdutoByte(lacpPDU);
					//Send the Packet Out
					TxUtils.dispatchPacket(payload, lacpPDU.getIngressPort(), lacpPDU.getSrcAddress(), lacpPDU.getDestAddress());
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

			
		}
	}


}
