/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfo;
import org.opendaylight.lacp.Utils.HexEncode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import java.nio.ByteBuffer;

public class TxUtils {
    private final static Logger LOG = LoggerFactory.getLogger(TxProcessor.class);

    public static byte[] padExtraZeroes(byte[] tb, int typelen) {
        byte[] newtb = new byte[typelen];
        byte zb = 0x00;
        int extrazeroes=0, i=0;
        if (tb.length < typelen)
        {
            extrazeroes = typelen - tb.length;
        }
        for (i=0; i< extrazeroes; i++)
        {
            newtb[i] = zb;
        }
        for (int j=0; i< typelen; i++, j++)
        {
            newtb[i] = tb[j];
        }
        return newtb;
    }

    public static byte[] convertLacpPdutoByte(LacpPacketPdu lacpPDU) {
        byte[] bdata = new byte[128];
        byte[] padData = new byte[1];

        ByteBuffer bb = ByteBuffer.wrap(bdata);
        bb.put(HexEncode.bytesFromHexString(lacpPDU.getDestAddress().getValue().toString()));
        bb.put(HexEncode.bytesFromHexString(lacpPDU.getSrcAddress().getValue().toString()));
        bb.putShort(lacpPDU.getLenType().shortValue());
        bb.put((byte)lacpPDU.getSubtype().getIntValue());
        bb.put((byte)lacpPDU.getVersion().getIntValue());

        ActorInfo actorInfo = lacpPDU.getActorInfo();
        bb.put((byte)actorInfo.getTlvType().getIntValue());
        bb.put(actorInfo.getInfoLen().byteValue());
        bb.putShort(actorInfo.getSystemPriority().shortValue());
        bb.put(HexEncode.bytesFromHexString(actorInfo.getSystemId().getValue().toString()));
        bb.putShort(actorInfo.getKey().shortValue());
        bb.putShort(actorInfo.getPortPriority().shortValue());
        bb.putShort(actorInfo.getPort().shortValue());
        bb.put(actorInfo.getState().byteValue());
        bb.put(padExtraZeroes(padData, 3));

        PartnerInfo partnerInfo = lacpPDU.getPartnerInfo();
        bb.put((byte)partnerInfo.getTlvType().getIntValue());
        bb.put(partnerInfo.getInfoLen().byteValue());
        bb.putShort(partnerInfo.getSystemPriority().shortValue());
        bb.put(HexEncode.bytesFromHexString(partnerInfo.getSystemId().getValue().toString()));
        bb.putShort(partnerInfo.getKey().shortValue());
        bb.putShort(partnerInfo.getPortPriority().shortValue());
        bb.putShort(partnerInfo.getPort().shortValue());
        bb.put(partnerInfo.getState().byteValue());
        bb.put(padExtraZeroes(padData, 3));

        bb.put((byte)lacpPDU.getCollectorTlvType().getIntValue());
        bb.put(lacpPDU.getCollectorInfoLen().byteValue());
        bb.putShort(lacpPDU.getCollectorMaxDelay().shortValue());
        bb.put(padExtraZeroes(padData, 12));

        bb.put((byte)lacpPDU.getTerminatorTlvType().getIntValue());
        bb.put(lacpPDU.getTerminatorInfoLen().byteValue());
        bb.put(padExtraZeroes(padData, 50));
        bb.putInt(lacpPDU.getFCS().intValue());
        return (bdata);
    }

    public static void dispatchPacket(byte[] payload, NodeConnectorRef ingress, 
                      MacAddress srcMac, MacAddress destMac, 
                      PacketProcessingService pServ) {
        NodeConnectorRef destNodeConnector = ingress;

        if(destNodeConnector != null) {
            LOG.debug ("dispatching the packet on nc {}", destNodeConnector);
             sendPacketOut(payload, destNodeConnector,pServ);
        } else {
            LOG.debug("TxProcessor: desNodeConnector is NULL");
        }
    }

    public static void sendPacketOut(byte[] payload, NodeConnectorRef egress,
                PacketProcessingService pServ) {
        if(egress == null){
            return;
        }
        InstanceIdentifier<Node> egressNodePath = getNodePath(egress.getValue());
        TransmitPacketInput input = new TransmitPacketInputBuilder() 
            .setPayload(payload) 
            .setNode(new NodeRef(egressNodePath)) 
            .setEgress(egress) 
            .build();

        if (pServ != null){
            pServ.transmitPacket(input);
            LOG.info("sucessfully sent the transmitPacket");
        } else {
            LOG.warn("packetProcessingService is null");
        }
    }


    public static InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }
}
