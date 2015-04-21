package org.opendaylight.lacp.queue;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.lacp.timer.TimerExpiryMessage;

public  class LacpTimerQueue {

    private static final Map<Long, LacpQueue<TimerExpiryMessage>> LacpTimerQueueMap =
        new ConcurrentHashMap<Long,LacpQueue<TimerExpiryMessage>>();
    private static final LacpTimerQueue instance = new LacpTimerQueue();

    protected LacpTimerQueue(){
    }

    /*
     * The LacpTimerQueue is a singleton class.
     */
    public static LacpTimerQueue getLacpTimerQueueInstance(){
        return instance;
    }

    /*
     * Validity method to check if the queue is created or not.
     */  
    public boolean isLacpQueuePresent(long switchId){

        boolean result = false;

        if(LacpTimerQueueMap.get(switchId) != null){
            result = true;
        }
        return result;
    }

    /*
     * The utility method enqueues the data to the Timer queue.
     * It creates the queue if it is not created.
     */  
    public boolean enqueue(long switchId, TimerExpiryMessage pdu){
        boolean result = false;
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LacpTimerQueueMap.get(switchId);

        if(lacpTimerQueueId == null){
            lacpTimerQueueId = new LacpQueue<TimerExpiryMessage>();
            LacpTimerQueueMap.put(switchId, lacpTimerQueueId);
        }

        synchronized(this.LacpTimerQueueMap.get(switchId)){
            LacpTimerQueueMap.get(switchId).enqueue(pdu);
            result = true;
        }	

        return result;
    }

    /*
     * Dequeues the data from the Timer queue
     */  
    public TimerExpiryMessage dequeue(long switchId){
        TimerExpiryMessage obj = null;
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LacpTimerQueueMap.get(switchId);

        if(lacpTimerQueueId != null){
            synchronized(this.LacpTimerQueueMap.get(switchId)){
                obj = LacpTimerQueueMap.get(switchId).dequeue();
            }
        }
        return obj;
        }

        /*
         * Adds a new Timer queue	 
         */ 	
        public boolean addLacpQueue(long switchId){
            boolean result = true;
            LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LacpTimerQueueMap.get(switchId);

            if(lacpTimerQueueId == null){
                lacpTimerQueueId = new LacpQueue<TimerExpiryMessage>();
                LacpTimerQueueMap.put(switchId, lacpTimerQueueId);
            }
            return result;
        }

        /*
         * Deletes all the enties in the Timer queue.
         * It also cleans the hash map entry for the
         * corresponding PDU queue.
         */ 	 
        public boolean deleteLacpQueue(long switchId){
            boolean result = false;

            if(isLacpQueuePresent(switchId)){
                synchronized(this.LacpTimerQueueMap.get(switchId)){
                    LacpTimerQueueMap.get(switchId).remove();
                    LacpTimerQueueMap.remove(switchId);

                    result  = true;
                }
            }

            return result;
        }

       public TimerExpiryMessage read(long switchId){
        TimerExpiryMessage obj = null;
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LacpTimerQueueMap.get(switchId);

        if(lacpTimerQueueId != null){
            synchronized(this.LacpTimerQueueMap.get(switchId)){
                obj = LacpTimerQueueMap.get(switchId).read();
            }
        }
        return obj;
        }

        /*
         * Utility Method to find the size of the queue
         */ 	 
        public long getLacpQueueSize(long switchId){

            long size = 0;

            if(LacpTimerQueueMap.get(switchId) != null){
                //System.out.println("The given switchId " + switchId + " is present in the LacpTimerqueueMap");
                size = LacpTimerQueueMap.get(switchId).size();
            }
            return size;
        }
    }
