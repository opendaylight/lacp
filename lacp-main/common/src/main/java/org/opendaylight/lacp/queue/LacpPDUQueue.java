package org.opendaylight.lacp.queue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public  class LacpPDUQueue {

    private static final Map<Long, LacpDeque<LacpPDUPortStatusContainer>> LACP_PDU_QUEUE_MAP =
        new ConcurrentHashMap<Long,LacpDeque<LacpPDUPortStatusContainer>>();
    private static final LacpPDUQueue INSTANCE = new LacpPDUQueue();

    protected LacpPDUQueue(){
    }

    /*
     * The LacpPDUqueue is a singleton class.
     */	
    public static LacpPDUQueue getLacpPDUQueueInstance(){
        return INSTANCE;
    }

    /*
     * Validity method to check if the queue is created or not.
     */
    public boolean isLacpQueuePresent(long switchId){
        boolean result = false;

        if(LACP_PDU_QUEUE_MAP.get(switchId) != null){
            result = true;
        }
        return result;
    }

    /*
     * The utility method enqueues the data to the PDU queue.
     * It creates the queue if it is not created.
     */	
    public boolean enqueue(Long switchId, LacpPDUPortStatusContainer pdu){
        boolean result = false;
        LacpDeque<LacpPDUPortStatusContainer> lacpPDUQueueId;

        synchronized(this.LACP_PDU_QUEUE_MAP.get(switchId)){
            LACP_PDU_QUEUE_MAP.get(switchId).enqueue(pdu);
            result = true;
        }

        return result;
    }


    /*
     * Dequeues the data from the PDU queue
     */ 	
    public LacpPDUPortStatusContainer dequeue(Long switchId){
        LacpPDUPortStatusContainer obj = null;
        LacpDeque<LacpPDUPortStatusContainer> lacpPDUQueueId = LACP_PDU_QUEUE_MAP.get(switchId);

        if(lacpPDUQueueId != null){
            synchronized(this.LACP_PDU_QUEUE_MAP.get(switchId)){
                obj = LACP_PDU_QUEUE_MAP.get(switchId).dequeue();
            }
        }
        return obj;
    }

    //Adds a new PDU queue	
    public boolean addLacpQueue(long switchId){
        boolean result = true;
        LacpDeque<LacpPDUPortStatusContainer> lacpPDUQueueId = LACP_PDU_QUEUE_MAP.get(switchId);

        if(lacpPDUQueueId == null){
            lacpPDUQueueId = new LacpDeque<LacpPDUPortStatusContainer>();
            LACP_PDU_QUEUE_MAP.put(switchId, lacpPDUQueueId);
        }
        return result;
    }

    /*
     * Deletes all the enties in the PDU queue.
     * It also cleans the hash map entry for the 
     * corresponding PDU queue.
     */ 	
    public boolean deleteLacpQueue(long switchId){
        boolean result = false;

        if(LACP_PDU_QUEUE_MAP.get(switchId) != null){
            synchronized(this.LACP_PDU_QUEUE_MAP.get(switchId)){
                LACP_PDU_QUEUE_MAP.get(switchId).remove();
                LACP_PDU_QUEUE_MAP.remove(switchId);
                result  = true;
            }
        }
        return result;
    }

    /*
     * The utility method enqueues the data in the head of the PDU queue.
     * It creates the queue if it is not created.
     */
    public boolean enqueueAtFront(Long switchId, LacpPDUPortStatusContainer pdu){
        boolean result = false;
        LacpDeque<LacpPDUPortStatusContainer> lacpPDUQueueId;

        synchronized(this.LACP_PDU_QUEUE_MAP.get(switchId)){
            LACP_PDU_QUEUE_MAP.get(switchId).addFirst(pdu);
            result = true;
        }

        return result;
    }

    public LacpPDUPortStatusContainer read(Long switchId){
        LacpPDUPortStatusContainer obj = null;
        LacpDeque<LacpPDUPortStatusContainer> lacpPDUQueueId = LACP_PDU_QUEUE_MAP.get(switchId);

        if(lacpPDUQueueId != null){
            synchronized(this.LACP_PDU_QUEUE_MAP.get(switchId)){
                obj = LACP_PDU_QUEUE_MAP.get(switchId).read();
            }
        }
        return obj;	
    }

    //Size of the queue
    public long getLacpQueueSize(long switchId){

        long size = 0;

        if(LACP_PDU_QUEUE_MAP.get(switchId) != null){
            size = LACP_PDU_QUEUE_MAP.get(switchId).size();
        }
        return size;
    }

}
