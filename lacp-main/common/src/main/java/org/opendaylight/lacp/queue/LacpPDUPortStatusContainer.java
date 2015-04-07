package org.opendaylight.lacp.queue;

public interface LacpPDUPortStatusContainer {
    public enum MessageType {
        LACP_PDU_MSG,
        LACP_PORT_STATUS_MSG,
        LACP_NODE_DEL_MSG
    }
    
    public MessageType getMessageType();
}
