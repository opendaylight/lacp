package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer.MessageType;

public class LacpPortStatus implements LacpPDUPortStatusContainer{
    private long swID;
    private int portID;
    private int portStatus;
    public static final int PORT_STATUS_UP = 1;
    public static final int PORT_STATUS_DOWN = 2; 

      public LacpPortStatus(long swid, int i, int portStatus) {
        this.swID = swid;
        this.portID = i;
        this.portStatus = portStatus;
      }

      long getSwID(){
              return swID;
      }

      int getPortID(){
              return portID;
      }

      int getPortStatis(){
              return portStatus;
      }  
      
	  public MessageType getMessageType(){
		  return LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG;
	  }

}

