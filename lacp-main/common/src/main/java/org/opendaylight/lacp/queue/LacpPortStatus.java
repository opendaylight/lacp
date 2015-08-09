/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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
    private boolean resetFlag;
    private InstanceIdentifier<NodeConnector> ncId;

      public LacpPortStatus(long swid, int i, int portStatus, InstanceIdentifier<NodeConnector> ncId, boolean rFlag)
      {
        this.swID = swid;
        this.portID = i;
        this.portStatus = portStatus;
        this.ncId = ncId;
        this.resetFlag = rFlag;
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
    public boolean getPortResetStatus()
    {
        return this.resetFlag;
    }
    public InstanceIdentifier<NodeConnector> getNodeConnectorInstanceId()
    {
        return ncId;
    }

      public MessageType getMessageType(){
	  return LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG;
      }

}

