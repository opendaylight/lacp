package org.opendaylight.lacp.queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

public  class LacpRxQueue {
    private static final LacpQueue<PacketReceived> lacpRxQueueId = new LacpQueue<PacketReceived>();	

    protected LacpRxQueue(){
    }

    /*
     * The LacpTxQueue is a singleton class.
     */
    public static LacpQueue<PacketReceived> getLacpRxQueueId(){
	return lacpRxQueueId;
    }

}
