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
import java.util.Iterator;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.Utils.*;



import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.inventory.LacpSystem;
import java.util.concurrent.locks.ReentrantLock;



public class RSMThread implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(RSMThread.class);
    private LacpNodeExtn lacpNode;
    private LacpPDUQueue pduQueue;
    private LacpTimerQueue timerQueue;
    private static final int MAX_PDU_ELM_CNT = 5;
    private static final int MAX_TMR_ELM_CNT = 3;
    private Thread rsm;
    private RSMManager rsmMgrRef;
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
            LOG.warn("failed to create the pdu queue for the node {}", lacpNode.getNodeId());
            return false;
        }
        timerQueue = LacpTimerQueue.getLacpTimerQueueInstance();
        if (timerQueue.addLacpQueue(lacpNode.getSwitchId()) == false)
        {
            LOG.warn("failed to create the timer queue for the node {}", lacpNode.getNodeId());
            return false;
        }
        return true;
    }
    public void startRSM()
    {
        rsm = new Thread(this);
        rsm.start();
    }
    public void interruptRSM()
    {
        rsm.interrupt();
    }

    private LacpBond findLacpBondByPartnerMacKey(byte[] sysId, short key) {

	if (lacpList.size() == 0){
		return null;
	}

	for (LacpBond bond: lacpList.values()) {
		if (bond.isPartnerExist(sysId,key)) {
			return bond;
		}
	}
	return null;
    }


    public void  handleLacpBpdu(LacpBpduInfo lacpBpdu){

	LOG.debug("handleLacpBpdu - Entry - New LacpBpdu received for processing...");

	LacpBond bond = null;

	long swId = lacpBpdu.getSwId();
	short portId = lacpBpdu.getPortId();

	bond = lacpList.get(portId);
		
	boolean newEntry = false;
        LacpSysKeyInfo sysKeyInfo = null;

	if (bond == null) {
		LOG.debug("handleLacpBpdu - Entry - bond not found based on portId={}",portId);

		if(isStaleLacpPduInQ(lacpBpdu)){
			LOG.info("handleLacpBpdu - isStaleLacpPduInQ - returned true, as this packet is yet to be processed from the queue and there is no corresponding bond found for this PDU and the state of the port is already in Collecting-Distributing");
			LOG.info("Dropping this packet with no further processing");
			return;
		}

		byte[] sysId = lacpBpdu.getActorSystemInfo().getNodeSysAddr();
		short key = lacpBpdu.getActorSystemInfo().getNodeKey();
		int priority = (lacpBpdu.getActorSystemInfo().getNodeSysPri()) & INT_PRIORITY;
		LOG.debug("handleLacpBpdu - entry - priority value is = {}", priority);
		sysKeyInfo = new LacpSysKeyInfo(sysId,key);
		bond = findLacpBondByPartnerMacKey(sysId, key);
		if (bond == null) {
			LOG.debug("handleLacpBpdu - couldn't find lacpBond for partner with sysId={} key={}", sysId, key);
			bond = lacpSysKeyList.get(sysKeyInfo);	
		}else{
			LOG.debug("handleLacpBpdu - bond found by partner mac key partner with sysId={} key={}", sysId, key);
		} 
		
		if (bond!= null ) {
			int bondPriority = (bond.bondGetSysPriority() & INT_PRIORITY);
			LOG.debug("handleLacpBpdu - LACP Bond is found sysId={}, key={}, priority={} ", LacpConst.toHex(sysId),
			String.format("0x%04x",key), String.format("0x%04x",priority));
			bond.bondAddSlave(swId, portId, 0x000000ff,lacpBpdu);
			if (bondPriority >= priority) {
				LOG.debug("handleLacpBpdu - Bond [Key={}] Priority is changed from {} to {} because of Pri={} over Port={} and SW={} at {}",
				String.format("0x%04x",bond.getAdminKey()),
				String.format("0x%04x",bondPriority),
				String.format("0x%04x",(priority >>1) & INT_PRIORITY),
				String.format("0x%04x",priority), String.format("0x%04x",portId), HexEncode.longToHexString(swId));

				bond.bondUpdateSystemPriority(((priority >>1) & INT_PRIORITY) );
			} 
			NodeConnector portNC = null;
			int portFeatureResult = 0;
			DataBroker ds = LacpUtil.getDataBrokerService();
			if(ds == null){
				LOG.error("handleLacpBpdu - Unable to get the DataBroker service,NOT processing the lacp pdu");
				return;
			}
			portNC = LacpPortProperties.getNodeConnector(ds, lacpBpdu.getNCRef());
			portFeatureResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(portNC);
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatureResult);
			if (lacpList.putIfAbsent(portId, bond) == null) {
				newEntry = true;
				LOG.debug("handleLacpBpdu - bond={} added for  given port={}",bond,  String.format("0x%04x",portId));
			}
		}else{
			LOG.debug("handleLacpBpdu - LACP Bond is not found sysId={}, key={}, priority={} ", LacpConst.toHex(sysId),
				String.format("0x%04x",key), String.format("0x%04x",priority));
			LOG.debug("LACP Bond is not found for portId={} ", portId);
			int bondPriority = (rsmMgrRef.getMidSysPriority() & INT_PRIORITY);
			LOG.debug("in else - bondPriority value is = {}", bondPriority);
			bond = LacpBond.newInstance((short)rsmMgrRef.getGlobalLacpkey(), lacpNode);
			rsmMgrRef.incGlobalLacpKey();
			newEntry = true;
			if (priority <  bondPriority) {
				priority = ((priority >>1) & INT_PRIORITY);
			} else {
				priority = ((rsmMgrRef.getMidSysPriority()>>1) & INT_PRIORITY);
			}
			LOG.debug("in else - priority value is = {}", priority);
			bond.bondSetSysPriority(priority);
			bond.bondAddSlave(swId,portId,0x000000ff,lacpBpdu);
			LOG.debug("LacpBond with key={} is created with system priority={} ",
					(rsmMgrRef.getGlobalLacpkey()-1), String.format("%04x",priority));
			bond.setLacpEnabled(true);
			NodeConnector portNC = null;
			int portFeatureResult = 0;
			DataBroker ds = LacpUtil.getDataBrokerService();
			if(ds == null){
				LOG.error("handleLacpBpdu - Unable to get the DataBroker service,NOT processing the lacp pdu");
				return;
			}
			portNC = LacpPortProperties.getNodeConnector(ds, lacpBpdu.getNCRef());
			portFeatureResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(portNC);
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatureResult);
			LacpBond lacpBond = lacpList.putIfAbsent(portId, bond);

			sysKeyInfo = new LacpSysKeyInfo(sysId,key);
			lacpBond = lacpSysKeyList.putIfAbsent(sysKeyInfo,bond);
			if( lacpBond != null) {
				LOG.debug("handleLacpBpdu - Exception: bond {} with sysKey {} already exist", bond.toString(), 
											sysKeyInfo.toString());
			}
		} 
	}
	if(bond != null){
		doRxPktProcess(swId,portId,lacpBpdu,bond);
	}
	LOG.debug("handleLacpBpdu - Exit...");
    }

    public void doRxPktProcess(long switchId,short portId,LacpBpduInfo bpduInfo, LacpBond bond){

	LOG.debug("doRxPktProcess - Entry...");
	bond.bondStateMachineLock();
	try {
		Iterator<LacpPort> iter = bond.getSlaveList().iterator();
		while(iter.hasNext()) { 
			LacpPort lacpPort = iter.next();
			if (lacpPort.slaveGetPortId() == portId && 
					lacpPort.getLacpSwId() == switchId) {

				lacpPort.slavePSMLock();
				try {
					LOG.debug("doRxPktProcess - retrieved LacpPort object for portId={}",portId);
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
	LOG.debug("doRxPktProcess - Exit...");
    }

    
    public void handlePortTimeout(TimerExpiryMessage tmExpiryMsg){
	LOG.debug("handlePortTimeout - Entry... ");

	short portId = (short)tmExpiryMsg.getPortID();
	long switchId = tmExpiryMsg.getSwitchID();
	LacpBpduInfo lacpdu = null;
	LOG.debug("handlePortTimeout - switchId={} portId={} expiryTimer={}", switchId, portId, tmExpiryMsg.getTimerWheelType());

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
						LOG.debug("handlePortTimeout - retrieved LacpPort object for portId={}",portId);
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
		//LOG.message	
	}
	LOG.debug("handlePortTimeout - Exit... ");
    }


    public void handleLacpPortState(LacpPortStatus portState){

	LOG.debug("handleLacpPortState - Entry");
	LacpBond bond = null;
	LacpPort lacpPort = null;

	long swId = portState.getSwID();
	short portId = (short)portState.getPortID();
	int portFeatures = portState.getPortFeatures();
        InstanceIdentifier<NodeConnector> ncId = portState.getNodeConnectorInstanceId();
	
	bond = lacpList.get(portId);	
	
	if(bond != null)
        {
		if(portState.getPortStatus()==1){
			LOG.debug("handleLacpPortState - found lacpBond for port={},  send link up into bond={}", portId,bond.getBondId());
			bond.bondUpdateLinkUpSlave(swId,portId,portFeatures);
		}else{
		    LOG.debug("handleLacpPortState - found lacpBond for port={},  send link down int bond={}", portId,bond.getBondId());
                    lacpPort = bond.getSlavePortObject(portId);
                    if( lacpPort != null)
                    {
                        lacpPort.setPortOperStatus(false); 
                        LOG.debug("in handleLacpPortState - setting timeout to true"); 
                    }
		    bond.bondUpdateLinkDownSlave(swId,portId);
                    if( lacpPort != null){
                        lacpPort.lacpPortCleanup();
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
                                LOG.debug("Port[{}] at SW={} is removed from LacpBond",
                                        portId,swId );

                                bond.bondDelSlave(swId, portId);
                        } finally {
                                bond.bondStateMachineUnlock();
                        }
                        lacpList.remove(portId);
                        if (!bond.bondHasMember()) {
                                if (key!=0) {
                                        LOG.debug("SW={} Key={} is removed from lacp system key list",
                                                swId, key);
                                        LacpSysKeyInfo sysKeyInfo = new LacpSysKeyInfo(sysId,key);
                                        lacpSysKeyList.remove(sysKeyInfo);
                                }
                        }
                    }
		}
	}
    else
    {
        boolean result = false;
        synchronized (lacpNode)
        {
            result = lacpNode.removeNonLacpPort(ncId);
        }
        if (result == false)
        {
		    LOG.debug("handleLacpPortState - couldn't find port={} in switch {}, no action to be taken", portId, swId);
        }
    }
	LOG.debug("handleLacpPortState - Exit");
  }

    public void nodeCleanup(){
            long swId = lacpNode.getSwitchId();
            byte[] sysId = null;
            short key = 0 ;

            LOG.debug("nodeCleanup Entry");
            if(lacpList.size() != 0){
                    for (LacpBond bond: lacpList.values()) {
                            if (bond.getActiveAgg() != null) {
                                    sysId = bond.getActiveAgg().getPartnerSystem();
                                    key = bond.getActiveAgg().aggGetPartnerOperAggKey();
                            }

                            if (!bond.bondHasMember()) {
                                    if (key!=0) {
                                            LOG.debug("SW={} Key={} is removed from lacp system key list",
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
                    }
            }
            LOG.debug("nodeCleanup Exit");
    }


    public void handleLacpNodeDeletion ()
    {
        // empty queues and delete it.
        LacpPDUPortStatusContainer pduElem = null;
        TimerExpiryMessage tmrElem = null;
        long swId = lacpNode.getSwitchId();
        while ((pduElem = pduQueue.dequeue(swId)) != null){};
        while ((tmrElem = timerQueue.dequeue(swId)) != null){};
        if (pduQueue.deleteLacpQueue(swId) == false)
        {
            LOG.warn("failed to delete the pdu queue for the node {}", lacpNode.getNodeId());
        }
        if (timerQueue.deleteLacpQueue(swId) == false)
        {
            LOG.warn("failed to delete the timer queue for the node {}", lacpNode.getNodeId());
        }
	//nodeCleanup();
        // remove from rsmThread mgr.
        rsmMgrRef.deleteRSM (lacpNode);
        LOG.debug("handleLacpNodeDeletion: removing the RSM thread created for this node");
        // remove from lacp system call deleteLacpNode.
        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        lacpSystem.removeLacpNode (swId);
    }

    @Override
    public void run()
    {
        LOG.debug("started RSM thread for the node {}", lacpNode.getNodeId());
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
			LOG.debug("Got LACP_PORT_STATUS_MSG message");
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

            while ((tmrElemCnt <= MAX_TMR_ELM_CNT)
                   && ((tmrElem = timerQueue.dequeue((long)lacpNode.getSwitchId())) != null))
            {
                tmrElemCnt++;
		handlePortTimeout(tmrElem);
            }

            // both the queues are free. Sleep for some time before verifing the queues
	    if ((pduElemCnt == 0) && (tmrElemCnt == 0)){
		    LOG.debug("RSM thread for the node {} is going to sleep...", lacpNode.getNodeId());
		    while((pduQueue.read((long)lacpNode.getSwitchId()) == null) &&
				    (timerQueue.read((long)lacpNode.getSwitchId()) == null) ){
			    try
			    {
				    Thread.sleep(2);
			    }
			    catch (InterruptedException e)
			    {
				    LOG.debug("RSM thread for node {} interrupted. continue further q reading", lacpNode.getNodeId());
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
    public LacpPort getLacpPortForPortId (short portId)
    {
        LacpPort port = null;
        LacpBond bond = lacpList.get(portId);
        if (bond != null)
        {
            port = bond.getSlavePortObject(portId);
        }
        return port;
    }
}
