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
