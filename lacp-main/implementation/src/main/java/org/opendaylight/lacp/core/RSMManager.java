/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.lacp.inventory.LacpNodeExtn;

import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.lacp.queue.LacpTxQueue;


public class RSMManager
{
    private static final Logger log = LoggerFactory.getLogger(RSMManager.class);
    private static final ConcurrentHashMap<Long, RSMThread> lacpThreadMap = new ConcurrentHashMap<Long, RSMThread>();
    final private static int midSysPriority = 0x8000;
    private static int globalLacpkey = 1;
    //private LacpTxQueue lacpTxQueue;
    //
    private static RSMManager rsmMgrInstance;
    
    
    private RSMManager(){

    }

    public static RSMManager getRSMManagerInstance(){
	if(rsmMgrInstance == null){
		synchronized(RSMManager.class){
			if(rsmMgrInstance == null){
				rsmMgrInstance = new RSMManager();
			}
		}
	}
	return rsmMgrInstance;
    }

    public synchronized int getGlobalLacpkey(){
	return  globalLacpkey;
    }

    public synchronized int incGlobalLacpKey(){
	globalLacpkey++;
	if (globalLacpkey > 0xffff) {
		globalLacpkey = 1;
	}
	return globalLacpkey;
    }

   public synchronized int getMidSysPriority(){
	return midSysPriority;
   }

   /*
   public synchronized LacpTxQueue getSystemTxQueueInstance(){
	return lacpTxQueue;
   }
   */

    public boolean createRSM(LacpNodeExtn lacpNode)
    {
        //hash for lacpNode. currently one thread per node.
        System.out.println("RSMManager: createRSM");
        RSMThread nodeThread = null;
        nodeThread = lacpThreadMap.get(lacpNode.getSwitchId());
        if (nodeThread != null)
        {
            log.warn("RSMThread object is already created for node {}", lacpNode.getNodeId());
            return false;
        }

	//lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();

        nodeThread = new RSMThread();
        if (nodeThread.setLacpNode(lacpNode) == false)
        {
            log.warn("RSMThread object cannot be assigned for the node {}", lacpNode.getNodeId());
            return false;
        }
        synchronized(RSMManager.class)
        {
            lacpThreadMap.put(lacpNode.getSwitchId(), nodeThread);
            nodeThread.startRSM();
        	System.out.println("RSMManager: RSM thread is put into the map and started");
        }
        return true;
    }
    public boolean wakeupRSM(LacpNodeExtn lacpNode)
    {
        RSMThread nodeThread = null;
        nodeThread = lacpThreadMap.get(lacpNode.getSwitchId());
        if (nodeThread == null)
        {
            return false;
        }
        nodeThread.interruptRSM();
        return true;
    }
    public boolean deleteRSM(LacpNodeExtn lacpNode)
    {
        RSMThread nodeThread = null;
        nodeThread = lacpThreadMap.get(lacpNode.getSwitchId());
        if (nodeThread == null)
        {
            return false;
        }
        //post a node deletion message to the RSM pdu queue - TODO
        synchronized(RSMManager.class)
        {
            lacpThreadMap.remove(lacpNode.getSwitchId());
        }
        return true;
    }
}
