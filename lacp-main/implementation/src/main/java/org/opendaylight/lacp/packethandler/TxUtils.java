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
import java.math.BigInteger;
import org.opendaylight.lacp.queue.*;




public class TxUtils {

	private final static Logger log = LoggerFactory.getLogger(TxProcessor.class);

	public static String macToString(String srcstr) {

                if(srcstr == null) {
                        return "null";
                }

                StringBuffer buf = new StringBuffer();
                for(int i = 0; i < srcstr.length(); i=i+3) {
			buf.append(srcstr.charAt(i));
			buf.append(srcstr.charAt(i+1));
                }
                return buf.toString();
        }

	public static void bytetoString(byte[] tb)
	{
		StringBuffer tsb = new StringBuffer();
                  for (int i=0;i<tb.length;i++) {
                          tsb.append(Integer.toHexString((int) tb[i]));
                 }

	}

	public static byte[] convertStringtoByte(String srcstr)
	{
		byte[] destb = new BigInteger(srcstr,16).toByteArray();
		bytetoString(destb);
		return (destb);
	}


	public static byte[] hexStringToByteArray(String s) {
    		int len = s.length();
		byte[] data = null;
		if(len == 1){
			data = new byte[1];
        		data[0] = (byte) Character.digit(s.charAt(0), 16);
		}
		else
		{
    			data = new byte[len / 2];
    			for (int i = 0; i < len; i += 2) {
        		data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
    			}
    		}
    		return data;
	}

	public static byte[] padExtraZeroes(byte[] tb, int typelen)
        {
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

	public static byte[] convertLacpPdutoByte( LacpPacketPdu lacpPDU ) {
		byte[] bdata = new byte[128];

		ByteBuffer bb = ByteBuffer.wrap(bdata);
		bb.put(HexEncode.bytesFromHexString((new String ((lacpPDU.getDestAddress().getValue())))));
		String s = macToString(new String ((lacpPDU.getDestAddress().getValue()).toString    ()));
		byte[] b = new BigInteger(s,16).toByteArray();
		StringBuffer sb3 = new StringBuffer();
                for (int i=0;i<b.length;i++) {
                        sb3.append(Integer.toHexString((int) b[i]));
                }

		bb.put(HexEncode.bytesFromHexString((new String ((lacpPDU.getSrcAddress().getValue())))));
		StringBuffer sb1 = new StringBuffer();
                for (int i=0;i<b.length;i++) {
                        sb1.append(Integer.toHexString((int) b[i]));
                }

		bytetoString(hexStringToByteArray(Integer.toHexString(lacpPDU.getLenType())));
		bb.put(padExtraZeroes(hexStringToByteArray(Integer.toHexString(lacpPDU.getLenType())),2));
		bb.put(convertStringtoByte((new String (new Integer((lacpPDU.getSubtype().getIntValue())).toString()))));
		bb.put(convertStringtoByte((new String (new Integer((lacpPDU.getVersion().getIntValue())).toString()))));

		ActorInfo actorInfo = lacpPDU.getActorInfo();
		bb.put(convertStringtoByte((new String (new Integer((actorInfo.getTlvType().getIntValue())).toString()))));
		bb.put(convertStringtoByte((new String (Integer.toHexString(actorInfo.getInfoLen()).toString()))));
		bb.put(padExtraZeroes(hexStringToByteArray(Integer.toHexString(actorInfo.getSystemPriority())),2));
		bb.put(HexEncode.bytesFromHexString((new String ((actorInfo.getSystemId().getValue()).toString()))));
		bb.put(padExtraZeroes(hexStringToByteArray(Integer.toHexString(actorInfo.getKey())),2));
		bb.put(padExtraZeroes(hexStringToByteArray(Integer.toHexString(actorInfo.getPortPriority())),2));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(actorInfo.getPort()).toString()))),2));
		bb.put(hexStringToByteArray(Integer.toHexString(actorInfo.getState())));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(actorInfo.getReserved()).toString()))),2));
		bb.put(convertStringtoByte((new String (Integer.toHexString(actorInfo.getReserved1()).toString()))));

		PartnerInfo partnerInfo = lacpPDU.getPartnerInfo();
		bb.put(convertStringtoByte((new String (new Integer((partnerInfo.getTlvType().getIntValue())).toString()))));
		bb.put(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getInfoLen()).toString()))));
		bb.put(padExtraZeroes(hexStringToByteArray(Integer.toHexString(partnerInfo.getSystemPriority()).toString()),2));
		bb.put(HexEncode.bytesFromHexString((new String ((partnerInfo.getSystemId().getValue()).toString()))));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getKey()).toString()))),2));
		bb.put(padExtraZeroes(convertStringtoByte(new String (Integer.toHexString(partnerInfo.getPortPriority()).toString())),2));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getPort()).toString()))),2));
		bb.put(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getState()).toString()))));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getReserved()).toString()))),2));
		bb.put(convertStringtoByte((new String (Integer.toHexString(partnerInfo.getReserved1()).toString()))));


		bb.put(convertStringtoByte((new String (new Integer((lacpPDU.getCollectorTlvType().getIntValue())).toString()))));
		bb.put(convertStringtoByte((new String (Integer.toHexString(lacpPDU.getCollectorInfoLen()).toString()))));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Integer.toHexString(lacpPDU.getCollectorMaxDelay()).toString()))),2));
		bb.put(padExtraZeroes(convertStringtoByte((new String ((lacpPDU.getCollectorReserved()).toString()))),8));
		bb.put(padExtraZeroes(convertStringtoByte((new String (Long.toHexString(lacpPDU.getCollectorReserved1()).toString()))),4));
		bb.put(convertStringtoByte((new String (new Integer((lacpPDU.getTerminatorTlvType().getIntValue())).toString()))));
		bb.put(convertStringtoByte((new String (Integer.toHexString(lacpPDU.getTerminatorInfoLen()).toString()))));
		bb.put(padExtraZeroes(convertStringtoByte((new String (lacpPDU.getTerminatorReserved()))),50));
		bb.put(padExtraZeroes(convertStringtoByte((new String ((lacpPDU.getFCS()).toString()))),4));
		
		StringBuffer sb4 = new StringBuffer();
                for (int i=0;i<bdata.length;i++) {
                        sb4.append(Integer.toHexString((int) bdata[i]));
                }

		return(bdata);
	}

	public static void dispatchPacket(byte[] payload, NodeConnectorRef ingress, 
					  MacAddress srcMac, MacAddress destMac, 
					  PacketProcessingService pServ) {

		String nodeId = ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class).getId().getValue();
		NodeConnectorRef destNodeConnector = ingress;

		if(destNodeConnector != null) {
		 	sendPacketOut(payload, destNodeConnector,pServ);
		} else {
			log.debug("TxProcessor: desNodeConnector is NULL");
		}
	}



	public static org.opendaylight.controller.sal.binding.api.RpcProviderRegistry rpcRegistryDependency;

	public static org.opendaylight.controller.sal.binding.api.RpcProviderRegistry getRpcRegistryDependency(){
        	return rpcRegistryDependency;
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

			if(pServ != null){
				pServ.transmitPacket(input);
				log.info("sucessfully sent the transmitPacket");
			}else{
				log.warn("packetProcessingService is null");
			}
	}


	public static InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
		return nodeChild.firstIdentifierOf(Node.class);
	}
}
