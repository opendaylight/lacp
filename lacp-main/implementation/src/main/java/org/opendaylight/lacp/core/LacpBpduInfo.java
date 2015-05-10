/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.core;

import java.util.Date;
import java.util.Arrays;
 
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.queue.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.math.BigInteger;


public class LacpBpduInfo implements LacpPDUPortStatusContainer {
	
	private static final Logger LOG = LoggerFactory.getLogger(LacpBpduInfo.class);
	static final int LACP_BPDU_TYPE= 0;
	static final int LACP_MARK_REQUEST = 1;
	static final int LACP_MARK_RESPONSE = 2;
	
	private long swId;
	private short portId;
	private int type;
	private LacpBpduSysInfo actorSystemInfo;
	private LacpBpduSysInfo partnerSystemInfo;
	private short collectorMaxDelay;
	private Date receivedDate;
	private NodeConnectorRef nodeConnRef;
        public MessageType getMessageType(){
         	return LacpPDUPortStatusContainer.MessageType.LACP_PDU_MSG;
        }

	public LacpBpduInfo(long swId,short portId, LacpBpduSysInfo actor, LacpBpduSysInfo partner, short delay) {
		super();
		this.type = LACP_BPDU_TYPE;
		this.swId = swId;
		this.portId = portId;
		actorSystemInfo = new LacpBpduSysInfo(actor);
		partnerSystemInfo = new LacpBpduSysInfo(partner);
		this.collectorMaxDelay = delay;
		receivedDate = new Date();
		nodeConnRef = null;
	}

	public LacpBpduInfo() {
		super();
		this.swId = 0;
		this.portId = 0;
		this.type = LACP_BPDU_TYPE;
		actorSystemInfo = new LacpBpduSysInfo();
		partnerSystemInfo = new LacpBpduSysInfo();
		this.collectorMaxDelay = 0;
		receivedDate = new Date();
		nodeConnRef = null;
	}

	public LacpBpduInfo(LacpPacketPdu pktPdu){
		nodeConnRef = pktPdu.getIngressPort();
		if(nodeConnRef == null){
			LOG.error("LacpBpduInfo constructor, nodeConnRef is null");
		}
		this.swId=NodePort.getSwitchId(nodeConnRef);
		this.portId=NodePort.getPortId(nodeConnRef);
		this.type = LACP_BPDU_TYPE;
		setActorInfoFromPkt(pktPdu.getActorInfo());
		setPartnerInfoFromPkt(pktPdu.getPartnerInfo());
		this.setCollectorMaxDelay((pktPdu.getCollectorMaxDelay()).shortValue());
		receivedDate = new Date();
		LOG.info("In LacpBpduInfo constructor -  after converting LacpPacketPdu to LacpBpduInfo the values are = {}", this.toString());

        }

	public NodeConnectorRef getNCRef(){
		return nodeConnRef;
	}

	public void setActorInfoFromPkt(ActorInfo actInfo){
		final byte[] nodeSysAddr;
		short portNum = actInfo.getPort().shortValue();
		byte  portState = (byte)actInfo.getState().shortValue();
		int portPri = actInfo.getPortPriority().intValue();
		short nodeKey = actInfo.getKey().shortValue();
		nodeSysAddr = HexEncode.bytesFromHexString(actInfo.getSystemId().getValue());
		int sysPri =  actInfo.getSystemPriority().intValue();
                actorSystemInfo = new LacpBpduSysInfo(sysPri, nodeSysAddr, nodeKey, portPri, portNum, portState);
	}

	public void setPartnerInfoFromPkt(PartnerInfo partInfo){
		final byte[] nodeSysAddr;
		short portNum = partInfo.getPort().shortValue();
		byte  portState = (byte)partInfo.getState().shortValue();
		int portPri = partInfo.getPortPriority().intValue();
		short nodeKey = partInfo.getKey().shortValue();
		nodeSysAddr = HexEncode.bytesFromHexString((partInfo.getSystemId().getValue()));
		int sysPri =  partInfo.getSystemPriority().intValue();
                partnerSystemInfo = new LacpBpduSysInfo(sysPri, nodeSysAddr, nodeKey, portPri, portNum, portState);
	}

	public long getSwId() {
		return swId;
	}


	public void setSwId(long swId) {
		this.swId = swId;
	}


	public short getPortId() {
		return portId;
	}


	public void setPortId(short portId) {
		this.portId = portId;
	}


	public LacpBpduSysInfo getActorSystemInfo() {
		return actorSystemInfo;
	}


	public void setActorSystemInfo(LacpBpduSysInfo actorSystemInfo) {
		this.actorSystemInfo = actorSystemInfo;
	}


	public LacpBpduSysInfo getPartnerSystemInfo() {
		return partnerSystemInfo;
	}


	public void setPartnerSystemInfo(LacpBpduSysInfo partnerSystemInfo) {
		this.partnerSystemInfo = partnerSystemInfo;
	}


	public short getCollectorMaxDelay() {
		return collectorMaxDelay;
	}


	public void setCollectorMaxDelay(short collectorMaxDelay) {
		this.collectorMaxDelay = collectorMaxDelay;
	}




	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
        if (this == obj){
            return true;
	}
        if (!super.equals(obj)){
            return false;
	}
        if (!(obj instanceof LacpBpduInfo)){
            return false;
	}
        LacpBpduInfo other = (LacpBpduInfo) obj;
        if (this.swId != other.swId){
        	return false;
	}
        if (this.portId != other.portId){
        	return false;
	}
        if (this.type != other.type){
        	return false;
	}
        if (!this.actorSystemInfo.equals(other.actorSystemInfo)){
        	return false;
	}
        if (!this.partnerSystemInfo.equals(other.partnerSystemInfo)){
        	return false;
	}
        if (this.collectorMaxDelay != other.collectorMaxDelay){
        	return false;
	}
        if (!this.receivedDate.equals(other.receivedDate)){
        	return false;
	}
        return true;	
	}


	@Override
	public int hashCode() {
	/* Update Prime Number */
        final int prime = 1121;
        int result = super.hashCode();
        result = prime * result + (int) this.swId;
        result = prime * result + this.portId;
        result = prime * result + this.actorSystemInfo.hashCode();
        result = prime * result + this.partnerSystemInfo.hashCode();
        result = prime * result + this.collectorMaxDelay;
        result = prime * result + this.receivedDate.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "LacpBpduInfo [swId=" + (swId) + ", portId=" + String.format("%04x",portId) + ", type="
				+ String.format("%02x",type) + ", actorSystemInfo=" + actorSystemInfo
				+ ", partnerSystemInfo=" + partnerSystemInfo + ", collectorMaxDelay=" + String.format("%05x",collectorMaxDelay)
				+ ", receivedDate=" + receivedDate + "]";
	}
}
