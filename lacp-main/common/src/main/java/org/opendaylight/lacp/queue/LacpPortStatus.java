package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer.MessageType;

public class LacpPortStatus implements LacpPDUPortStatusContainer{
    private long swID;
    private int portID;
    private int portStatus;
    public static final int PORT_STATUS_UP = 1;
    public static final int PORT_STATUS_DOWN = 2; 
    private int portFeatures = 0;

      public LacpPortStatus(long swid, int i, int portStatus,int portFeatures) {
        this.swID = swid;
        this.portID = i;
        this.portStatus = portStatus;
	this.portFeatures = portFeatures;
      }

      public long getSwID(){
          return swID;
      }

      public int getPortID(){
          return portID;
      }

      public int getPortStatus(){
          return portStatus;
      }  
      public int getPortFeatures(){
	 return portFeatures;
      }
      public MessageType getMessageType(){
	  return LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG;
      }

}

