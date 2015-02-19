package org.opendaylight.lacp.timer;

public class BasePortTimerRegister {
	private int portID;
	private long systemID;
	
	BasePortTimerRegister(int pid, int sysid){
		portID = pid;
		systemID = sysid;
	}
	
	void setPortID(int pid){
		portID = pid;
	}
	
	void setSystemID (long sysid){
		systemID = sysid;
	}

        long getSystemID(){
                return systemID;
        }
	
	int getPortID(){
		return portID;
	}
}
