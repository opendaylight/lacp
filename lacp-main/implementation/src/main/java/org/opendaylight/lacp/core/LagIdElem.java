/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LagIdElem implements Comparable<LagIdElem> {

	private static final Logger log = LoggerFactory.getLogger(LagIdElem.class);
	final LagIdSys system;   // System MAC + Priority
	final short  	 key;   // Operational Key
	final LagIdPort port;  // Port Parameter Number + Priority	
	
	
	public LagIdElem(int sys_priority, byte[] sys_mac, short key, int port_priority, short port_number) {
		super();
		log.debug("Entering LagIdElem constructor");
		this.system = new LagIdSys(sys_priority, sys_mac);
		this.key = key;
		this.port = new LagIdPort(port_priority, port_number);
		log.debug("LagIdElem LagId is ={}",port.toString());
		log.debug("Exiting LagIdElem constructor");
	}
	
	public LagIdElem(LagIdSys system, short key, LagIdPort port) {
		this(system.sysPriority, system.sysMacAddress, key, port.portPriority, port.portNumber);
	}

	public LagIdElem(LagIdElem arg0) {
		this(arg0.system, arg0.key, arg0.port);
	}
	
	public boolean isNeighborFound() {
		return this.system.isNeighborFound();
	}

	public boolean isMacAddrEqual(byte[] macAddr) {
		
		return this.system.isMacAddrEqual(macAddr);
	}
	/* Comparison with data except port information */
	
	public int compareToPartial(LagIdElem arg0) {
		int val1,val2;
		int result = this.system.compareTo(arg0.system);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		val1 = this.key & 0xffff;
		val2 = arg0.key & 0xffff;
		if (val1 < val2)
			return -1;
		else if (val1 > val2)
			return 1;
		return 0;		
	}
	
	@Override
	public int compareTo(LagIdElem arg0) {
		// TODO Auto-generated method stub
		int val1,val2;
		int result = this.system.compareTo(arg0.system);
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		val1 = this.key & 0xffff;
		val2 = arg0.key & 0xffff;
		if (val1 < val2)
			return -1;
		else if (val1 > val2)
			return 1;
		result = this.port.compareTo(arg0.port);
			if (result < 0)
				return -1;
			else if (result > 0)
				return 1;
		return 0;
	}

	@Override
	public String toString() {
		return "LagIdElem [system=" + system + ", key=" + key + ", port="
				+ port + "]";
	}

}
