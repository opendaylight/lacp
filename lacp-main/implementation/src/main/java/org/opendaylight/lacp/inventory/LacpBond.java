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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.lacpaggregator.LagPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.lacpaggregator.LagPortsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.lacpaggregator.LagPorts;
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

	private static final Logger LOG = LoggerFactory.getLogger(LacpBond.class);
	private int slaveCnt;  /* default 1 slave port per bond */
	private List<LacpPort> slaveList;
	private LinkedHashMap<Short, LacpPort> portSlaveMap;
	private int minLinks;
	private int maximumLink = 8;  /* maximum links per each system for this bond */
	private int bondId;
	private ReentrantLock bondLock;
	private Date activeSince;
	
	private int sysPriority; 
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
    private Group lagGroup;

	public byte[] getVirtualSysMacAddr() {
		return virtualSysMacAddr;
	}
	public void setVirtualSysMacAddr(byte[] virtualSysMacAddr) {
		this.virtualSysMacAddr = Arrays.copyOf(virtualSysMacAddr, LacpConst.ETH_ADDR_LEN);
	}
    public byte[] getBondSystemId()
    {
        byte[] systemId = HexEncode.bytesFromHexString(this.lacpNodeRef.getNodeSystemId().getValue());
        return systemId;
    }
	public List<LacpPort> getSlaveList() {
		return slaveList;
	}
	public LinkedHashMap<Short, LacpPort> getPortSlaveMap() {
		return portSlaveMap;
	}
	public int getSysPriority() {
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
	public void setSysPriority(int sysPriority) {
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
		
		return new LacpBond(0x0000ffff,key, lacpNode);
	}

	public static LacpBond newInstance(int sysPri,short key, LacpNodeExtn lacpNode) {
		
		return new LacpBond(sysPri,key, lacpNode);
	}
	
	private LacpBond(int sys_priority,short key, LacpNodeExtn lacpNode) 
	{
		LOG.debug("LacpBond is created with sys priority ={} and key={}",sys_priority,key); 

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
		this.activeSince = null;
		this.dirty = true;

        lacpNodeRef = lacpNode;
        lacpAggBuilder = new LacpAggregatorsBuilder();
        NodeRef node = new NodeRef(lacpNode.getNodeId());
        lacpAggBuilder.setLagNodeRef(node);
        logNodeConnRef = null;
        InstanceIdentifier<Node> nodeId = lacpNode.getNodeId();
        NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
        bondInstanceId = lacpNode.getAndIncrementNextAggId();
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
        lacpAggBuilder.setKey(new LacpAggregatorsKey(bondInstanceId));
        lagGroup = null;
	}
	
	public int bondGetSysPriority() {
		return sysPriority;
	}

	public void bondSetSysPriority(int sysPri) {
		this.sysPriority = sysPri;

	}

	int checkAggSelectTimer() {
		return (this.aggSelectTimer > 0 ? 1 : 0);
		
	}
	
	public void bondAddSlave(long swId, short portId, int portPri,LacpBpduInfo bpduInfo)
    {
		byte[] macAddr;
		short systemId = 0;
		
        macAddr = HexEncode.bytesFromHexString(this.lacpNodeRef.getNodeSystemId().getValue());
		bondStateMachineLock();
		try {
			setDirty(true);
		if (this.systemIdMap.containsKey(swId)){
			systemId = systemIdMap.get(swId);
		}
		else {
			if (systemIdMap.size() != 0) {
				for (short value : systemIdMap.values()) {
					if (value > systemId){
						systemId = value;
					}
				}
			}
			systemId ++;
			this.systemIdMap.put(swId, systemId);
		}
        if (slaveList.isEmpty())
        {
			/* Set Virtual MAC address for Bond */
			this.virtualSysMacAddr = Arrays.copyOf(macAddr, LacpConst.ETH_ADDR_LEN);
			this.virtualSysMacAddr[5] += bondInstanceId;			
		}
		
		LacpPort slave = LacpPort.newInstance((long)swId,portId, this, portPri, bpduInfo);

        /* When the 1st port is added to the bond, get portkey from port
         *  and save it as the key value for the bond. For the subsequent 
         *  ports, verify if the portkey matches with the bond key value */
        if (slaveList.isEmpty())
        {
		    this.adminKey = slave.getActorAdminPortKey();
        }
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

		LOG.info(
				"Port[Port ID = " + portId +  
				"] from SW= " +HexEncode.longToHexString(swId) +"  is added into LACP Bond Key=" + this.adminKey +
				" with Virutal Mac=" + HexEncode.bytesToHexString(virtualSysMacAddr));
		} finally {
			bondStateMachineUnlock();	
		}
	}
	

	
	public void bondDelSlave(long swId, short portId) {
		
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
			LOG.info(
					"Port[Port ID = {} ] from SW={} is removed from LACP Bond Key={} with Virutal Mac={} at {}",
					new Object[] { HexEncode.longToHexString((long)portId),
							HexEncode.longToHexString(swId),
							HexEncode.longToHexString((long)this.adminKey), HexEncode.bytesToHexString(virtualSysMacAddr),
							new Date()
					});
		} finally {
			bondStateMachineUnlock();
		}
	}

	
	
	LacpAggregator bondGetFreeAgg() {
		
    	if (aggregatorList == null || aggregatorList.size() == 0){
    		return null;
	}

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.getNumOfPorts() == 0){
			LOG.debug("bondGetFreeAgg found free aggregator"); 
    			return agg;
		}
    	}
		return null;		
	}
		
	
    public LacpAggregator getActiveAgg(){
    	if (aggregatorList == null || aggregatorList.size() == 0){
    		return null;
	}

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.getIsActive() > 0){
			LOG.debug("getActiveAgg - Found active agg"); 
    			return agg;
		}
    		
    	}
	return null;
    }

		
    LacpAggregator findLacpAggByFitPort(LacpPort port) 
    {
    	if (aggregatorList == null || aggregatorList.size() == 0){
    		return null;
	}

    	for (LacpAggregator agg : aggregatorList) {
    		if (agg.isPortFitToAgg(port)) {
			LOG.debug("findLacpAggByFitPort - found aggregator for port={}", port.slaveGetPortId());
			 return agg;
		}
	}
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
    }
    
    
   public void bondUpdateSystemPriority(int priority) 
    {
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
    }

 
    public void bondAggSelectionLogic()
    {

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
				LOG.debug("bondAggSelectionLogic - active agg not null, setting the aggregator to active"); 
    			}
    		} else if (active == null) {
    			active = best;
    			best = null;
    			active.setIsActive((short)1);
			LOG.debug("bondAggSelectionLogic - active agg is null, setting the active=best aggregator to active"); 
    		}
    	}

    	if (best!=null && (best == active)) {
    		best = null;
    		active.setIsActive((short)1);
		LOG.debug("bondAggSelectionLogic - active == best, setting the aggregator to active"); 

    	}

    	if (best!=null) {

    		if (best.getIsIndiv()) 
    		{
    		}
    		best.setIsActive((short)1);
    		active = getActiveAgg();
		LOG.debug("bondAggSelectionLogic - best!=null, setting the aggregator to active"); 
    	}  	
    	if (orig != active) {
    		LOG.info(
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

    }

	public boolean isLacpEnabled() {
		return isLacpEnabled;
	}

	public void setLacpEnabled(boolean enabled) {
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
		
	}

	
	public boolean isPartnerExist(byte[] sysId, short key) {
		for (LacpPort slave:slaveList) {
			if (slave.portPartnerOperGetKey() == key && Arrays.equals(sysId, slave.portPartnerOperGetSystem())){
				LOG.debug("isPartnerExist - returning true"); 
				return true;
			}
		}
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
		short result = 0;
		short systemId;
		
		if (this.systemIdMap.containsKey(swId)){
			systemId = systemIdMap.get(swId);
		}
		else {
			return 0;
		}
	
		result = (short) ((portNumber & 0x0fff) | (systemId << 12 & 0xf000));
		return result;
	}

	public void bondUpdateLinkUpSlave(long swId, short portId)
    {
		this.bondStateMachineLock();
		try {
			if (portId != 0) {
				LOG.debug("entering bondUpdateLinkUpSlave");
				LacpPort slave = portSlaveMap.get(portId);
				slave.slavePSMLock();
				try {
					slave.slaveHandleLinkChange(LacpConst.BOND_LINK_UP);
				} finally {
					slave.slavePSMUnlock();
				}
				LOG.info("LACP Port [ PortId={}, Virtual={} ] in SW={} Link Up at {}",
						HexEncode.longToHexString((long)portId),
						HexEncode.longToHexString(swId), new Date());
			} else {
				LOG.debug("bondUpdateLinkUpSlave:Port is 0");
			}
		} finally {
			this.bondStateMachineUnlock();
		}
	}

	public void bondUpdateLinkDownSlave(long swId, short portId) {
		this.bondStateMachineLock();
		try {
			if (portId != 0) {
				
				LacpPort slave = portSlaveMap.get(portId);
				slave.slavePSMLock();
				try {
					slave.slaveHandleLinkChange(LacpConst.BOND_LINK_DOWN);
					LOG.info("LACP Port [ PortId={} ] in SW={} Link Down at {}",
						HexEncode.longToHexString((long)portId), 
						HexEncode.longToHexString(swId), new Date());
				} finally {
					slave.slavePSMUnlock();	
				}
			}
		} finally {
			this.bondStateMachineUnlock();
		}
	}
	
	public void bondDelMembersFrSw(long swId) {
		this.bondStateMachineLock();
		try {
			this.setDirty(true);
			HashSet<LacpPort> set = new HashSet<LacpPort>();
			if (systemIdMap.containsKey(swId)) {
				for (LacpPort node: slaveList) {
					if (node.slaveGetSwId() == swId){
						set.add(node);
					}
				}
				for (LacpPort node: set) {
					 this.bondDelSlave(swId, node.slaveGetPortId());
				}
				set.clear();
			}
		} finally {
			this.bondStateMachineUnlock();
		}
	}

	public short bondGetAggId(long swId, short portId) {
		this.bondStateMachineLock();
		try {
			LacpPort slave = portSlaveMap.get(portId);
			if (slave != null &&  slave.getPortAggregator()!=null)
			{
				LOG.debug("bondGetAggId - returning AggId= {}",slave.getPortAggregator().getAggId()); 
				return (slave.getPortAggregator().getAggId());
			}
			else{
				LOG.debug("bondGetAggId - returning AggId= {} as slave is null",0); 
				return 0;
			}
		}
		finally {
			this.bondStateMachineUnlock();
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
		if (!systemIdMap.containsKey(swId)){
			return result;
		}
		for (LacpPort port : this.slaveList){
			if (port.slaveGetSwId() == swId){
				result++;
			}
		}
		return result;
		} finally {
			this.bondStateMachineUnlock();		
		}
	}
	
	short getBondActiveAggId() {
		if (getActiveAgg()!=null){
			return getActiveAgg().getAggId();
		}
		else {
			return 0;
		}
	}
    public void updateLacpAggregatorsDS ()
    {
        NodeConnectorRef ncRef;
        DataBroker dataService = LacpUtil.getDataBrokerService();

        final WriteTransaction write = dataService.newWriteOnlyTransaction();

        if (lacpNodeRef.getLacpNodeDeleteStatus() == true)
        {
            LOG.debug ("updation of the LACP Aggregator DS is skipped as the node is in process of deletion");
            return;
        }
        LOG.debug ("entering updateLacpAggregators for bond {} ", bondInstanceId);
        LacpAggregators lacpAggs = lacpAggBuilder.build();
        
        LOG.debug ("writing the add ds");
        write.merge(LogicalDatastoreType.OPERATIONAL, aggInstId, lacpAggs, true);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess(Object o)
            {
                LOG.info("LacpAggregators updation write success for txt {}", write.getIdentifier());
            }
            @Override
            public void onFailure(Throwable throwable)
            {
                LOG.error("LacpAggregators updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
        LOG.debug ("exiting updateLacpAggregators");
    }
    public void deleteLacpAggregatorDS (InstanceIdentifier instId)
    {
        if (lacpNodeRef.getLacpNodeDeleteStatus() == true)
        {
            LOG.debug ("updation of the LACP Aggregator DS is skipped as the node is in process of deletion");
            return;
        }
        DataBroker dataService = LacpUtil.getDataBrokerService();
        final WriteTransaction write = dataService.newWriteOnlyTransaction();

        LOG.debug ("deleting/updating the agg ds for bond {} for {}", bondInstanceId, instId);
        write.delete(LogicalDatastoreType.OPERATIONAL, instId);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess(Object o)
            {
                LOG.info("LacpAggregators deletion write success for txt {}", write.getIdentifier());
            }
            @Override
            public void onFailure(Throwable throwable)
            {
                LOG.error("LacpAggregators deletion write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
    }
    public boolean addActivePort (LacpPort lacpPort)
    {
        List <LagPorts> lagPortList;
        LOG.debug ("entring addActivePort for {}", lacpPort.getNodeConnectorId());
        if (activePortList.contains (lacpPort))
        {
            LOG.debug ("port {} is already present. returning false ", lacpPort.getNodeConnectorId());
            return false;
        }
        LOG.debug ("adding port {} to bond{} ", lacpPort.getNodeConnectorId(), aggInstId);
        activePortList.add (lacpPort);
        LagPortsBuilder lagPort = new LagPortsBuilder();
        long portId = lacpPort.slaveGetPortId();
        lagPort.setKey (new LagPortsKey(portId));
        lagPort.setLagPortId(portId);
        NodeConnectorRef ncRef = new NodeConnectorRef (lacpPort.getNodeConnectorId());
        lagPort.setLagPortRef (ncRef);

        if (activePortList.size() <= 1)
        {
            LOG.debug ("creating the logical port and adding lag group ");
            LacpLogPort.createLogicalPort(this);
            lagGroup = lacpGroupTbl.lacpAddGroup (true, new NodeConnectorRef(lacpPort.getNodeConnectorId()), aggGrpId);
            lacpNodeRef.addLacpAggregator(this);
            LacpAggregator lacpAgg = getActiveAgg();

            MacAddress mac = new MacAddress(HexEncode.bytesToHexStringFormat(lacpAgg.getAggMacAddress()));
            lacpAggBuilder.setActorAggMacAddress(mac);
            int actorKey = lacpAgg.getActorOperAggKey();
            lacpAggBuilder.setActorOperAggKey(actorKey);
            int partnerKey = lacpAgg.getPartnerOperAggKey();
            lacpAggBuilder.setPartnerOperAggKey(partnerKey);
            MacAddress pMac = new MacAddress(HexEncode.bytesToHexStringFormat(lacpAgg.getPartnerSystem()));
            lacpAggBuilder.setPartnerSystemId(pMac);
            lacpAggBuilder.setPartnerSystemPriority(lacpAgg.getPartnerSystemPriority());
    
            lagPortList = new ArrayList<LagPorts>();
            lagPortList.add(lagPort.build());
        }
        else
        {
            LOG.debug ("setting NCRef and adding port to lag group");
            lacpPort.setLogicalNCRef(logNodeConnRef);
            lagGroup = lacpGroupTbl.lacpAddPort(true, new NodeConnectorRef(lacpPort.getNodeConnectorId()), lagGroup);
            lagPortList = lacpAggBuilder.getLagPorts();
            lagPortList.add(lagPort.build());
        }
        lacpAggBuilder.setLagPorts(lagPortList);
        updateLacpAggregatorsDS();
        return true;
    }
    public boolean removeActivePort (LacpPort lacpPort)
    {
        LOG.debug ("in removeActivePort for {}, active portlist size {}", lacpPort.getNodeConnectorId(), activePortList.size());
        if (!(activePortList.contains (lacpPort)))
        {
            LOG.debug ("port {} is not present. returning false ", lacpPort.getNodeConnectorId());
            return false;
        }
        lacpPort.resetLacpParams();
        if (activePortList.size() == 1)
        {
            lacpNodeRef.removeLacpAggregator(this);
            activePortList.remove (lacpPort);
            LacpLogPort.deleteLogicalPort(this);
            lacpGroupTbl.lacpRemGroup (true, new NodeConnectorRef(lacpPort.getNodeConnectorId()), aggGrpId);
            deleteLacpAggregatorDS(this.aggInstId);
            LOG.debug ("cleaned up the aggregator info for agg {}", aggInstId);
        }
        else
        {
            activePortList.remove (lacpPort);
    	    lagGroup = lacpGroupTbl.lacpRemPort (lagGroup, new NodeConnectorRef(lacpPort.getNodeConnectorId()), true);
            InstanceIdentifier<Node> nodeId = lacpNodeRef.getNodeId();
            NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
            long portId = lacpPort.slaveGetPortId();
            InstanceIdentifier instId = InstanceIdentifier.builder(Nodes.class)
                .child (Node.class, new NodeKey (nId))
                .augmentation(LacpNode.class)
                .child (LacpAggregators.class, new LacpAggregatorsKey(bondInstanceId))
                .child (LagPorts.class, new LagPortsKey(portId)).toInstance();

            deleteLacpAggregatorDS(instId);
        }
        synchronized (lacpNodeRef)
        {
            lacpNodeRef.removeLacpPort(lacpPort.getNodeConnectorId(), false);
            if (lacpPort.getPortOperStatus() == true)
            {
                LOG.debug("removing the port as lacp port and adding as non-lacp port for port {}", lacpPort.getNodeConnectorId());
                lacpNodeRef.addNonLacpPort(lacpPort.getNodeConnectorId());
            }
            else
            {
                LOG.debug("removing the port as lacp port and not adding as non-lacp port for port {}", lacpPort.getNodeConnectorId());
            }
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
    public LacpPort getSlavePortObject(short portId){
            return portSlaveMap.get(portId);
    }

    public void lacpBondCleanup()
    {
        bondStateMachineLock();
        for (LacpPort lacpPort :slaveList)
        {
            lacpPort.slavePSMLock();
            try
            {
                lacpPort.lacpDisablePort();
                portSlaveMap.remove(lacpPort.slaveGetPortId());
            }
            finally
            {
                lacpPort.slavePSMUnlock();
            }
            LOG.debug("Port {} is removed from LACP Bond Key {}", lacpPort.getNodeConnectorId(), this.adminKey);
        }
        slaveList.clear();
        this.slaveCnt = 0;
        bondStateMachineUnlock();
   }
}
