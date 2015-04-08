package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;

public class LacpNodeNotif implements LacpPDUPortStatusContainer
{
    public MessageType getMessageType()
    {
        return LacpPDUPortStatusContainer.MessageType.LACP_NODE_DEL_MSG;
    }
}

