package org.opendaylight.lacp.queue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public  class LacpPDUQueue extends LacpQueue {

    private static final Map<Long, LacpQueue<LacpPDUPortStatusContainer>> LacpPDUQueueMap =
        new ConcurrentHashMap<Long,LacpQueue<LacpPDUPortStatusContainer>>();
    private static final LacpPDUQueue instance = new LacpPDUQueue();

    protected LacpPDUQueue(){
    }

    /*
     * The LacpPDUqueue is a singleton class.
     */	
    public static LacpPDUQueue getLacpPDUQueueInstance(){
        return instance;
    }

    /*
     * Validity method to check if the queue is created or not.
     */
    public boolean isLacpQueuePresent(long switchId){
        boolean result = false;

        if(LacpPDUQueueMap.get(switchId) != null){
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
        LacpQueue<LacpPDUPortStatusContainer> lacpPDUQueueId;

        if(!isLacpQueuePresent(switchId)){
            //System.out.println("Adding new queue and new item in queue");
            lacpPDUQueueId = new LacpQueue<LacpPDUPortStatusContainer>();
            LacpPDUQueueMap.put(switchId, lacpPDUQueueId);                          
        }

        synchronized(this.LacpPDUQueueMap.get(switchId)){
            LacpPDUQueueMap.get(switchId).enqueue(pdu);
            result = true;
        }

        return result;
    }


    /*
     * Dequeues the data from the PDU queue
     */ 	
    public LacpPDUPortStatusContainer dequeue(Long switchId){
        LacpPDUPortStatusContainer obj = null;
        LacpQueue<LacpPDUPortStatusContainer> lacpPDUQueueId = LacpPDUQueueMap.get(switchId);

        if(lacpPDUQueueId != null){
            synchronized(this.LacpPDUQueueMap.get(switchId)){
                obj = LacpPDUQueueMap.get(switchId).dequeue();
            }
        }
        return obj;
    }

    //Adds a new PDU queue	
    public boolean addLacpQueue(long switchId){
        boolean result = true;
        LacpQueue<LacpPDUPortStatusContainer> lacpPDUQueueId = LacpPDUQueueMap.get(switchId);

        if(lacpPDUQueueId == null){
            lacpPDUQueueId = new LacpQueue<LacpPDUPortStatusContainer>();
            LacpPDUQueueMap.put(switchId, lacpPDUQueueId);
            //System.out.println("Adding new queue for a given switch " + switchId);
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

        if(LacpPDUQueueMap.get(switchId) != null){
            synchronized(this.LacpPDUQueueMap.get(switchId)){
                LacpPDUQueueMap.get(switchId).remove();
                LacpPDUQueueMap.remove(switchId);
                result  = true;
            }
        }
        return result;
    }

    //Size of the queue
    public long getLacpQueueSize(long switchId){

        long size = 0;

        if(LacpPDUQueueMap.get(switchId) != null){
            //System.out.println("The given switchId " + switchId + " is present in the LacpPDUqueueMap");
            size = LacpPDUQueueMap.get(switchId).size();
        }
        return size;
    }

}
