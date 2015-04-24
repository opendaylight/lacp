/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.core;

public class LagId implements Comparable<LagId> {
	
	final LagIdElem highSystem;
	final LagIdElem lowSystem;
	
	public final byte LAGID_MAC_NOTFOUND = 0;
	public final byte LAGID_MAC_HGIHSYSTEM = 1;
	public final byte LAGID_MAC_LOWSYSTEM = 2;
	
	public LagId(LagIdElem elemA, LagIdElem elemB) 
	{
		super();
		
		int result = elemA.compareTo(elemB);
		highSystem = (result > 0) ? (new LagIdElem(elemA)) : ( new LagIdElem(elemB));
		lowSystem = (result <= 0) ? (new LagIdElem(elemA)) : ( new LagIdElem(elemB));
		
	}

	public LagId(LagId arg0) {
		this(arg0.highSystem, arg0.lowSystem);
	}
	
	public boolean isNeighborFound() {
		if (highSystem.isNeighborFound() && lowSystem.isNeighborFound()){
			return true;
		}
		return false;
	}
	
	public byte isMacAddressInLagId(byte[] macAddr) {
		if (highSystem.isMacAddrEqual(macAddr))
			return LAGID_MAC_HGIHSYSTEM;
		else if (lowSystem.isMacAddrEqual(macAddr))
			return LAGID_MAC_LOWSYSTEM;
		return LAGID_MAC_NOTFOUND;
	}
	
	
	public int compareToPartial(LagId arg0) {
		int result;
		result = this.highSystem.compareToPartial(arg0.highSystem);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		result = this.lowSystem.compareToPartial(arg0.lowSystem);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;		
		return 0;		
	}
	
	@Override
	public int compareTo(LagId arg0) {
		int result;
		result = this.highSystem.compareTo(arg0.highSystem);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		result = this.lowSystem.compareTo(arg0.lowSystem);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;		
		return 0;
	}

	public String MacString(byte[] content) {
		if (content!=null)
		return (String.format("%2x:%2x:%2x:%2x:%2x:%2x", content[0],content[1],content[2],content[3],
				content[4],content[5]));
		else
			return ("No MAC");
		
	}
	@Override
	public String toString() {
		String result="";
		result+= String.format("HIGH SYS(SKP) : SYS_PRI=%x, SYS_MAC=%s, KEY = %x, PORT_PRI=%x, PORT_NUM=%x\n",
				this.highSystem.system.getSys_priority(), MacString(this.highSystem.system.getSys_mac()), this.highSystem.key,
				this.highSystem.port.portPriority, this.highSystem.port.portNumber);
		result+= String.format("LOW  SYS(TLQ) : SYS_PRI=%x, SYS_MAC=%s, KEY = %x, PORT_PRI=%x, PORT_NUM=%x\n",
				this.lowSystem.system.getSys_priority(), MacString(this.lowSystem.system.getSys_mac()), this.lowSystem.key,
				this.lowSystem.port.portPriority, this.lowSystem.port.portNumber);		
		return result;
	}
}
