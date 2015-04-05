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
//import org.opendaylight.l2switch.packethandler.decoders.utils.BitBufferHelper;
//import org.opendaylight.l2switch.packethandler.decoders.utils.BufferException;
//import org.opendaylight.l2switch.packethandler.decoders.utils.NetUtils;
//import org.opendaylight.l2switch.packethandler.decoders.utils.HexEncode;
//import org.opendaylight.lacp.Utils.BitBufferHelper;
//import org.opendaylight.lacp.Utils.BufferException;
//import org.opendaylight.lacp.Utils.HexEncode;

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

	private final static Logger _logger = LoggerFactory.getLogger(PduQueueHandler.class);

	public void checkQueue(){

	   boolean IsnewNode = false;
	   boolean HasPktArrvd = false;
	   PacketReceived packetReceived = null;
	   System.out.println("checkQueue ");
	   /* while (!IsnewNode)
	   { */
		// Get the Node specific PDU Queue Instance
		LacpPDUQueue lacpPduQ = LacpPDUQueue.getLacpPDUQueueInstance();

		//LacpTimerQueue lacpTmrQ = LacpTimerQueue.getLacpTimerQueueInstance();

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
				Thread.sleep(100);
			}catch( InterruptedException e ) {
				_logger.debug("PduQueueHandler: Interrupted Exception ", e.getMessage());
			}
		}

		// Decode the received Packet.
		LacpPacketPduBuilder builder = decodeLacp(packetReceived);


		System.out.println("PduQueueHandler : returned from decodeLACP ");
		LacpPacketPdu lacpPacketPdu = builder.build();
		ActorInfo actorInfo = builder.getActorInfo();
		System.out.println("PduQueueHandler: actorInfo" + actorInfo);
		//long sid = Long.valueOf(macToString(actorInfo.getSystemId()));
		long sid = NodePort.getSwitchId(builder.getIngressPort());
		System.out.println("PduQueueHandler: sid" + sid);
		// Check if this is the first LACP PDU received for the Node.
		IsnewNode = !(lacpPduQ.isLacpQueuePresent(sid));
		System.out.println("PduQueueHandler : IsnewNode - " + IsnewNode + " lacpPduQ" + lacpPduQ.isLacpQueuePresent(sid));

		if (IsnewNode)
                {
                	System.out.println("PduDecoderProcessor: before spwaning rsm create");
                      	RSMManager instance = RSMManager.getRSMManagerInstance();
			LacpSystem lacpSystem = LacpSystem.getLacpSystem();
			LacpNodeExtn lacpNodeExtn = lacpSystem.getLacpNode(sid);
                       	instance.createRSM(lacpNodeExtn);
			System.out.println("PduDecoderProcessor: After createRSM");

			//create the timer queue
			//lacpTmrQ.addLacpQueue(sid);

                }
		// Enqueue the decoded LACP Packet to LACP Packet PDU Queue
		LacpBpduInfo lacpBpduInfo = new LacpBpduInfo(lacpPacketPdu);
		System.out.println("PduDecoderProcessor: Before enqueue");
		lacpPduQ.enqueue(sid, lacpBpduInfo);
		System.out.println("PduDecoderProcessor: After enqueue");
	   //}

	   return;

	}
			

	// Delete the LACP Packet PDU Queue when the node is deleted.
	public boolean deleteQueue(long switchId) {
		boolean Isdeleted = false;

                System.out.println("Inside deleteQueue");
		LacpPDUQueue lacpPduQ = LacpPDUQueue.getLacpPDUQueueInstance();
		Isdeleted = lacpPduQ.deleteLacpQueue(switchId);
		return(Isdeleted);

	}

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


	public static String bytesToString(byte[] bytes) {

    		if(bytes == null) {
      			return "null";
    		}

    		String ret = "";
    		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < bytes.length; i++) {
      			short u8byte = (short) (bytes[i] & 0xff);
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
                System.out.println("Inside PduQueueHandler");


		int bitOffset = 0;
		byte[] data = packetReceived.getPayload();

		LacpPacketPduBuilder builder = new LacpPacketPduBuilder();
		ActorInfoBuilder actorbuilder = new ActorInfoBuilder();
		PartnerInfoBuilder partnerbuilder = new PartnerInfoBuilder();
		try {

			builder.setIngressPort(packetReceived.getIngress());
			builder.setDestAddress(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, 48))));
			bitOffset = bitOffset + 48;

			System.out.println("builder.getDestAddress - " + builder.getDestAddress());

			builder.setSrcAddress(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, 48))));
			bitOffset = bitOffset + 48;
			System.out.println("builder.getSrcAddress - " + builder.getSrcAddress());

			builder.setLenType(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			builder.setSubtype(SubTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;

			builder.setVersion(VersionValue.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;


			actorbuilder.setTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;

			actorbuilder.setInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			actorbuilder.setSystemPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			actorbuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, 48))));
			bitOffset = bitOffset + 48;

			actorbuilder.setKey(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			actorbuilder.setPortPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			actorbuilder.setPort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			actorbuilder.setState(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			actorbuilder.setReserved(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			actorbuilder.setReserved1(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			builder.setActorInfo(actorbuilder.build());

			partnerbuilder.setTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;

			partnerbuilder.setInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			partnerbuilder.setSystemPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			partnerbuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, bitOffset, 48))));
			bitOffset = bitOffset + 48;

			System.out.println("partnerbuilder.setSystemId : " + partnerbuilder.getSystemId());

			partnerbuilder.setKey(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			partnerbuilder.setPortPriority(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			partnerbuilder.setPort(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			partnerbuilder.setState(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			partnerbuilder.setReserved(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			partnerbuilder.setReserved1(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			builder.setPartnerInfo(partnerbuilder.build());

			builder.setCollectorTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;

			builder.setCollectorInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			builder.setCollectorMaxDelay(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			BigInteger bi = new BigInteger(BitBufferHelper.getBits(data, bitOffset, 32));
			builder.setCollectorReserved(bi);
			bitOffset = bitOffset + 32;

			builder.setCollectorReserved1(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, 16)));
			bitOffset = bitOffset + 16;

			builder.setTerminatorTlvType(TlvTypeOption.forValue(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 8))));
			bitOffset = bitOffset + 8;

			builder.setTerminatorInfoLen(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
			bitOffset = bitOffset + 8;

			builder.setTerminatorReserved(bytesToString(BitBufferHelper.getBits(data, bitOffset, 400)));
			bitOffset = bitOffset + 400;

			builder.setFCS(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, 32)));
			bitOffset = bitOffset + 32;

		}catch(BufferException  e) {
			_logger.debug("Exception while decoding LACP PDU  packet", e.getMessage());
		}

		System.out.println("Exiting decodelacp");
		return(builder);
	}


					
}

