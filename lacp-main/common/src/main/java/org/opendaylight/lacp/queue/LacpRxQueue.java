package org.opendaylight.lacp.queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

public  class LacpRxQueue extends LacpQueue {
    private static LacpQueue<PacketReceived> lacpRxQueueId;

    protected LacpRxQueue(){
    }

    /*
     * The LacpTxQueue is a singleton class.
     */
    public static LacpQueue<PacketReceived> getLacpRxQueueId(){
	if (lacpRxQueueId == null) {
		synchronized (LacpPDUQueue.class) {
				lacpRxQueueId = new LacpQueue<PacketReceived>();
		}
	}
        return lacpRxQueueId;
    }

}
