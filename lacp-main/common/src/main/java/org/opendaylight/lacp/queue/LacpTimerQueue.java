package org.opendaylight.lacp.queue;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.lacp.timer.TimerExpiryMessage;

public  class LacpTimerQueue {

    private static final Map<Long, LacpQueue<TimerExpiryMessage>> LACP_TIMER_QUEUE_MAP =
        new ConcurrentHashMap<Long,LacpQueue<TimerExpiryMessage>>();
    private static final LacpTimerQueue INSTANCE = new LacpTimerQueue();

    protected LacpTimerQueue(){
    }

    /*
     * The LacpTimerQueue is a singleton class.
     */
    public static LacpTimerQueue getLacpTimerQueueInstance(){
        return INSTANCE;
    }

    /*
     * Validity method to check if the queue is created or not.
     */  
    public boolean isLacpQueuePresent(long switchId){

        boolean result = false;

        if(LACP_TIMER_QUEUE_MAP.get(switchId) != null){
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
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LACP_TIMER_QUEUE_MAP.get(switchId);

        if(lacpTimerQueueId == null){
            lacpTimerQueueId = new LacpQueue<TimerExpiryMessage>();
            LACP_TIMER_QUEUE_MAP.put(switchId, lacpTimerQueueId);
        }

        synchronized(this.LACP_TIMER_QUEUE_MAP.get(switchId)){
            LACP_TIMER_QUEUE_MAP.get(switchId).enqueue(pdu);
            result = true;
        }	

        return result;
    }

    /*
     * Dequeues the data from the Timer queue
     */  
    public TimerExpiryMessage dequeue(long switchId){
        TimerExpiryMessage obj = null;
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LACP_TIMER_QUEUE_MAP.get(switchId);

        if(lacpTimerQueueId != null){
            synchronized(this.LACP_TIMER_QUEUE_MAP.get(switchId)){
                obj = LACP_TIMER_QUEUE_MAP.get(switchId).dequeue();
            }
        }
        return obj;
        }

        /*
         * Adds a new Timer queue	 
         */ 	
        public boolean addLacpQueue(long switchId){
            boolean result = true;
            LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LACP_TIMER_QUEUE_MAP.get(switchId);

            if(lacpTimerQueueId == null){
                lacpTimerQueueId = new LacpQueue<TimerExpiryMessage>();
                LACP_TIMER_QUEUE_MAP.put(switchId, lacpTimerQueueId);
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
                synchronized(this.LACP_TIMER_QUEUE_MAP.get(switchId)){
                    LACP_TIMER_QUEUE_MAP.get(switchId).remove();
                    LACP_TIMER_QUEUE_MAP.remove(switchId);

                    result  = true;
                }
            }

            return result;
        }

       public TimerExpiryMessage read(long switchId){
        TimerExpiryMessage obj = null;
        LacpQueue<TimerExpiryMessage> lacpTimerQueueId = LACP_TIMER_QUEUE_MAP.get(switchId);

        if(lacpTimerQueueId != null){
            synchronized(this.LACP_TIMER_QUEUE_MAP.get(switchId)){
                obj = LACP_TIMER_QUEUE_MAP.get(switchId).read();
            }
        }
        return obj;
        }

        /*
         * Utility Method to find the size of the queue
         */ 	 
        public long getLacpQueueSize(long switchId){

            long size = 0;

            if(LACP_TIMER_QUEUE_MAP.get(switchId) != null){
                size = LACP_TIMER_QUEUE_MAP.get(switchId).size();
            }
            return size;
        }
    }
