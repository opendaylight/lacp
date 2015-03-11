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
