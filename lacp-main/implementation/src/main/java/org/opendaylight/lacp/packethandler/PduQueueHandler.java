/*
  * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  *   * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  *     * and is available at http://www.eclipse.org/legal/epl-v10.html
  *     */
package org.opendaylight.lacp.packethandler;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


import org.opendaylight.controller.sal.binding.api.NotificationProviderService;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;


import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.*;
import org.opendaylight.lacp.Utils.*;

import org.opendaylight.lacp.queue.*;
import org.opendaylight.lacp.core.*;
import org.opendaylight.lacp.inventory.*;


public class PduQueueHandler {

	private final static Logger LOG = LoggerFactory.getLogger(PduQueueHandler.class);
	private final int SLEEP_TIME = 100;

	public void checkQueue(){

	   boolean IsnewNode = false;
	   boolean HasPktArrvd = false;
	   PacketReceived packetReceived = null;
		// Get the Node specific PDU Queue Instance
		LacpPDUQueue lacpPduQ = LacpPDUQueue.getLacpPDUQueueInstance();


		// Get the RAW Packet Queue Instance
 		LacpQueue <PacketReceived> lacpRxQ = LacpRxQueue.getLacpRxQueueId();

		// Dequeue LACP Packet from RAW Packet Queue.
		while (!HasPktArrvd)
		{
			packetReceived = lacpRxQ.dequeue();
			if (packetReceived != null)  {
				HasPktArrvd = true;
			}
			try {
				Thread.sleep(SLEEP_TIME);
			}catch( InterruptedException e ) {
				LOG.debug("PduQueueHandler: Interrupted Exception ", e.getMessage());
			}
		}

		// Decode the received Packet.
		LacpPacketPduBuilder builder = decodeLacp(packetReceived);


		LacpPacketPdu lacpPacketPdu = builder.build();
		ActorInfo actorInfo = builder.getActorInfo();
		long sid = NodePort.getSwitchId(builder.getIngressPort());
		// Check if this is the first LACP PDU received for the Node.
		IsnewNode = !(lacpPduQ.isLacpQueuePresent(sid));

		if (IsnewNode)
                {
                      	RSMManager instance = RSMManager.getRSMManagerInstance();
			LacpSystem lacpSystem = LacpSystem.getLacpSystem();
			LacpNodeExtn lacpNodeExtn = lacpSystem.getLacpNode(sid);
                       	instance.createRSM(lacpNodeExtn);

                }
		// Enqueue the decoded LACP Packet to LACP Packet PDU Queue
		LacpBpduInfo lacpBpduInfo = new LacpBpduInfo(lacpPacketPdu);
		lacpPduQ.enqueue(sid, lacpBpduInfo);
	   

	   return;

	}
			

	// Delete the LACP Packet PDU Queue when the node is deleted.
	public boolean deleteQueue(long switchId) {
		boolean Isdeleted = false;

		LacpPDUQueue lacpPduQ = LacpPDUQueue.getLacpPDUQueueInstance();
		Isdeleted = lacpPduQ.deleteLacpQueue(switchId);
		return(Isdeleted);

	}

	public static String macToString(String srcstr) {

                if(srcstr == null) {
                        return "null";
                }

                StringBuffer buf = new StringBuffer();
		int index = 3;
                for(int i = 0; i < srcstr.length(); i=i+index) {
                        buf.append(srcstr.charAt(i));
                        buf.append(srcstr.charAt(i+1));
                }
                return buf.toString();
        }


	public static String bytesToString(byte[] bytes) {

    		if(bytes == null) {
      			return "null";
    		}

    		String ret = "";
    		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < bytes.length; i++) {
			byte byteMask = (byte) 0xff;
      			short u8byte = (short) (bytes[i] & byteMask);
      			String tmp = Integer.toHexString(u8byte);
      			if(tmp.length() == 1) {
        			buf.append("0");
      			}
      			buf.append(tmp);
    	   	}
		ret = buf.toString();
		return ret;
	}

        public  LacpPacketPduBuilder decodeLacp(PacketReceived packetReceived) {


		int bitOffset = 0;
		int offsetLenEight = 8;
		int offsetLenSixteen = 16;
		int offsetLenThirtyTwo = 32; 
		int offsetLenFortyEight = 48; 
		int offsetLenFourHundred = 400; 

		byte[] data = packetReceived.getPayload();

		LacpPacketPduBuilder builder = new LacpPacketPduBuilder();
		ActorInfoBuilder actorbuilder = new ActorInfoBuilder();
		PartnerInfoBuilder partnerbuilder = new PartnerInfoBuilder();
		try {

			builder.setIngressPort(packetReceived.getIngress());
			builder.setDestAddress(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, offsetLenFortyEight))));
			bitOffset = bitOffset + offsetLenFortyEight;


			builder.setSrcAddress(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, offsetLenFortyEight))));
			bitOffset = bitOffset + offsetLenFortyEight;

			builder.setLenType(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			builder.setSubtype(SubTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;

			builder.setVersion(VersionValue.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;


			actorbuilder.setTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;

			actorbuilder.setInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			actorbuilder.setSystemPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			actorbuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, offsetLenFortyEight))));
			bitOffset = bitOffset + offsetLenFortyEight;

			actorbuilder.setKey(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			actorbuilder.setPortPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			actorbuilder.setPort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			actorbuilder.setState(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			actorbuilder.setReserved(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			actorbuilder.setReserved1(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			builder.setActorInfo(actorbuilder.build());

			partnerbuilder.setTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;

			partnerbuilder.setInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			partnerbuilder.setSystemPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			partnerbuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, offsetLenFortyEight))));
			bitOffset = bitOffset + offsetLenFortyEight;


			partnerbuilder.setKey(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			partnerbuilder.setPortPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			partnerbuilder.setPort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			partnerbuilder.setState(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			partnerbuilder.setReserved(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			partnerbuilder.setReserved1(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			builder.setPartnerInfo(partnerbuilder.build());

			builder.setCollectorTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;

			builder.setCollectorInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			builder.setCollectorMaxDelay(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			BigInteger bi = new BigInteger(BitBufferHelper.getBits(data, bitOffset, offsetLenThirtyTwo));
			builder.setCollectorReserved(bi);
			bitOffset = bitOffset + offsetLenThirtyTwo;

			builder.setCollectorReserved1(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, offsetLenSixteen)));
			bitOffset = bitOffset + offsetLenSixteen;

			builder.setTerminatorTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, offsetLenEight))));
			bitOffset = bitOffset + offsetLenEight;

			builder.setTerminatorInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, offsetLenEight)));
			bitOffset = bitOffset + offsetLenEight;

			builder.setTerminatorReserved(bytesToString(BitBufferHelper.getBits(data, bitOffset, offsetLenFourHundred)));
			bitOffset = bitOffset + offsetLenFourHundred;

			builder.setFCS(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, offsetLenThirtyTwo)));
			bitOffset = bitOffset + offsetLenThirtyTwo;

		}catch(BufferException  e) {
			LOG.debug("Exception while decoding LACP PDU  packet", e.getMessage());
		}

		return(builder);
	}
					
}
