package org.opendaylight.lacp.queue;

public  class LacpRxQueue extends LacpQueue {
    private static final LacpQueue<LacpRawPacket> lacpRxQueueId = new LacpQueue<LacpRawPacket>();

    protected LacpRxQueue(){
    }

    /*
     * The LacpTxQueue is a singleton class.
     */
    public static LacpQueue<LacpRawPacket> getLacpRxQueueId(){
        return lacpRxQueueId;
    }

}
