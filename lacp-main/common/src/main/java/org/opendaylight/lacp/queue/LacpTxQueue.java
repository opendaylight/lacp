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

    public static final int LACP_TX_NTT_QUEUE = 0;
    public static final int LACP_TX_PERIODIC_QUEUE = 1;

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

    /*
     * Validity method to check if the queue is created or not.
     */ 
    public boolean isLacpQueuePresent(int queueId){
        boolean result = false;

        if((!LacpTxQueueArr.isEmpty()) && 
                LacpTxQueueArr.get(queueId) != null){
            result = true;
        }           
        return result;
    }

    /*
     * The utility method enqueues the data to the Tx queue.
     * It creates the queue if it is not created.
     */  	
    public boolean enqueue(int queueId, LacpPacketPdu pdu){
        boolean result = false;
        synchronized(LacpTxQueueArr.get(queueId)){
            if(isLacpQueuePresent(queueId)){
                LacpTxQueueArr.get(queueId).enqueue(pdu);
                result = true;
            }
        }
        return result;
    }

    /*
     * Dequeues the data from the Tx queue
     */  
    public LacpPacketPdu dequeue(int queueId){
        LacpPacketPdu obj = null;
        LacpQueue<LacpPacketPdu> lacpTxQueueId = LacpTxQueueArr.get(queueId);

        if(lacpTxQueueId != null){
            synchronized(LacpTxQueueArr.get(queueId)){
                obj = LacpTxQueueArr.get(queueId).dequeue();
            }
        }
        return obj;
    }

    /*
     * Adds a new Transmit queue 
     */ 
    public boolean addLacpQueue(int queueId){
        boolean result = true;
        LacpQueue<LacpPacketPdu> lacpTxQueueId = new LacpQueue<LacpPacketPdu>();

        LacpTxQueueArr.add(queueId, lacpTxQueueId);
        return result;
    }

    /*
     * Deletes all the enties in the Tx queue.
     * It also cleans the arraylist entry for the 	
     * corresponding PDU queue.
     */ 
    public boolean deleteLacpQueue(int queueId){
        boolean result = false;

        if(isLacpQueuePresent(queueId)){
            synchronized(LacpTxQueueArr.get(queueId)){
                LacpTxQueueArr.get(queueId).remove();
                LacpTxQueueArr.remove(queueId);
                result = true;
            } 
        }

        return result;
    }

    /*
     * Utility method to find the size of the queue
     */  
    public long getLacpQueueSize(int queueId){

        long size = 0;

        if(LacpTxQueueArr.get(queueId) != null){
            //System.out.println("The given queueId " + queueId + " is present in the LacpPDUqueueMap");
            size = LacpTxQueueArr.get(queueId).size();
        }
        return size;
    }

}
