/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.timer;

import org.opendaylight.lacp.timer.Utils.timerWheeltype;

public class TimerExpiryMessage {
	int portid;
        long switchId;
	timerWheeltype wType;

	public TimerExpiryMessage(long swid,int pid,timerWheeltype wt){
                switchId = swid;
		portid = pid;
		wType = wt;
	}
	public void setPortID (int pid){
		portid = pid;
	}

	public int getPortID(){
		return portid;
	}

        public void setSwitchID(long swid){
                switchId = swid;
        }

        public long getSwitchID(){
                return switchId;
        }

	public timerWheeltype getTimerWheelType(){
		return wType;
	}

	public void setTimerWheelType(timerWheeltype wt){
		wType = wt;
	}

}
