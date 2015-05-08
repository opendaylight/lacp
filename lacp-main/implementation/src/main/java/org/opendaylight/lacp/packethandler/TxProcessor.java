/*
/ * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.core.RSMManager;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.lacp.queue.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;

public class TxProcessor implements Runnable
{
    private LacpTxQueue.QueueType  queueId;
    private final static Logger log = LoggerFactory.getLogger(TxProcessor.class);
    PacketProcessingService pktProcessService;
    private static boolean isLacpLoaded = true;

    public TxProcessor(LacpTxQueue.QueueType queueId, PacketProcessingService serv) {
        this.queueId = queueId;
        this.pktProcessService = serv;
    }
    public static void resetLacpLoaded()
    {
        isLacpLoaded = false;
        return;
    }

    @Override
    public void run() 
    {
        boolean isQueueRdy=true;
        LacpPortInfo lacpPortId = null;
        LacpTxQueue  lacpTxQueue = null;
        LacpPort lacpPort = null;
        byte[] payload ;
        log.info("Spawned TxProcessor Thread");

        lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();

        while (isLacpLoaded) 
        {
            isQueueRdy=true;

            while (isQueueRdy)
            {
                lacpPort = null;
                lacpPortId = lacpTxQueue.dequeue(queueId);
                if (lacpPortId != null)
                {
                    log.debug("LACP TxProcessor queueId is = {}  and  lacpPort is = {}",queueId, lacpPortId.getPortId());
                    RSMManager rsmManager = RSMManager.getRSMManagerInstance();
                    lacpPort = rsmManager.getLacpPortFromBond(lacpPortId.getSwitchId(), (short) lacpPortId.getPortId());
                    if (lacpPort == null)
                    {
                        log.debug ("Unable to obtain the Lacp port object cannot be retrieved for port {} in node {}", lacpPortId.getPortId(), lacpPortId.getSwitchId());
                        continue;
                    }
                    else
                    {
                        log.debug ("Generating LacpPacketPdu for the port {}", lacpPortId.getPortId());
                        LacpPacketPdu pdu = lacpPort.updateLacpFromPortToLacpPacketPdu();
                        payload = TxUtils.convertLacpPdutoByte(pdu);
                        TxUtils.dispatchPacket(payload, pdu.getIngressPort(), pdu.getSrcAddress(), pdu.getDestAddress(), pktProcessService);
                        log.debug ("dispatched the packet out for port {}", lacpPortId.getPortId());
                    }
                }
                else
                {
                    isQueueRdy=false;
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e)
                    {
                        log.debug("TxProcessor: InterruptedException", e.getMessage());
                    }
                }
            }
        }
    }
}
