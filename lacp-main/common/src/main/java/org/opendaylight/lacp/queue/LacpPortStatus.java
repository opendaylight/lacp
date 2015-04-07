package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer.MessageType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;

public class LacpPortStatus implements LacpPDUPortStatusContainer{
    private long swID;
    private int portID;
    private int portStatus;
    public static final int PORT_STATUS_UP = 1;
    public static final int PORT_STATUS_DOWN = 2; 
    private int portFeatures = 0;
    private InstanceIdentifier<NodeConnector> ncId;

      public LacpPortStatus(long swid, int i, int portStatus,int portFeatures, InstanceIdentifier<NodeConnector> ncId)
      {
        this.swID = swid;
        this.portID = i;
        this.portStatus = portStatus;
	this.portFeatures = portFeatures;
        this.ncId = ncId;
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
      public InstanceIdentifier<NodeConnector> getNodeConnectorInstanceId()
      {
            return ncId;
      }

      public MessageType getMessageType(){
	  return LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG;
      }

}

