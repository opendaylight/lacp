package org.opendaylight.lacp.queue;

public  class LacpRxQueue extends LacpQueue {
    private static LacpQueue<LacpRawPacket> lacpRxQueueId;

    protected LacpRxQueue(){
    }

    	/*
	 * The LacpTxQueue is a singleton class.
         */
    public static LacpQueue<LacpRawPacket> getLacpRxQueueId(){
            if (lacpRxQueueId == null) {
                    synchronized (LacpPDUQueue.class) {
                                    lacpRxQueueId = new LacpQueue<LacpRawPacket>();
                    }
            }
            return lacpRxQueueId;
    }

}
