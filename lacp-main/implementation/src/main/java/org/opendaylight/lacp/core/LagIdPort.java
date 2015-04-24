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

public class LagIdPort implements Comparable<LagIdPort> {
	final int portPriority;
	final short portNumber;
	private static final Logger log = LoggerFactory.getLogger(LagIdPort.class);
	
	
	public LagIdPort(int port_priority, short port_number) {
		super();
		this.portPriority = port_priority;
		this.portNumber = port_number;
	}

	public LagIdPort(LagIdPort arg0) {
		this(arg0.portPriority,arg0.portNumber);
	}	
	
	
	
	public int getPort_priority() {
		return portPriority;
	}

	public short getPort_number() {
		return portNumber;
	}

	@Override
	public int compareTo(LagIdPort arg0) {
		int val1, val2;
		if (arg0 == null)
			return -1;
		val1 = this.portPriority & 0xffffffff;
		val2 = arg0.portPriority & 0xffffffff;
		if (val1 < val2)
			return -1;
		else if (val1 > val2 )
			 return 1;
		val1 = this.portNumber & 0xffff;
		val2 = arg0.portNumber & 0xffff;
		
		if (val1 < val2)
			return -1;
		else if (val1 > val2)
			return 1;		
		return 0;
	}

}
