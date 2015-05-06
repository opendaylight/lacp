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
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.*;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

import org.opendaylight.lacp.queue.*;



public class TxProcessor implements Runnable {

	private LacpTxQueue.QueueType  queueId;
	private final static Logger log = LoggerFactory.getLogger(TxProcessor.class);
	PacketProcessingService pktProcessService;

	

	public TxProcessor(LacpTxQueue.QueueType queueId, PacketProcessingService serv) {
		this.queueId = queueId;
		this.pktProcessService = serv;
	}


	@Override
	public void run() {
		boolean IsLacpLoaded=true;
		boolean IsQueueRdy=true;
		LacpPacketPdu lacpPDU = null;
		LacpTxQueue  lacpTxQueue = null;
		byte[] payload ;
		log.info("Spawned TxProcessor Thread");

		lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();
		log.info("QueueInstance is : " , lacpTxQueue);
		log.info("Queue ID is : " , queueId);

		while (IsLacpLoaded) {
			IsQueueRdy=true;

			while (IsQueueRdy)
			{
				try{
				lacpPDU = lacpTxQueue.dequeue(queueId);
				if (lacpPDU != null)
				{
					log.info("LACP PDU non-null, queueId is = {}  and  lacpPDU is = {}",queueId, lacpPDU);
					payload = TxUtils.convertLacpPdutoByte(lacpPDU);
					TxUtils.dispatchPacket(payload, lacpPDU.getIngressPort(), lacpPDU.getSrcAddress(), lacpPDU.getDestAddress(),pktProcessService);
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
				}catch(Exception e){
					log.error(e.getMessage());
				}
			}

		}
	}


}
