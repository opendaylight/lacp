/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpTimerQueue;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.timer.TimerExpiryMessage;
import org.opendaylight.lacp.inventory.LacpNodeExtn;

import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.inventory.LacpBond;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.core.PortId;

import java.util.concurrent.ConcurrentHashMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.Utils.*;



import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.inventory.LacpSystem;
import java.util.concurrent.locks.ReentrantLock;



public class RSMThread implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(RSMThread.class);
    private LacpNodeExtn lacpNode;
    private LacpPDUQueue pduQueue;
    private LacpTimerQueue timerQueue;
    private static final int MAX_PDU_ELM_CNT = 5;
    private static final int MAX_TMR_ELM_CNT = 3;
    private Thread rsm;
    private RSMManager rsmMgrRef;
    private ReentrantLock thrLock;
    private static final int INT_PRIORITY = 0xffffffff;

    //PortId-Bond map
    private ConcurrentHashMap  <Short, LacpBond> lacpList;
    //SysKeyInfo-Bond map
    private ConcurrentHashMap <LacpSysKeyInfo, LacpBond> lacpSysKeyList;

    public RSMThread(){
	rsmMgrRef = RSMManager.getRSMManagerInstance();
	lacpList = new ConcurrentHashMap <Short,LacpBond>();
	lacpSysKeyList = new ConcurrentHashMap <LacpSysKeyInfo,LacpBond>();
    }
    public boolean setLacpNode (LacpNodeExtn lacpNode)
    {
        this.lacpNode = lacpNode;
        pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
        if (pduQueue.addLacpQueue(lacpNode.getSwitchId()) == false)
        {
            log.warn("failed to create the pdu queue for the node {}", lacpNode.getNodeId());
            return false;
        }
        timerQueue = LacpTimerQueue.getLacpTimerQueueInstance();
        if (timerQueue.addLacpQueue(lacpNode.getSwitchId()) == false)
        {
            log.warn("failed to create the timer queue for the node {}", lacpNode.getNodeId());
            return false;
        }
        return true;
    }
    public void startRSM()
    {
	System.out.println("RSMThread: Starting RSM Thread");
        rsm = new Thread(this);
        rsm.start();
    }
    public void interruptRSM()
    {
        rsm.interrupt();
    }

    private LacpBond findLacpBondByPartnerMacKey(byte[] sysId, short key) {

	if (lacpList.size() == 0)
		return null;

	for (LacpBond bond: lacpList.values()) {
		if (bond.isPartnerExist(sysId,key)) {
			return bond;
		}
	}
	return null;
    }


    public void  handleLacpBpdu(LacpBpduInfo lacpBpdu){

	log.info("handleLacpBpdu - Entry - New LacpBpdu received for processing...");
	System.out.println("RSMThread: handleLacpBpdu - Entry - New LacpBpdu received for processing...");

	LacpBond bond = null;

	//PortId obj = new PortId(lacpBpdu.getPortId());

	long swId = lacpBpdu.getSwId();
	short portId = lacpBpdu.getPortId();

	bond = lacpList.get(portId);
		
	boolean newEntry = false;
        LacpSysKeyInfo sysKeyInfo = null;

	if (bond == null) {
		log.info("handleLacpBpdu - Entry - bond not found based on portId={}",portId);
		System.out.println("handleLacpBpdu - bond not found for portId={}" + portId);

		if(isStaleLacpPduInQ(lacpBpdu)){
			log.info("handleLacpBpdu - isStaleLacpPduInQ - returned true, as this packet is yet to be processed from the queue and there is no corresponding bond found for this PDU and the state of the port is already in Collecting-Distributing");
			log.info("Dropping this packet with no further processing");
			return;
		}

		byte[] sysId = lacpBpdu.getActorSystemInfo().getNodeSysAddr();
		short key = lacpBpdu.getActorSystemInfo().getNodeKey();
		int priority = (lacpBpdu.getActorSystemInfo().getNodeSysPri()) & INT_PRIORITY;
		log.info("handleLacpBpdu - entry - priority value is = {}", priority);
		sysKeyInfo = new LacpSysKeyInfo(sysId,key);
		bond = findLacpBondByPartnerMacKey(sysId, key);
		if (bond == null) {
			log.info("handleLacpBpdu - couldn't find lacpBond for partner with sysId={} key={}", sysId, key);
			bond = lacpSysKeyList.get(sysKeyInfo);	
		}else{
			log.info("handleLacpBpdu - bond found by partner mac key partner with sysId={} key={}", sysId, key);
		} 
		
		if (bond!= null ) {
			int bond_priority = (bond.bondGetSysPriority() & INT_PRIORITY);
			log.info("handleLacpBpdu - LACP Bond is found sysId={}, key={}, priority={} ", LacpConst.toHex(sysId),
			String.format("0x%04x",key), String.format("0x%04x",priority));
			System.out.println("handleLacpBpdu - LACP Bond is found sysId={}, key={}, priority={} "+ LacpConst.toHex(sysId) + String.format("0x%04x",key) +  String.format("0x%04x",priority));
			bond.bondAddSlave(swId, portId, 0x000000ff,lacpBpdu);
			if (bond_priority >= priority) {
				log.info("handleLacpBpdu - Bond [Key={}] Priority is changed from {} to {} because of Pri={} over Port={} and SW={} at {}",
				String.format("0x%04x",bond.getAdminKey()),
				String.format("0x%04x",bond_priority),
				String.format("0x%04x",(priority >>1) & INT_PRIORITY),
				String.format("0x%04x",priority), String.format("0x%04x",portId), HexEncode.longToHexString(swId));

				bond.bondUpdateSystemPriority(((priority >>1) & INT_PRIORITY) );
			} 
			NodeConnector portNC = null;
			int portFeatureResult = 0;
			DataBroker ds = LacpUtil.getDataBrokerService();
			if(ds == null){
				log.error("handleLacpBpdu - Unable to get the DataBroker service,NOT processing the lacp pdu");
				return;
			}
			portNC = LacpPortProperties.getNodeConnector(ds, lacpBpdu.getNCRef());
			portFeatureResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(portNC);
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatureResult);
			if (lacpList.putIfAbsent(portId, bond) == null) {
				newEntry = true;
				log.info("handleLacpBpdu - bond={} added for  given port={}",bond,  String.format("0x%04x",portId));
			}
		}else{
			log.info("handleLacpBpdu - LACP Bond is not found sysId={}, key={}, priority={} ", LacpConst.toHex(sysId),
				String.format("0x%04x",key), String.format("0x%04x",priority));
			log.info("LACP Bond is not found for portId={} ", portId);
			int bond_priority = (rsmMgrRef.getMidSysPriority() & INT_PRIORITY);
			log.info("in else - bond_priority value is = {}", bond_priority);
			bond = LacpBond.newInstance((short)rsmMgrRef.getGlobalLacpkey(), lacpNode);
			rsmMgrRef.incGlobalLacpKey();
			newEntry = true;
			if (priority <  bond_priority) {
				priority = ((priority >>1) & INT_PRIORITY);
			} else {
				priority = ((rsmMgrRef.getMidSysPriority()>>1) & INT_PRIORITY);
			}
			log.info("in else - priority value is = {}", priority);
			bond.bondSetSysPriority(priority);
			bond.bondAddSlave(swId,portId,0x000000ff,lacpBpdu);
			log.info("LacpBond with key={} is created with system priority={} ",
					(rsmMgrRef.getGlobalLacpkey()-1), String.format("%04x",priority));
			bond.setLacpEnabled(true);
			NodeConnector portNC = null;
			int portFeatureResult = 0;
			DataBroker ds = LacpUtil.getDataBrokerService();
			if(ds == null){
				log.error("handleLacpBpdu - Unable to get the DataBroker service,NOT processing the lacp pdu");
				return;
			}
			portNC = LacpPortProperties.getNodeConnector(ds, lacpBpdu.getNCRef());
			portFeatureResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(portNC);
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatureResult);
			LacpBond lacpBond = lacpList.putIfAbsent(portId, bond);

			sysKeyInfo = new LacpSysKeyInfo(sysId,key);
			lacpBond = lacpSysKeyList.putIfAbsent(sysKeyInfo,bond);
			if( lacpBond != null) {
				log.info("handleLacpBpdu - Exception: bond {} with sysKey {} already exist", bond.toString(), 
											sysKeyInfo.toString());
			}
		} 
	}
	if(bond != null){
		System.out.println("handleLacpBpdu - Entry - bond found for portId={}" + portId);
		doRxPktProcess(swId,portId,lacpBpdu,bond);
	}
	log.info("handleLacpBpdu - Exit...");
    }

    public void doRxPktProcess(long switchId,short portId,LacpBpduInfo bpduInfo, LacpBond bond){

	log.info("doRxPktProcess - Entry...");
	System.out.println("doRxPktProcess - Entry...");
	bond.bondStateMachineLock();
	try {
		Iterator<LacpPort> iter = bond.getSlaveList().iterator();
		while(iter.hasNext()) { 
			LacpPort lacpPort = iter.next();
			if (lacpPort.slaveGetPortId() == portId && 
					lacpPort.getLacpSwId() == switchId) {

				lacpPort.slavePSMLock();
				try {
					log.info("doRxPktProcess - retrieved LacpPort object for portId={}",portId);
					System.out.println("doRxPktProcess - retrieved LacpPort object for portId={}" + portId);
					lacpPort.slaveRxLacpBpduReceived(bpduInfo);
				}	 
				finally {
					lacpPort.slavePSMUnlock();
				}	
			}	
		}
	}	 
	finally {
		bond.bondStateMachineUnlock();
	}
	log.info("doRxPktProcess - Exit...");
	System.out.println("doRxPktProcess - Exit...");
    }

    
    public void handlePortTimeout(TimerExpiryMessage tmExpiryMsg){
	log.info("handlePortTimeout - Entry... ");

	short portId = (short)tmExpiryMsg.getPortID();
	long switchId = tmExpiryMsg.getSwitchID();
	LacpBpduInfo lacpdu = null;
	log.info("handlePortTimeout - switchId={} portId={} expiryTimer={}", switchId, portId, tmExpiryMsg.getTimerWheelType());
	System.out.println("handlePortTimeout - switchId= " + switchId + " " + "portId= " + portId + " expiryTimer= " + tmExpiryMsg.getTimerWheelType());

	//PortId obj = new PortId(portId);
	LacpBond bond = lacpList.get(portId);

	if(bond != null){
		
		bond.bondStateMachineLock();
		try {
			Iterator<LacpPort> iter = bond.getSlaveList().iterator();
			while(iter.hasNext()) { 
				LacpPort lacpPort = iter.next();
				if (lacpPort.slaveGetPortId() == portId && 
						lacpPort.getLacpSwId() == switchId) {

					lacpPort.slavePSMLock();
					try {
						log.info("handlePortTimeout - retrieved LacpPort object for portId={}",portId);
						System.out.println("handlePortTimeout - calling runProtocolStateMachine");
						lacpPort.runProtocolStateMachine(lacpdu,tmExpiryMsg);
					}	 
					finally {
						lacpPort.slavePSMUnlock();
					}	
				}	
			}
		}	 
		finally {
			bond.bondStateMachineUnlock();
		}
	}else{
		//log message	
	}
	log.info("handlePortTimeout - Exit... ");
	System.out.println("handlePortTimeout - Exit... ");
    }


    public void handleLacpPortState(LacpPortStatus portState){

	log.info("handleLacpPortState - Entry");
	LacpBond bond = null;
	LacpPort lacpPort = null;

	long swId = portState.getSwID();
	short portId = (short)portState.getPortID();
	int portFeatures = portState.getPortFeatures();
    InstanceIdentifier<NodeConnector> ncId = portState.getNodeConnectorInstanceId();
	System.out.println("In handleLacpPortState");	
	//PortId portObj = new PortId((short)portState.getPortID());
	
	bond = lacpList.get(portId);	
	
	if(bond != null){
		if(portState.getPortStatus()==1){
			log.info("handleLacpPortState - found lacpBond for port={},  send link up into bond={}", portId,bond.getBondId());
			System.out.println("POrt status is UP, not passing ");
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatures);
		}else{
		    log.info("handleLacpPortState - found lacpBond for port={},  send link down int bond={}", portId,bond.getBondId());
		    System.out.println("POrt status is Down");	 	
		    bond.bondUpdateLinkDownSlave(swId,portId);
            		lacpNode.removeLacpPort(ncId, false);
		
                    lacpPort = bond.getSlavePortObject(portId);

                    if( lacpPort != null){
                            lacpPort.lacpPortCleanup();
                        System.out.println("After cancelling port timers");
                    }

                    short aggId = 0;
                    if (bond!=null)  {
                        byte[] sysId = null;
                        short key = 0 ;
                        bond.bondStateMachineLock();
                        try {
                                aggId = bond.bondGetAggId(swId,portId);
                                if (bond.getActiveAgg()!=null) {
                                        sysId = bond.getActiveAgg().getPartnerSystem();
                                        key = bond.getActiveAgg().aggGetPartnerOperAggKey();
                                }
                                log.info("Port[{}] at SW={} is removed from LacpBond",
                                        portId,swId );

                                bond.bondDelSlave(swId, portId);
                        } finally {
                                bond.bondStateMachineUnlock();
                        }
                        lacpList.remove(portId);
                        if (!bond.bondHasMember()) {
                                if (key!=0) {
                                        log.info("SW={} Key={} is removed from lacp system key list",
                                                swId, key);
                                        LacpSysKeyInfo sysKeyInfo = new LacpSysKeyInfo(sysId,key);
                                        lacpSysKeyList.remove(sysKeyInfo);
                                }
                        }
                    }


		    /*
		    short aggId = 0;
 		    if (bond!=null)  {
			byte[] sysId = null;
			short key = 0 ;
			bond.bondStateMachineLock();	
			try {
				aggId = bond.bondGetAggId(swId,portId);
				if (bond.getActiveAgg()!=null) {
					sysId = bond.getActiveAgg().getPartnerSystem();
					key = bond.getActiveAgg().aggGetPartnerOperAggKey();
				}							
				if (aggId > 0) {
					if (bond.getBondId() > 0) {
						int bondId = bond.getBondId();
						//TODO - Notify to sw - Group table update
						//updateActionToSw(bondId,swId,portId,false);
						//modifyBond(bondId,false,swId,portId);
					}
				}	
				log.info("Port[{}] at SW={} is removed from LacpBond", 
					portId,swId );

				bond.bondUpdateLinkDownSlave(swId,portId);
				bond.bondDelSlave(swId, portId);
			} finally {
				bond.bondStateMachineUnlock();
			}
			lacpList.remove(new PortId(portId));
			if (!bond.bondHasMember()) {
				//TODO
				//removeBond(bond.getBondId());
				if (key!=0) {
					log.info("SW={} Key={} is removed from lacp system key list",
						swId, key);
					LacpSysKeyInfo sysKeyInfo = new LacpSysKeyInfo(sysId,key);
					lacpSysKeyList.remove(sysKeyInfo);
				}		
			}						

	    	    }
		    */
		}
	}
    else
    {
        boolean result = lacpNode.removeNonLacpPort(ncId);
        if (result == false)
        {
		    log.info("handleLacpPortState - couldn't find port={} in switch {}, no action to be taken", portId, swId);
        }
	}
	log.info("handleLacpPortState - Exit");
    }

    public void nodeCleanup(){
            long swId = lacpNode.getSwitchId();
            byte[] sysId = null;
            short key = 0 ;

            log.info("nodeCleanup Entry");
            if(lacpList.size() != 0){
                    for (LacpBond bond: lacpList.values()) {
                            if (bond.getActiveAgg() != null) {
                                    sysId = bond.getActiveAgg().getPartnerSystem();
                                    key = bond.getActiveAgg().aggGetPartnerOperAggKey();
                            }

                            if (!bond.bondHasMember()) {
                                    if (key!=0) {
                                            log.info("SW={} Key={} is removed from lacp system key list",
                                                            swId, key);
                                            LacpSysKeyInfo sysKeyInfo = new LacpSysKeyInfo(sysId,key);
                                            lacpSysKeyList.remove(sysKeyInfo);
                                    }
                            }

                            for (LacpPort lacpPort: bond.getSlaveList()) {
                                    if( lacpPort != null){
                                            lacpPort.lacpPortCleanup();
                                            bond.bondDelSlave(swId, lacpPort.slaveGetPortId());
                                            lacpList.remove(new PortId(lacpPort.slaveGetPortId()));
                                    }
                            }
			     System.out.println("The ports in the bond is deleted");	

                    }
            }
            log.info("nodeCleanup Exit");
    }


    public void handleLacpNodeDeletion ()
    {
        // empty queues and delete it.
        LacpPDUPortStatusContainer pduElem = null;
        TimerExpiryMessage tmrElem = null;
        long swId = lacpNode.getSwitchId();
	System.out.println("In NOde Deletion API");
        while ((pduElem = pduQueue.dequeue(swId)) != null);
        while ((tmrElem = timerQueue.dequeue(swId)) != null);
        if (pduQueue.deleteLacpQueue(swId) == false)
        {
            log.warn("failed to delete the pdu queue for the node {}", lacpNode.getNodeId());
        }
        if (timerQueue.deleteLacpQueue(swId) == false)
        {
            log.warn("failed to delete the timer queue for the node {}", lacpNode.getNodeId());
        }
	System.out.println("Cleared all the Queues");
	//nodeCleanup();
        // remove from rsmThread mgr.
        rsmMgrRef.deleteRSM (lacpNode);
        log.debug("handleLacpNodeDeletion: removing the RSM thread created for this node");
        // remove from lacp system call deleteLacpNode.
        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        lacpSystem.removeLacpNode (swId);
        log.info("deleting the node {} from lacp system", swId);
    }

    @Override
    public void run()
    {
        log.info("started RSM thread for the node {}", lacpNode.getNodeId());
	System.out.println("RSMThread: started RSM thread for the node {}" + lacpNode.getNodeId());
        boolean continueRun = true;

        while (continueRun == true)
        {
            LacpPDUPortStatusContainer pduElem = null;
            TimerExpiryMessage tmrElem = null;
            int pduElemCnt = 0;
            int tmrElemCnt = 0;
            while ((pduElemCnt <= MAX_PDU_ELM_CNT)
                   && ((pduElem = pduQueue.dequeue((long)lacpNode.getSwitchId())) != null))
            {

                pduElemCnt++;
                //if node del msg free queues
		if(pduElem.getMessageType() == LacpPDUPortStatusContainer.MessageType.LACP_PDU_MSG){
			handleLacpBpdu((LacpBpduInfo)pduElem);
		}else if(pduElem.getMessageType() == 
				LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG){
			System.out.println("Got .LACP_PORT_STATUS_MSG message");
			handleLacpPortState((LacpPortStatus)pduElem);
		}
		else if (pduElem.getMessageType() == LacpPDUPortStatusContainer.MessageType.LACP_NODE_DEL_MSG)
		{
			/* as node is getting deleted, break out of this while loop. Skip the next timer queue loop
			 * and break the outer while loop also */
			handleLacpNodeDeletion();
			return;
		}
            }

	    System.out.println("Checking for any timer expiry objects....");
            while ((tmrElemCnt <= MAX_TMR_ELM_CNT)
                   && ((tmrElem = timerQueue.dequeue((long)lacpNode.getSwitchId())) != null))
            {
                tmrElemCnt++;
		System.out.println("Found timer expiry message on the queue");
		handlePortTimeout(tmrElem);
            }

            // both the queues are free. Sleep for some time before verifing the queues
	    if ((pduElemCnt == 0) && (tmrElemCnt == 0)){
		    log.info("RSM thread for the node {} is going to sleep...", lacpNode.getNodeId());
		    while((pduQueue.read((long)lacpNode.getSwitchId()) == null) &&
				    (timerQueue.read((long)lacpNode.getSwitchId()) == null) ){
			    try
			    {
				    Thread.sleep(2);
			    }
			    catch (InterruptedException e)
			    {
				    log.info("RSM thread for node {} interrupted. continue further q reading", lacpNode.getNodeId());
			    }
		    }
	    }
            
        }

    }
    public boolean isStaleLacpPduInQ(LacpBpduInfo lacpdu){
	boolean result = false;
	if(((lacpdu.getActorSystemInfo().getNodePortState() & LacpConst.PORT_STATE_COLLECTING) > 0) || 
                                      ((lacpdu.getActorSystemInfo().getNodePortState() & LacpConst.PORT_STATE_DISTRIBUTING) > 0)) {
		result = true;
	}
	return result;
    }
}
