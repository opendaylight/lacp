/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.packethandler;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.lacp.Utils.HexEncode;
import org.opendaylight.lacp.Utils.BitBufferHelper;
import org.opendaylight.lacp.util.LacpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.lacp.queue.LacpQueue;
import org.opendaylight.lacp.inventorylistener.LacpDataListener;

public class LacpPacketHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(LacpPacketHandler.class);
    private static DataBroker dataBroker;
    private LacpQueue<PacketReceived> rawQueueId;

    public static void setDataBrokerService (DataBroker dataService)
    {
        dataBroker = dataService;
    }
    public void updateQueueId(LacpQueue<PacketReceived>queueId)
    {
        rawQueueId = queueId;
        return;
    }
    @Override
    public void onPacketReceived (PacketReceived packetReceived)
    {
        boolean result = false;
        if (packetReceived == null)
        {
            LOG.debug("receiving null packet. returning without any processing");
            return;
        }
        byte[] data = packetReceived.getPayload();
        if (data.length <= 0)
        {
            LOG.debug ("received packet with invalid length {}", data.length);
            return;
        }
        try
        {
            MacAddress destMac = new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 0, 48)));
            int ethType = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96, 16));

            if ((ethType == LacpUtil.LACP_ETHTYPE) && (destMac.equals(LacpUtil.LACP_MAC) == true))
            {
                /* receive packets only on external ports */
                if (verifyExternalPort(packetReceived.getIngress()) == true)
                {
                    result = rawQueueId.enqueue(packetReceived);
                    LOG.debug ("enqueue  packet to the packetReceived queue, result - {} size {}", result, rawQueueId.size());
                }
                else
                {
                    LOG.debug("LACP Pdu received on internal port dropped {} ", packetReceived.getIngress());
                }
            }
            else
            {
                return;
            }
        }
        catch(Exception e)
        {
            LOG.warn("Failed to decode packet: {}", e.getMessage());
            return;
        }
    }
    private boolean verifyExternalPort (NodeConnectorRef ncRef)
    {
        return (LacpDataListener.checkExternalNodeConn(ncRef.getValue()));
    }
}
