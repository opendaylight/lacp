package org.opendaylight.lacp.timer;

public class BasePortTimerRegister {

	private int portID;
	private int systemID;
	
	BasePortTimerRegister(int pid, int sysid){
		portID = pid;
		systemID = sysid;
	}
	
	void setPortID(int pid){
		portID = pid;
	}
	
	void setSystemID (int sysid){
		systemID = sysid;
	}
	
	int getPortID(){
		return portID;
	}
}
