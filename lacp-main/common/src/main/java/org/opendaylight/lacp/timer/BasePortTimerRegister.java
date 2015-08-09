/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.timer;

public class BasePortTimerRegister {
	private short portID;
	private long systemID;

	public BasePortTimerRegister(short pid, long sysid){
		portID = pid;
		systemID = sysid;
	}

	public void setPortID(short pid){
		portID = pid;
	}

	public void setSystemID (long sysid){
		systemID = sysid;
	}

        public long getSystemID(){
                return systemID;
        }

	public int getPortID(){
		return portID;
	}
}
