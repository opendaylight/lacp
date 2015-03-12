package org.opendaylight.lacp.queue;

import java.util.ArrayList;

public  class LacpTxQueue extends LacpQueue {
    private static final ArrayList<LacpQueue<LacpPDU>>  LacpTxQueueArr = 
        new ArrayList<LacpQueue<LacpPDU>>();					
    private static final LacpTxQueue instance = new LacpTxQueue();

    public static final int LACP_TX_NTT_QUEUE = 0;
    public static final int LACP_TX_PERIODIC_QUEUE = 1;

    protected LacpTxQueue(){
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
    public boolean enqueue(int queueId, LacpPDU pdu){
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
    public LacpPDU dequeue(int queueId){
        LacpPDU obj = null;
        LacpQueue<LacpPDU> lacpTxQueueId = LacpTxQueueArr.get(queueId);

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
        LacpQueue<LacpPDU> lacpTxQueueId = new LacpQueue<LacpPDU>();

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
