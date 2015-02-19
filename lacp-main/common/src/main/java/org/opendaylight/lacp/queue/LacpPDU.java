package org.opendaylight.lacp.queue;


class LacpPDU implements LacpPDUPortStatusContainer{
	private long swID;
	private int portID;

	  public LacpPDU(long swid, int i) {
	    this.swID = swid;
	    this.portID = i;
	  }

	  long getSwID(){
		  return swID;
	  }
	  
	  int getPortID(){
		  return portID;
	  }
	  
	  public MessageType getMessageType(){
		  return LacpPDUPortStatusContainer.MessageType.LACP_PDU_MSG;
	  }
	
}
