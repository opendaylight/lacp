package org.opendaylight.lacp.timer;

import org.opendaylight.lacp.timer.Utils.timerWheeltype;

public class TimerExpiryMessage {
	int portid;
	timerWheeltype wType;
	
	TimerExpiryMessage(int pid,timerWheeltype wt){
		portid = pid;
		wType = wt;
	}
	public void setPortID (int pid){
		portid = pid;
	}
	
	public int getPortID(){
		return portid;
	}
	
	public timerWheeltype getTimerWheelType(){
		return wType;
	}
	
	public void setTimerWheelType(timerWheeltype wt){
		wType = wt;
	}
	
}
