package org.opendaylight.lacp.queue;

import java.util.ArrayList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.*;


public  class LacpTxQueue {
    private static final ArrayList<LacpQueue<LacpPacketPdu>>  LacpTxQueueArr = 
        new ArrayList<LacpQueue<LacpPacketPdu>>();					
    private static final LacpTxQueue instance = new LacpTxQueue();

    public static enum QueueType{
        LACP_TX_NTT_QUEUE,
        LACP_TX_PERIODIC_QUEUE,
        LACP_TX_QUEUE_MAX
    }

    protected LacpTxQueue(){

	//LACP_TX_NTT_QUEUE
	LacpQueue<LacpPacketPdu> lacpQ = new LacpQueue<LacpPacketPdu>();
	LacpTxQueueArr.add(lacpQ);

	//LACP_TX_PERIODIC_QUEUE
	lacpQ = new LacpQueue<LacpPacketPdu>();
	LacpTxQueueArr.add(lacpQ);
		
    }


    /*
     * The LacpTxQueue is a singleton class.
     */  
    public static LacpTxQueue getLacpTxQueueInstance(){
        return instance;
    }

    private int getQueueId(QueueType queueType){
        int queueId = 0;
        switch(queueType){
            case LACP_TX_NTT_QUEUE:
                queueId = 0;
                break;

            case LACP_TX_PERIODIC_QUEUE:
                queueId = 1;
                break;

            default:
                queueId = -1;
                break;
        }
        return queueId;
    }

    /*
     * Validity method to check if the queue is created or not.
     */ 
    public boolean isLacpQueuePresent(QueueType queueType){
        boolean result = false;

        if((!LacpTxQueueArr.isEmpty()) && 
                LacpTxQueueArr.get(getQueueId(queueType)) != null){
            result = true;
        }           
        return result;
    }

    /*
     * The utility method enqueues the data to the Tx queue.
     * It creates the queue if it is not created.
     */  	

    public boolean enqueue(QueueType queueType, LacpPacketPdu pdu){
        boolean result = false;
        synchronized(LacpTxQueueArr.get(getQueueId(queueType))){
            if(isLacpQueuePresent(queueType)){
                LacpTxQueueArr.get(getQueueId(queueType)).enqueue(pdu);
                result = true;
            }
        }
        return result;
    }

    /*
     * Dequeues the data from the Tx queue
     */  
    public LacpPacketPdu dequeue(QueueType queueType){
        LacpPacketPdu obj = null;
        LacpQueue<LacpPacketPdu> lacpTxQueueId = LacpTxQueueArr.get(getQueueId(queueType));

        if(lacpTxQueueId != null){
            synchronized(LacpTxQueueArr.get(getQueueId(queueType))){
                obj = LacpTxQueueArr.get(getQueueId(queueType)).dequeue();
            }
        }
        return obj;
    }

    /*
     * Adds a new Transmit queue 
     */ 
    public boolean addLacpQueue(QueueType queueType){
        boolean result = true;
        LacpQueue<LacpPacketPdu> lacpTxQueueId = new LacpQueue<LacpPacketPdu>();

        LacpTxQueueArr.add(getQueueId(queueType), lacpTxQueueId);
        return result;
    }

    /*
     * Deletes all the enties in the Tx queue.
     * It also cleans the arraylist entry for the 	
     * corresponding PDU queue.
     */ 
    public boolean deleteLacpQueue(QueueType queueType){
        boolean result = false;

        if(isLacpQueuePresent(queueType)){
            synchronized(LacpTxQueueArr.get(getQueueId(queueType))){
                LacpTxQueueArr.get(getQueueId(queueType)).remove();
                LacpTxQueueArr.remove(getQueueId(queueType));
                result = true;
            }
        }

        return result;
    }

    /*
     * Utility method to find the size of the queue
     */  

    public long getLacpQueueSize(QueueType queueType){

        long size = 0;

        if(LacpTxQueueArr.get(getQueueId(queueType)) != null){
            size = LacpTxQueueArr.get(getQueueId(queueType)).size();
        }
        return size;
    }

}
