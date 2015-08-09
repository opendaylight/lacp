/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.inventory.LacpNodeExtn;

import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.lacp.inventory.LacpPort;


public class RSMManager
{
    private static final Logger LOG = LoggerFactory.getLogger(RSMManager.class);
    private static final ConcurrentHashMap<Long, RSMThread> lacpThreadMap = new ConcurrentHashMap<Long, RSMThread>();
    final private static int midSysPriority = 0x8000;
    private static int globalLacpkey = 1;
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

    public boolean createRSM(LacpNodeExtn lacpNode)
    {
        //hash for lacpNode. currently one thread per node.
        LOG.debug ("entering createRSM ");
        RSMThread nodeThread = null;
        nodeThread = lacpThreadMap.get(lacpNode.getSwitchId());
        if (nodeThread != null)
        {
            LOG.warn("RSMThread object is already created for node {}", lacpNode.getNodeId());
            return false;
        }

        nodeThread = new RSMThread();
        if (nodeThread.setLacpNode(lacpNode) == false)
        {
            LOG.warn("RSMThread object cannot be assigned for the node {}", lacpNode.getNodeId());
            return false;
        }
        LOG.debug ("created the thread and set the node");
        synchronized(RSMManager.class)
        {
            lacpThreadMap.put(lacpNode.getSwitchId(), nodeThread);
            nodeThread.startRSM();
            LOG.debug ("started RSM thread for switch {}", lacpNode.getSwitchId());
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
    public LacpPort getLacpPortFromBond (long switchId, short portId)
    {
        RSMThread nodeThread = null;
        LacpPort lacpPort = null;
        nodeThread = lacpThreadMap.get(switchId);
        if (nodeThread != null)
        {
            return nodeThread.getLacpPortForPortId(portId);
        }
        return null;
    }
}
