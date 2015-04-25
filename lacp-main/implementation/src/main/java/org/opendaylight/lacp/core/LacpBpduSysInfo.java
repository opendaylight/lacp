/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.core;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpBpduSysInfo {

	private static final Logger log = LoggerFactory.getLogger(LacpBpduSysInfo.class);
	private static final byte ETH_ADDR_LEN = 6;
	private static final byte SYSTEMINFO_SIZE = 15; 
	private int nodeSysPri;
	private byte[] nodeSysAddr;
	private short nodeKey;
	private int nodePortPri;
	private short nodePortNum;
	private byte nodePortState;

	public LacpBpduSysInfo() {
		super();
		this.nodeSysAddr = new byte[ETH_ADDR_LEN];
		this.nodeKey = 1;
		this.nodePortPri = (int)0x000000ff;
		this.nodeSysPri = (int)0x0000ffff;
		this.nodePortNum = 1;
		this.nodePortState = 1;
		Arrays.fill(this.nodeSysAddr, (byte)0);		
	}

	public LacpBpduSysInfo(int priority, byte[] macAddr, short key, int portPriority, short portNumber, byte portState) 
	{
		super();
		this.nodeSysAddr = Arrays.copyOf(macAddr, ETH_ADDR_LEN);
		this.nodeSysPri = priority;
		this.nodeKey = key;
		this.nodePortPri = portPriority;
		this.nodePortNum = portNumber;
		this.nodePortState = portState;
	}
		
	public LacpBpduSysInfo(LacpBpduSysInfo src) 
	{
		this();
		if (src!= null) {
			this.nodeSysAddr = Arrays.copyOf(src.nodeSysAddr, ETH_ADDR_LEN);
			this.nodeSysPri = src.nodeSysPri;
			this.nodeKey = src.nodeKey;
			this.nodePortPri = src.nodePortPri;
			this.nodePortNum = src.nodePortNum;
			this.nodePortState = src.nodePortState;		  
		}
	}		

	public byte[] serialize() {
		byte[] data = new byte[SYSTEMINFO_SIZE];
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.putInt(this.nodeSysPri);
		bb.put(this.nodeSysAddr);
		bb.putShort(this.nodeKey);
		bb.putInt(this.nodePortPri);
		bb.putShort(this.nodePortNum);
		bb.put(this.nodePortState);
		return data;
	}
		
	public LacpBpduSysInfo deserialize(ByteBuffer bb) {
		this.nodeSysPri = bb.getShort();
		this.nodeSysAddr = new byte[ETH_ADDR_LEN];
		bb.get(this.nodeSysAddr);
		this.nodeKey = bb.getShort();
		this.nodePortPri = bb.getShort();
		this.nodePortNum = bb.getShort();
		this.nodePortState = bb.get();
		return this;
	}

	public int getNodeSysPri() {
		return nodeSysPri;
	}

	public void setNodeSysPri(int nodeSystemPriority) {
		this.nodeSysPri = nodeSystemPriority;
	}

	public byte[] getNodeSysAddr() {
		return nodeSysAddr;
	}

	public void setNodeSysAddr(byte[] nodeSystemAddr) {
		this.nodeSysAddr = Arrays.copyOf(nodeSystemAddr, ETH_ADDR_LEN);
	}

	public short getNodeKey() {
		return nodeKey;
	}

	public void setNodeKey(short nodeKey) {
		this.nodeKey = nodeKey;
	}

	public int getNodePortPriority() {
		return nodePortPri;
	}

	public void setNodePortPri(int nodePortPriority) {
		this.nodePortPri = nodePortPriority;
	}

	public short getNodePortNum() {
		return nodePortNum;
	}

	public void setNodePortNum(short nodePortNumber) {
		this.nodePortNum = nodePortNumber;
	}

	public byte getNodePortState() {
		return nodePortState;
	}

	public void setNodePortState(byte nodePortState) {
		this.nodePortState = nodePortState;
	}
		
	@Override
	public boolean equals(Object obj) {

	        if (this == obj)
	            return true;
	        if (!super.equals(obj))
	            return false;
	        if (!(obj instanceof LacpBpduSysInfo))
	            return false;
	        LacpBpduSysInfo other = (LacpBpduSysInfo) obj;
	        if (this.nodeSysPri != other.nodeSysPri)
	        	return false;
	        if (this.nodeKey != other.nodeKey)
	        	return false;
	        if (this.nodePortPri != other.nodePortPri)
	        	return false;
	        if (this.nodePortNum != other.nodePortNum)
	        	return false;
	        if (this.nodePortState != other.nodePortState)
	        	return false;

	        if (!Arrays.equals(this.nodeSysAddr, other.nodeSysAddr))
	        	return false;
	        return true;				
	}

		@Override
		public int hashCode() {
			
			/* Update Prime Number */
	        final int prime = 1111;
	        int result = super.hashCode();
	        result = prime * result + this.nodeSysPri;
	          if (this.nodeSysAddr != null) {
	        	  String nodeSysAddrStr = LacpConst.toHex(this.nodeSysAddr); 
	        	  result = prime * result + nodeSysAddrStr.hashCode();
	          }
	        result = prime * result + this.nodeKey;
	        result = prime * result + this.nodePortPri;
	        result = prime * result + this.nodePortNum;
	        result = prime * result + this.nodePortState;
			return result;
		}
		
		@Override
		public String toString() {
			return "LacpBpduSysInfo [nodeSysPri="
					+ String.format("%04x", nodeSysPri) + ", nodeSysAddr="
					+ (nodeSysAddr!= null ? LacpConst.toHex(nodeSysAddr) : "NULL") + ", nodeKey=" 
					+ String.format("%04x", nodeKey)
					+ ", nodePortPri=" + String.format("%04x", nodePortPri)
					+ ", nodePortNum=" + String.format("%04x", nodePortNum) + ", nodePortState="
					+ String.format("%04x", nodePortState) + "]";
		}		
	}
