/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.core;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LagIdSys implements Comparable<LagIdSys> {

	private static final Logger log = LoggerFactory.getLogger(LagIdSys.class);
	final int sysPriority;
	final byte[] sysMacAddress;
	
	public LagIdSys(int sys_priority, byte[] sys_mac) {
		super();
		this.sysPriority = sys_priority;
		this.sysMacAddress = Arrays.copyOf(sys_mac, LacpConst.ETH_ADDR_LEN);;
	}

	public LagIdSys(LagIdSys arg0) {
		this(arg0.sysPriority, arg0.sysMacAddress);
	}	
	
	public int getSys_priority() {
		return sysPriority;
	}

	public byte[] getSys_mac() {
		return sysMacAddress;
	}

	public boolean isNeighborFound() {
		if (Arrays.equals(this.sysMacAddress,LacpConst.NULL_MAC_ADDRESS )){
			return false;
		}
		return true;
	}
	
	public boolean isMacAddrEqual(byte[] macAddr) {
		if (Arrays.equals(this.sysMacAddress,macAddr)){
			return true;
		}
		return false;
		
	}
	@Override
	public int compareTo(LagIdSys arg0) {
		int val1, val2;
		if (arg0 == null)
			return -1;
		if (this.sysPriority < arg0.sysPriority)
			return -1;
		else if (this.sysPriority > arg0.sysPriority)
			return 1;
		if (arg0.sysMacAddress == null)
			return -1;
		if (Arrays.equals(arg0.sysMacAddress,LacpConst.NULL_MAC_ADDRESS ))
			return -1;
		if (Arrays.equals(this.sysMacAddress,LacpConst.NULL_MAC_ADDRESS ))
			return 1;		
		for (int i = 0; i < this.sysMacAddress.length; i++) {
			/* byte f0 < 0 :  we need to compare integer value instead of byte */
			val1 = this.sysMacAddress[i] & 0xff;
			val2 = arg0.sysMacAddress[i] & 0xff;
			if (val1 < val2)
				return (-1);
			else if (val1 > val2)
				return (1);
		}
		return 0;
	}
		
	
}
