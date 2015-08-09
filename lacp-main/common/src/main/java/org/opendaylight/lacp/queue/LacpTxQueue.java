/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

import java.util.ArrayList;
import org.opendaylight.lacp.queue.LacpPortInfo;

public  class LacpTxQueue {
    private static final ArrayList<LacpQueue<LacpPortInfo>>  LACP_TX_QUEUE_ARR =
        new ArrayList<LacpQueue<LacpPortInfo>>();
    private static final LacpTxQueue INSTANCE = new LacpTxQueue();

    public static enum QueueType
    {
        LACP_TX_NTT_QUEUE,
        LACP_TX_PERIODIC_QUEUE,
        LACP_TX_QUEUE_MAX
    }

    protected LacpTxQueue()
    {
        //LACP_TX_NTT_QUEUE
        LacpQueue<LacpPortInfo> lacpQ = new LacpQueue<LacpPortInfo>();
        LACP_TX_QUEUE_ARR.add(lacpQ);
        //LACP_TX_PERIODIC_QUEUE
        lacpQ = new LacpQueue<LacpPortInfo>();
        LACP_TX_QUEUE_ARR.add(lacpQ);
    }

    /*
     * The LacpTxQueue is a singleton class.
     */
    public static LacpTxQueue getLacpTxQueueInstance()
    {
        return INSTANCE;
    }

    private int getQueueId(QueueType queueType)
    {
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
    public boolean isLacpQueuePresent(QueueType queueType)
    {
        boolean result = false;

        if((!LACP_TX_QUEUE_ARR.isEmpty()) &&
                LACP_TX_QUEUE_ARR.get(getQueueId(queueType)) != null){
            result = true;
        }
        return result;
    }

    /*
     * The utility method enqueues the data to the Tx queue.
     * It creates the queue if it is not created.
     */

    public boolean enqueue(QueueType queueType, LacpPortInfo lacpPort)
    {
        boolean result = false;
        int queueId = getQueueId(queueType);
        if(isLacpQueuePresent(queueType))
        {
            synchronized(LACP_TX_QUEUE_ARR.get(queueId))
            {
                LACP_TX_QUEUE_ARR.get(queueId).enqueue(lacpPort);
                result = true;
            }
        }
        return result;
    }

    /*
     * Dequeues the data from the Tx queue
     */
    public LacpPortInfo dequeue(QueueType queueType)
    {
        LacpPortInfo obj = null;
        int queueId = getQueueId(queueType);
        LacpQueue<LacpPortInfo> lacpTxQueue = LACP_TX_QUEUE_ARR.get(queueId);

        if(lacpTxQueue != null)
        {
            synchronized(LACP_TX_QUEUE_ARR.get(queueId))
            {
                obj = LACP_TX_QUEUE_ARR.get(queueId).dequeue();
            }
        }
        return obj;
    }

    /*
     * Adds a new Transmit queue
     */
    public boolean addLacpQueue(QueueType queueType){
        boolean result = true;
        LacpQueue<LacpPortInfo> lacpTxQueue = new LacpQueue<LacpPortInfo>();

        LACP_TX_QUEUE_ARR.add(getQueueId(queueType), lacpTxQueue);
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
            synchronized(LACP_TX_QUEUE_ARR.get(getQueueId(queueType))){
                LACP_TX_QUEUE_ARR.get(getQueueId(queueType)).remove();
                LACP_TX_QUEUE_ARR.remove(getQueueId(queueType));
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

        if(LACP_TX_QUEUE_ARR.get(getQueueId(queueType)) != null){
            size = LACP_TX_QUEUE_ARR.get(getQueueId(queueType)).size();
        }
        return size;
    }
}
