/*
 *  Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import java.util.TreeSet;

import java.util.LinkedHashMap;
import java.util.List;

import java.util.concurrent.locks.ReentrantLock;

import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpLogPort;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.lacpaggregator.ListOfLagPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.lacpaggregator.ListOfLagPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;


public class LacpBond {

	private static int UniqueId = 1;	
	private int bondInstanceId;

	private static final Logger log = LoggerFactory.getLogger(LacpBond.class);
	private int slaveCnt;  /* default 1 slave port per bond */
	private List<LacpPort> slaveList;
	private LinkedHashMap<Short, LacpPort> portSlaveMap;
	private int minLinks;
	private int maximumLink = 8;  /* maximum links per each system for this bond */
	private int bondId;
	private ReentrantLock bondLock;
	private Date activeSince;
	
	private short sysPriority; 
	private byte[] virtualSysMacAddr; /* The first system's MAC address + 0x2 */
	private int aggSelectTimer;
	private int lacpFast;
	private LacpConst.BOND_TYPE select;
	private short adminKey;
	private LinkedHashMap<Long, Short> systemIdMap;  /* System MAC and ID */
	
	/* Aggregator List bound to this bond */
	private List<LacpAggregator>  aggregatorList;  
	private boolean isLacpEnabled;
	private boolean dirty;
	private boolean failed;
        
    private InstanceIdentifier aggInstId;
    private LacpAggregatorsBuilder lacpAggBuilder;
    private LacpNodeExtn lacpNodeRef;
    private NodeConnectorRef logNodeConnRef;
    private GroupId aggGrpId;
    private LacpGroupTbl lacpGroupTbl;
    private List<LacpPort> activePortList;

	public byte[] getVirtualSysMacAddr() {
		return virtualSysMacAddr;
	}
	public void setVirtualSysMacAddr(byte[] virtualSysMacAddr) {
		this.virtualSysMacAddr = virtualSysMacAddr;
	}
	public List<LacpPort> getSlaveList() {
		return slaveList;
	}
	public LinkedHashMap<Short, LacpPort> getPortSlaveMap() {
		return portSlaveMap;
	}
	public short getSysPriority() {
		return sysPriority;
	}

	public LinkedHashMap<Long, Short> getSystemIdMap() {
		return systemIdMap;
	}
	public List<LacpAggregator> getAggregatorList() {
		return aggregatorList;
	}
	public void setSlaveList(List<LacpPort> slaveList) {
		this.slaveList = slaveList;
	}
	public void setPortSlaveMap(LinkedHashMap<Short, LacpPort> portSlaveMap) {
		this.portSlaveMap = portSlaveMap;
	}
	public void setSysPriority(short sysPriority) {
		this.sysPriority = sysPriority;
	}
	public void setAdminKey(short adminKey) {
		this.adminKey = adminKey;
	}
	public void setSystemIdMap(LinkedHashMap<Long, Short> systemIdMap) {
		this.systemIdMap = systemIdMap;
	}
	public void setAggregatorList(List<LacpAggregator> aggregatorList) {
		this.aggregatorList = aggregatorList;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	
	
	public boolean isFailed() {
		return failed;
	}
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	public Date getActiveSince() {
		return activeSince;
	}
	public void setActiveSince(Date activeSince) {
		this.activeSince = activeSince;
	}
	public int getMinLinks() {
		return minLinks;
	}
	public void setMinLinks(int minLinks) {
		this.minLinks = minLinks;
	}
	public int getBondInstanceId() {
		return bondInstanceId;
	}

	int bondGetMaxLink() {
		return maximumLink;
	}
	
	void bondSetMaxLink(int link) {
		this.maximumLink = link;
	}
	
	public int getSlaveCnt() {
		return slaveCnt;
	}
	public void setSlaveCnt(short id2) {
		slaveCnt = id2;
		
	}
	
	public void bondStateMachineLock() {
		
	    this.bondLock.lock();
	}
	
	public void bondStateMachineUnlock() {
	    this.bondLock.unlock();
	}
	public int getBondId() {
		return bondId;
	}
	public void setBondId(int bondLagId) {
		this.bondId = bondLagId;
	}	
	
	public short getAdminKey() {
		return adminKey;
	}


	@Override
	public String toString() {
		String result;
		
		result = super.toString()+"\n";
		if (virtualSysMacAddr!=null && portSlaveMap!=null) {
		result = result+String.format("sys_priority:%x, agg_select_timer=%d, lacp_fast=%d, slave_cnt=%d ",
				sysPriority, aggSelectTimer, lacpFast, slaveCnt);
		
		result = result + String.format(" MAC Address:%2x:%2x:%2x:%2x:%2x:%2x \n ",virtualSysMacAddr[0],
				virtualSysMacAddr[1], virtualSysMacAddr[2],virtualSysMacAddr[3],virtualSysMacAddr[4],virtualSysMacAddr[5]) + "\n"; 
		result = result + "Slave PORT ID:";
		for (Short val: this.portSlaveMap.keySet()) {
			result = result +String.format("%x ",val);
		}
		result = result +"\n";
		}
		return result;
	}
	


	byte[] getSysMacAddr() {
		return virtualSysMacAddr;		
	}
	
	int getLacpFast() {
		return lacpFast;
	}
	
	void setLacpFast(int val) {
		this.lacpFast = val;
	}	
	
	public static LacpBond newInstance(short key, LacpNodeExtn lacpNode) {
		
		return new LacpBond((short)0xffff,key, lacpNode);
	}

	public static LacpBond newInstance(short sys_priority,short key, LacpNodeExtn lacpNode) {
		
		return new LacpBond(sys_priority,key, lacpNode);
	}
	
	private LacpBond(short sys_priority,short key, LacpNodeExtn lacpNode) 
	{
		log.info("LacpBond constructor"); 
		log.info("LacpBond is created with sys priority ={} and key={}",sys_priority,key); 

		bondInstanceId = UniqueId++;
		this.bondLock = new ReentrantLock();
		minLinks = 1;		
		slaveCnt = 0;
		portSlaveMap = new LinkedHashMap<Short, LacpPort>();
		
		slaveList = new ArrayList<LacpPort>();		
		systemIdMap = new LinkedHashMap<Long,Short>();
		aggregatorList = new ArrayList<LacpAggregator>();
		this.virtualSysMacAddr = new byte[6];
		
		this.sysPriority = sys_priority;

		this.lacpFast = 0;
		this.isLacpEnabled = false;
		this.select = LacpConst.BOND_TYPE.BOND_STABLE;
		this.sysPriority = sys_priority;
		this.adminKey = key;
		this.activeSince = null;
		this.dirty = true;
                lacpNodeRef = lacpNode;
                lacpAggBuilder = new LacpAggregatorsBuilder();
                NodeRef node = new NodeRef(lacpNode.getNodeId());
                lacpAggBuilder.setLagNodeRef(node);
                logNodeConnRef = null;
                InstanceIdentifier<Node> nodeId = lacpNode.getNodeId();
                NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
                aggInstId = InstanceIdentifier.builder(Nodes.class)
                              .child (Node.class, new NodeKey (nId))
                              .augmentation(LacpNode.class)
                              .child (LacpAggregators.class, new LacpAggregatorsKey(bondInstanceId)).toInstance();
                lacpGroupTbl = new LacpGroupTbl(LacpUtil.getSalGroupService(), LacpUtil.getDataBrokerService());
                Long groupId = LacpUtil.getNextGroupId();
                aggGrpId = new GroupId(groupId);
                activePortList = new ArrayList<LacpPort>();
                lacpAggBuilder.setLagGroupid(groupId);
                lacpAggBuilder.setAggId(bondInstanceId);
		log.info("Exiting LacpBond constructor"); 
	}
	
	public short bondGetSysPriority() {
		return sysPriority;
	}

	public void bondSetSysPriority(short sys_priority) {
		this.sysPriority = sys_priority;

	}

	int checkAggSelectTimer() {
		return (this.aggSelectTimer > 0 ? 1 : 0);
		
	}
	
	public void bondAddSlave(long swId, short portId, short port_priority,LacpBpduInfo bpduInfo) {

		log.info("bondAddSlave Entry"); 
		byte[] macAddr;

		macAddr = LacpConst.mapMacAddrFromSwId(swId);
		short systemId = 0;
		
		bondStateMachineLock();
		try {
			setDirty(true);
		if (this.systemIdMap.containsKey(swId))
			systemId = systemIdMap.get(swId);
		else {
			if (systemIdMap.size() != 0) {
				for (short value : systemIdMap.values()) {
					if (value > systemId)
						systemId = value;
				}
			}
			systemId ++;
			this.systemIdMap.put(swId, systemId);
		}
		if (systemId == 1) {
			/* Set Virtual MAC address for Bond */
			this.virtualSysMacAddr = Arrays.copyOf(macAddr, LacpConst.ETH_ADDR_LEN);
			this.virtualSysMacAddr[0] |= 0x02;			
		}
		
		LacpPort slave = LacpPort.newInstance((long)swId,portId, this, port_priority, bpduInfo);

		portSlaveMap.put(portId, slave);
		slaveList.add(slave);
		Collections.sort(slaveList);
		LacpAggregator agg = LacpAggregator.newInstance();
		aggregatorList.add(agg);
		Collections.sort(aggregatorList);
		agg.setAggBond(this);
		if (this.isLacpEnabled){
			slave.slaveSetLacpPortEnabled(this.isLacpEnabled);
		}

		log.info(
				"Port[Port ID = " + portId +  
				"] from SW= " +HexEncode.longToHexString(swId) +"  is added into LACP Bond Key=" + this.adminKey +
				" with Virutal Mac=" + HexEncode.bytesToHexString(virtualSysMacAddr));
		} finally {
			bondStateMachineUnlock();	
		}
		log.info("bondAddSlave Exit"); 
	}
	

	
	void bondDelSlave(long swId, short portId) {
		log.info("bondDelSlave Entry"); 
		
		short systemId = 0;


		bondStateMachineLock();	
		
		try {
			if (!this.systemIdMap.containsKey(swId)){
				return;
			}

			systemId = systemIdMap.get(swId);
			
			LacpPort slave = portSlaveMap.get(portId);
			
			if (slave == null) {
				return;
			}
			setDirty(true);
			slave.slavePSMLock();
			try {
				slave.lacpDisablePort();
				portSlaveMap.remove(portId);
				slaveList.remove(slave);
				Collections.sort(slaveList);
				this.slaveCnt--;
			} finally {
				slave.slavePSMUnlock();
			}
			slave = null;
			log.info(
					"Port[Port ID = {} ] from SW={} is removed from LACP Bond Key={} with Virutal Mac={} at {}",
					new Object[] { HexEncode.longToHexString((long)portId),
							HexEncode.longToHexString(swId),
							HexEncode.longToHexString((long)this.adminKey), HexEncode.bytesToHexString(virtualSysMacAddr),
							new Date()
					});
		} finally {
			bondStateMachineUnlock();
		}
		log.info("bondDelSlave Exit"); 
	}

	
	
	LacpAggregator bondGetFreeAgg() {
	log.info("bondGetFreeAgg Entry"); 
		
    	if (aggregatorList == null || aggregatorList.size() == 0)
    		return null;

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.getNumOfPorts() == 0){
			log.info("bondGetFreeAgg found free aggregator"); 
    			return agg;
		}
    	}
	log.info("bondGetFreeAgg Exit"); 
		return null;		
	}
		
	
    LacpAggregator getActiveAgg(){
	log.info("getActiveAgg Entry"); 
    	if (aggregatorList == null || aggregatorList.size() == 0)
    		return null;

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.getIsActive() > 0){
			log.info("getActiveAgg - Found active agg"); 
    			return agg;
		}
    		
    	}
	log.info("getActiveAgg Exit"); 
	return null;
    }

		
    LacpAggregator findLacpAggByFitPort(LacpPort port) 
    {
	log.info("findLacpAggByFitPort Entry"); 
    	if (aggregatorList == null || aggregatorList.size() == 0)
    		return null;

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.isPortFitToAgg(port)) {
			log.info("findLacpAggByFitPort - found aggregator for port={}", port.slaveGetPortId());
			 return agg;
		}
	}
	log.info("findLacpAggByFitPort Exit"); 
	return null;
    } 
    

    public LacpConst.BOND_TYPE getAggSelectionMode() {
    	return this.select;
    }

    public  void setAggSelectionMode(LacpConst.BOND_TYPE selectionMode) {
    	 this.select = selectionMode;
    }
		
		
    public void bondUpdateLacpRate()
    {
	log.info("bondUpdateLacpRate Entry"); 

    	this.bondStateMachineLock();
	try{
    		this.setDirty(true);
		for (LacpPort data: slaveList) {
			data.slaveUpdateLacpRate(this.lacpFast);
		}
	}
	finally{
		this.bondStateMachineUnlock();
	}
	log.info("bondUpdateLacpRate Exit"); 
    }
    
    
   public void bondUpdateSystemPriority(short priority) 
    {
	log.info("bondUpdateSystemPriority Entry"); 
    	if (this.sysPriority == priority){
    		return;
	}
    	this.bondStateMachineLock();
    	try {
    		this.setDirty(true);
    		this.sysPriority = priority;
    		this.bondSetSysPriority(priority);
		for (LacpPort data: slaveList) {
			data.slaveSystemPriorityChange(priority);
		}
    	} finally {
		this.bondStateMachineUnlock();
    	}
	log.info("bondUpdateSystemPriority Exit"); 
    }

 
    public void bondAggSelectionLogic()
    {
	log.info("bondAggSelectionLogic Entry"); 

    	LacpAggregator best, active, agg, orig;
    	int i = 0;

    	Collections.sort(aggregatorList);
    	agg =   aggregatorList.get(i++);

    	active = this.getActiveAgg();
    	orig = active;
    	best = (active != null && active.aggDevUp()) ? active : null;

    	do {
    		agg.setIsActive((short)0);
    		if ((agg.getNumOfPorts() > 0)&& agg.aggDevUp()) {
    			best = LacpAggregator.aggregatorSelection(best, agg);

    		}
    		if (aggregatorList.size() > i){
    			agg =   aggregatorList.get(i++);
		}
    		else{
    			agg = null;
		}

    	} while (agg!=null);

    	if (best!=null && !best.existPortwithDist()){
    		best = null;	
	}
    	if (best!=null && getAggSelectionMode() == LacpConst.BOND_TYPE.BOND_STABLE){

    		if (active != null && (active.getLagPorts().size() > 0) &&  
    				(active.aggHasPartner() || 
    						((!active.aggHasPartner()) && (!best.aggHasPartner())))) {
    			if (!(((active.aggGetActorOperAggKey()==0) && (best.aggGetActorOperAggKey()>0)))) {
    				best = null;
    				active.setIsActive((short)1);
				log.info("bondAggSelectionLogic - active agg not null, setting the aggregator to active"); 
    			}
    		} else if (active == null) {
    			active = best;
    			best = null;
    			active.setIsActive((short)1);
			log.info("bondAggSelectionLogic - active agg is null, setting the active=best aggregator to active"); 
    		}
    	}

    	if (best!=null && (best == active)) {
    		best = null;
    		active.setIsActive((short)1);
		log.info("bondAggSelectionLogic - active == best, setting the aggregator to active"); 

    	}

    	if (best!=null) {

    		if (best.getIsIndiv()) 
    		{
    		}
    		best.setIsActive((short)1);
    		active = getActiveAgg();
		log.info("bondAggSelectionLogic - best!=null, setting the aggregator to active"); 
    	}  	
    	if (orig != active) {
    		log.info(
    				"Aggregator Reselection : Old Aggregator ID = {}, New Aggregator Id={} with Status[{}] and Number of Members={} at {}",
    				new Object[] { orig == null? "NULL":HexEncode.longToHexString((long)orig.getAggId()), 
    						active == null? "NULL" : HexEncode.longToHexString((long)active.getAggId()),
    								active == null? "N/A" : (active.getIsActive() > 0 ? "Active" : "Ready"),
    										active == null? "N/A" : active.getNumOfPorts(),
    												new Date()
    				});     
    		if (orig == null && active!=null){ 
    			this.activeSince = new Date();
		}
    		else if (active == null){
    			this.activeSince = null;
		}
    		this.setDirty(true);
    	}

	log.info("bondAggSelectionLogic Exit"); 
    }

	public boolean isLacpEnabled() {
		return isLacpEnabled;
	}

	public void setLacpEnabled(boolean enabled) {
	log.info("setLacpEnabled Entry"); 
		if (this.isLacpEnabled != enabled) {
			this.bondStateMachineLock();
			try {
				this.setDirty(true);
				this.isLacpEnabled = enabled;
				for (LacpPort port:this.slaveList) {
					port.slaveSetLacpPortEnabled(enabled);
				}
			} finally {
				this.bondStateMachineUnlock();
			}
		}
	log.info("setLacpEnabled Exit"); 
		
	}

	
	public boolean isPartnerExist(byte[] sysId, short key) {
		log.info("isPartnerExist Entry"); 
		for (LacpPort slave:slaveList) {
			if (slave.portPartnerOperGetKey() == key && Arrays.equals(sysId, slave.portPartnerOperGetSystem())){
				log.info("isPartnerExist - returning true"); 
				return true;
			}
		}
		log.info("isPartnerExist Exit"); 
		return false;
	}    
	
	public short findPortIdByPartnerMacPortId(byte[] sysId, short portId) {
		for (LacpPort slave:slaveList) {
			if (slave.portPartnerOperGetPortNumber() == portId && Arrays.equals(sysId, slave.portPartnerOperGetSystem())){
				return slave.slaveGetPortId();
			}
		}
		return 0;
	} 
	
	public short getVirtualPortId(long swId, short portNumber) {
		log.info("getVirtualPortId Entry"); 
		short result = 0;
		short systemId;
		
		if (this.systemIdMap.containsKey(swId)){
			systemId = systemIdMap.get(swId);
		}
		else {
			log.info("getVirtualPortId - VirtualPortId is not found for switch={} port number={}",swId,portNumber); 
			return 0;
		}
	
		result = (short) ((portNumber & 0x0fff) | (systemId << 12 & 0xf000));
		log.info("getVirtualPortId - VirtualPortId is found for switch={} port number={} and the value is={}",swId,portNumber,result); 
		log.info("getVirtualPortId Exit"); 
		return result;
	}

	public void bondUpdateLinkUpSlave(long swId, short portId,
			int currentFeatures) {
		log.info("bondUpdateLinkUpSlave Entry"); 
		this.bondStateMachineLock();
		try {
			if (portId != 0) {
				int result = currentFeatures;
				int speed = (result >> LacpConst.DUPLEX_KEY_BITS);
				byte duplex = (byte) (result & LacpConst.DUPLEX_KEY_BITS);
				log.info("bondUpdateLinkUpSlave : currentFeatures={}, speed={}",
						String.format("%x", currentFeatures), String.format("%x", speed));
				LacpPort slave = portSlaveMap.get(portId);
				slave.slavePSMLock();
				try {
					slave.slaveSetSpeed(speed);
					slave.slaveSetDuplex(duplex);
					slave.slaveHandleLinkChange(LacpConst.BOND_LINK_UP);
				} finally {
					slave.slavePSMUnlock();
				}
				log.info("LACP Port [ PortId={}, Virtual={} ] in SW={} Link Up at {}",
						HexEncode.longToHexString((long)portId),
						HexEncode.longToHexString(swId), new Date());
			} else {
				log.info("bondUpdateLinkUpSlave:Virtual Port is 0");
			}
		} finally {
			this.bondStateMachineUnlock();
		}
		log.info("bondUpdateLinkUpSlave Exit"); 
	}

	public void bondUpdateLinkDownSlave(long swId, short portId) {
		log.info("bondUpdateLinkDownSlave Entry"); 
		this.bondStateMachineLock();
		try {
			if (portId != 0) {
				
				LacpPort slave = portSlaveMap.get(portId);
				slave.slavePSMLock();
				try {
					slave.slaveSetSpeed((byte)0);
					slave.slaveSetDuplex((byte)0);
					slave.slaveHandleLinkChange(LacpConst.BOND_LINK_DOWN);
					log.info("LACP Port [ PortId={} ] in SW={} Link Down at {}",
						HexEncode.longToHexString((long)portId), 
						HexEncode.longToHexString(swId), new Date());
				} finally {
					slave.slavePSMUnlock();	
				}
			}
		} finally {
			this.bondStateMachineUnlock();
		}
		log.info("bondUpdateLinkDownSlave Exit"); 
	}
	
	public void bondDelMembersFrSw(long swId) {
		log.info("bondDelMembersFrSw Entry"); 
		this.bondStateMachineLock();
		try {
			this.setDirty(true);
			HashSet<LacpPort> set = new HashSet<LacpPort>();
			if (systemIdMap.containsKey(swId)) {
				for (LacpPort node: slaveList) {
					if (node.slaveGetSwId() == swId)
						set.add(node);
				}
				for (LacpPort node: set) {
					 this.bondDelSlave(swId, node.slaveGetPortId());
				}
				set.clear();
			}
		} finally {
			this.bondStateMachineUnlock();
		}
		log.info("bondDelMembersFrSw Exit"); 
	}

	public short bondGetAggId(long swId, short portId) {
		log.info("bondGetAggId Entry"); 
		this.bondStateMachineLock();
		try {
			LacpPort slave = portSlaveMap.get(portId);
			if (slave != null &&  slave.getPortAggregator()!=null)
			{
				log.info("bondGetAggId - returning AggId= {}",slave.getPortAggregator().getAggId()); 
				return (slave.getPortAggregator().getAggId());
			}
			else{
				log.info("bondGetAggId - returning AggId= {} as slave is null",0); 
				return 0;
			}
		}
		finally {
			this.bondStateMachineUnlock();
			log.info("bondGetAggId Exit"); 
		}
	}
	
	public Set<Short> bondGetAggId(long swId) {
		this.bondStateMachineLock();
		Set<Short> result = new TreeSet<Short>();
		try {
			if (systemIdMap.get(swId)!= null) {
				for (LacpPort slave: slaveList) {
					if (slave.slaveGetSwId() == swId && slave.getPortAggregator()!=null) {
						result.add(slave.getPortAggregator().getAggId());
					}
				}
				return result;
			}
			return result;
		}
		finally {
			this.bondStateMachineUnlock();
		}
		
	}
	
	public boolean bondHasMember(long swId) {
		this.bondStateMachineLock();
		try {
			if (systemIdMap.containsKey(swId))
                        {
				return true;
                        }
			return false;
		} finally {
			this.bondStateMachineUnlock();			
		}
	}	
	
	public boolean bondHasMember() {
		this.bondStateMachineLock();
		try {
			return (slaveList.isEmpty());
		} finally {
			this.bondStateMachineUnlock();			
		}
	}
	
	public int bondNumMembersInSw(long swId) {
		short result = 0;
		this.bondStateMachineLock();
		try {
		if (!systemIdMap.containsKey(swId))
			return result;
		for (LacpPort port : this.slaveList)
			if (port.slaveGetSwId() == swId)
				result++;
		return result;
		} finally {
			this.bondStateMachineUnlock();		
		}
	}
	
	short getBondActiveAggId() {
		if (getActiveAgg()!=null)
			return getActiveAgg().getAggId();
		else 
			return 0;
	}
    public void updateLacpAggregatorsDS ()
    {
        NodeConnectorRef ncRef;
        DataBroker dataService = LacpUtil.getDataBrokerService();

        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        // TODO KALAI fill other fields also.

        LacpAggregator lacpAgg = getActiveAgg();

        MacAddress mac = new MacAddress(HexEncode.bytesToHexStringFormat(lacpAgg.getAggMacAddress()));
        lacpAggBuilder.setActorAggMacAddress(mac);
        int actorKey = lacpAgg.getActorOperAggKey();
        lacpAggBuilder.setActorOperAggKey(actorKey);
        lacpAggBuilder.setKey(new LacpAggregatorsKey(bondInstanceId));
       // lacpAggBuilder.setPartnerAggMacAddress(new MacAddress(lacp
        int partnerKey = lacpAgg.getPartnerOperAggKey();
        lacpAggBuilder.setPartnerOperAggKey(partnerKey);
        MacAddress pMac = new MacAddress(HexEncode.bytesToHexStringFormat(lacpAgg.getPartnerSystem()));
        lacpAggBuilder.setPartnerSystemId(pMac);
        int partPrio = lacpAgg.getPartnerSystemPriority();
        lacpAggBuilder.setPartnerSystemPriority(partPrio);
        ListOfLagPortsBuilder lagPortBuilder = new ListOfLagPortsBuilder();
        List<ListOfLagPorts> lagPortList = new ArrayList<ListOfLagPorts>();
        for(LacpPort lacpPortTmp : activePortList)
        {
            ncRef = new NodeConnectorRef (lacpPortTmp.getNodeConnectorId());
            lagPortBuilder.setLacpPortRef(ncRef);
            lagPortList.add(lagPortBuilder.build());
        }
        lacpAggBuilder.setListOfLagPorts(lagPortList);
        LacpAggregators lacpAggs = lacpAggBuilder.build();

        write.merge(LogicalDatastoreType.OPERATIONAL, aggInstId, lacpAggs, true);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess(Object o)
            {
                log.info("LacpAggregators updation write success for txt {}", write.getIdentifier());
            }
            @Override
            public void onFailure(Throwable throwable)
            {
                log.error("LacpAggregators updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
    }
    public boolean addActivePort (LacpPort lacpPort)
    {
        if (activePortList.contains (lacpPort))
        {
            return false;
        }
        activePortList.add (lacpPort);
        lacpGroupTbl.lacpAddPort(true, new NodeConnectorRef(lacpPort.getNodeConnectorId()), aggGrpId);
        updateLacpAggregatorsDS();
        if (activePortList.size() <= 1)
        {
            LacpLogPort.createLogicalPort(this);
        }
        else
        {
            lacpPort.setLogicalNCRef(logNodeConnRef);
        }
        return true;
    }
    public boolean removeActivePort (LacpPort lacpPort)
    {
        if (!(activePortList.contains (lacpPort)))
        {
            return false;
        }
        activePortList.remove (lacpPort);
        lacpGroupTbl.lacpRemPort(aggGrpId, new NodeConnectorRef(lacpPort.getNodeConnectorId()), true);
        updateLacpAggregatorsDS();
        if (activePortList.size() == 0)
        {
            LacpLogPort.deleteLogicalPort(this);
        }
        return true;
    }
    public LacpNodeExtn getLacpNode()
    {
        return lacpNodeRef;
    }
    public void setLogicalNCRef (NodeConnectorRef ncRef)
    {
        logNodeConnRef = ncRef;
    }
    public NodeConnectorRef getLogicalNCRef ()
    {
        return logNodeConnRef;
    }
    public List getActivePortList()
    {
        return activePortList;
    }
    public LacpAggregators buildLacpAgg ()
    {  
        return lacpAggBuilder.build();
    }
    public InstanceIdentifier getLacpAggInstId()
    {
        return aggInstId;
    }
    
}
