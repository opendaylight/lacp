/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.lacp.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//INTEGRATION WITH YANG GENERATED LACPAGGREGATOR - START
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LagId;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.inventory.LacpPort;

//INTEGRATION WITH YANG GENERATED LACPAGGREGATOR - END

public class LacpAggregator implements Comparable<LacpAggregator> {

	
	private static final Logger log = LoggerFactory.getLogger(LacpAggregator.class);

	private static int id = 1;
	private byte[] aggMacAddress;
	private short aggId;
	private boolean isIndiv;
	private short actorAdminAggKey;
	private short actorOperAggKey;
	private byte[] partnerSystem;
	private int partnerSystemPriority;
	private short partnerOperAggKey;
	short receiveState;
	short transmitState; 
	private List<LacpPort> lagPortList;
	
	private List<LacpPort> standByPorts;
	private LagId aggLagId;  /* Operational LagId */
	private short numOfPorts;
	private short numOfStandbyPort;
	private short isActive;
	private boolean reselect;
	private LacpBond bond;
	
	public boolean isReselect() {
		return reselect;
	}

	public void setReselect(boolean reselect) {
		this.reselect = reselect;
	}

	public short aggGetActorOperAggKey() {
		return getActorOperAggKey();
	}

	public void aggSetActorOperAggKey(short key) {
		this.setActorOperAggKey(key);
	}


	public short aggGetPartnerOperAggKey() {
		return getPartnerOperAggKey();
	}
	
	public LacpBond aggGetBond() {
		return getBond();
	}

	public void aggSetBond(LacpBond bond) {
		this.setBond(bond);
	}


	
	public List<LacpPort> getLagPorts() {
		return getLagPortList();
	}
	

	public short getNumOfPorts() {
		return numOfPorts;
	}

	private  LacpAggregator() {
		
			this.setAggId((short) id);
			if (++id > 0xffff) {
				id = 1;
			}
	}
	
	public static LacpAggregator newInstance() {
		return new LacpAggregator();
	}
	
	public boolean aggHasPartner() {
		if (getPartnerSystem()==null){
			return false;
		}
		if (!Arrays.equals(getPartnerSystem(), LacpConst.NULL_MAC_ADDRESS)){
			return true;
		}
		return false;
	}
	
	
	public boolean aggPartnerIsNullMac() {
		if (getPartnerSystem() == null){
			 return false;
		}
		if (Arrays.equals(getPartnerSystem(),LacpConst.NULL_MAC_ADDRESS)){
			return true;
		}
		return false;
	}
	
	public boolean aggHasPort(LacpPort port) {
		if (getLagPortList() != null && getLagPortList().contains(port)){
			return true;
		}
		return false;
	}
	
	public boolean aggHasStandbyPort(LacpPort port) {
		if (getStandByPorts() != null && getStandByPorts().contains(port)){
			return true;
		}
		return false;
	}
	
	public short getAggId() {
		return (this.aggId);
	}
	
	boolean getIsIndiv() {
		return this.isIndiv();
	}
	
	void setIsIndiv(boolean val) {
		this.setIndiv(val);
	}
	
	void setAggPortsReady(int val) 
	{
		if (getLagPortList() == null){
		 return;
		}
		for (LacpPort port : getLagPortList()){
			port.setPortsReady(val);
		}
	}

	int getAggPortsReady() {
		if (getLagPortList() == null){
		 return 0;
		}
		for (LacpPort port : getLagPortList()) {
			if (port.getPortsReady() == 0) {
				return(0);
			}
		}		
		return(1);
	}
	
	
	
	int getAggBandwidth() {
		int bandwidth = 0;
		LacpPort port = null;
		if (this.getNumOfPorts() > 0) {
			port = this.getLagPortList().get(0);
			switch (port.getLinkSpeed()) {
				case LacpConst.LINK_SPEED_BITMASK_10MBPS:
					bandwidth = this.getNumOfPorts() * 10;
					break;
				case LacpConst.LINK_SPEED_BITMASK_100MBPS:
					bandwidth = this.getNumOfPorts() * 100;
					break;
				case LacpConst.LINK_SPEED_BITMASK_1000MBPS:
					bandwidth = this.getNumOfPorts() * 1000;
					break;
				case LacpConst.LINK_SPEED_BITMASK_10000MBPS:
					bandwidth = this.getNumOfPorts() * 10000;
					break;
				case LacpConst.LINK_SPEED_BITMASK_40000MBPS:
					bandwidth = this.getNumOfPorts() * 40000;
					break;
				default:
					bandwidth = 0; 
			}
		}
		return  bandwidth;
	}
	
	public short getIsActive() {
		return this.isActive;
	}
	
	public void setIsActive(short val) {
		this.isActive = val;
	}
	
	void rmPortFromAgg(LacpPort port) {
		if (getLagPortList() != null && getLagPortList().size()> 0) {
			getLagPortList().remove(port);
			this.setNumOfPorts((short)(this.getNumOfPorts() - 1));
			if (getLagPortList().size() == 0) {
				log.debug("LacpAggregator rmPortFromAgg has only one port hence clearing aggregator");
				clearAgg();
			}else{
				Collections.sort(getLagPortList());
			}
		}		
	}
	
	void addPortToAgg(LacpPort port) {
		if (getLagPortList() == null) {
			setLagPortList(new ArrayList<LacpPort>());		
		}
		if (getLagPortList().contains(port)){
			log.debug("Port is already present in the aggregator");
			return;
		}			
		getLagPortList().add(port);
		this.setNumOfPorts((short)(this.getNumOfPorts() + 1));
		Collections.sort(getLagPortList());
	}
	
	
	void addPortToAggStandBy(LacpPort port) {
		if (getStandByPorts() == null) {
			setStandbyPorts(new ArrayList<LacpPort>());		
		}
		if (getStandByPorts().contains(port)){
			log.debug("LacpAggregator addPortToAggStandBy port={} is already present as a standby", 
					port.slaveGetPortId());
			return;
		}
		getStandByPorts().add(port);
		log.debug("LacpAggregator addPortToAggStandBy port ={} is added to agg standby");
		this.setNumOfStandbyPort((short)(this.getNumOfStandbyPort() + 1));
		Collections.sort(getStandByPorts());
	}

	void rmPortFromAggStandBy(LacpPort port) {
		if (getStandByPorts() != null && getStandByPorts().size()> 0) {
			getStandByPorts().remove(port);
			log.debug("Remove port={} from agg standby", port.slaveGetPortId());
			this.setNumOfStandbyPort((short)(this.getNumOfStandbyPort() - 1));
			Collections.sort(getStandByPorts());
		}		
	}		
	
	LacpPort getLastPortFromAggStandBy() {
		if (getStandByPorts() != null && getStandByPorts().size() > 0) {
			log.debug("LacpAggregator getLastPortFromAggStandBy returning - last port from standby");
			return(getStandByPorts().get(getStandByPorts().size()-1));
		}
		else{
			log.debug("LacpAggregator getLastPortFromAggStandBy returning - no ports in standby");
			return null;
		}
	}
	
	LacpPort getLastPortFromAgg() {
		if (getLagPortList() != null && getLagPortList().size() > 0) {
			log.debug("LacpAggregator getLastPortFromAgg returning - last port from agg");
			return(getLagPortList().get(getLagPortList().size()-1));
		}
		else{
			log.debug("LacpAggregator getLastPortFromAgg returning null");
			return null;
		}
	}
	
	void clearAgg() {
		setIndiv(false);
		setActorAdminAggregatorKey((short)0);
		setActorOperAggKey((short)0);
		setPartnerSystem(Arrays.copyOf(LacpConst.NULL_MAC_ADDRESS, LacpConst.ETH_ADDR_LEN));
		setPartnerSystemPriority((int)0);
		setPartnerOperAggKey((short)0);
		this.setReceiveState((short)0);
		this.setTransmitState((short)0);
		setLagPortList(new ArrayList<LacpPort>());
		setStandbyPorts(new ArrayList<LacpPort>());
		isActive = 0;
		setNumOfPorts((short)0);	
		setNumOfStandbyPort((short)0);
		reselect = false;
		this.setAggLagId(null);
	}
	
	void initAgg() {
		clearAgg();
		setAggMacAddress(Arrays.copyOf(LacpConst.NULL_MAC_ADDRESS, LacpConst.ETH_ADDR_LEN));
		// aggregate_identifier = 0;
	}
	
	void copyAggInfoFromPort(LacpPort port) 
	{
		this.setIndiv(port.get_Duplex()> 0 ? false : true);
		this.setActorAdminAggregatorKey(port.getActorAdminPortKey());
		this.setActorOperAggKey(port.getActorOperPortKey());
		this.setPartnerSystem(Arrays.copyOf(port.portPartnerOperGetSystem(),LacpConst.ETH_ADDR_LEN));
		this.setPartnerSystemPriority(port.portPartnerOperGetSystemPriority());
		this.setPartnerOperAggKey(port.portPartnerOperGetKey());
		this.setReceiveState((short)1);
		this.setTransmitState((short)1);
		this.setLagPortList(new ArrayList<LacpPort>());
		this.setStandbyPorts(new ArrayList<LacpPort>());
		this.setNumOfStandbyPort((short)0);
		this.setAggLagId(new LagId(port.portGetLagId()));			
	}
	
	boolean isPortFitToAgg(LacpPort port) 
	{
		if (this.getAggLagId() != null && this.getAggLagId().compareToPartial(port.portGetLagId())==0 && port.portGetLagId().isNeighborFound() && (!this.isIndiv())) {
			log.debug("isPortFitToAgg returned true");
			return true;

		}
		log.debug("isPortFitToAgg returned false");
		return false;

	}
	
	boolean aggDevUp() 
	{
		Byte status;
		if (this.getLagPortList() != null && this.getLagPortList().size()> 0) {
			for (LacpPort temp: this.getLagPortList() ) {
				if (temp!= null && temp.isInitialized) {
					status  = temp.portGetPortStatus();
					if (status == LacpConst.BOND_LINK_UP  || status ==LacpConst.BOND_LINK_BACK ) {
						log.debug("aggDevUp returned true");
						return true;
					}
				}
			}
		}
		log.debug("aggDevUp returned false");
		return false;
	}
	
	
	static LacpAggregator aggregatorSelection(LacpAggregator aggBest,LacpAggregator aggCurrent)
	{

		if (aggBest == null){
			return aggCurrent;
		}
		else if (aggCurrent == null){
			return  aggBest;
		}
		if (!aggCurrent.isIndiv() && aggBest.isIndiv()){
			return aggCurrent;
		}
		if (aggCurrent.isIndiv() && !aggBest.isIndiv()){
			return aggBest;
		}
		if (aggCurrent.aggHasPartner()&& !aggBest.aggHasPartner()){
			return aggCurrent;
		}
		if (!aggCurrent.aggHasPartner() && aggBest.aggHasPartner()){
			return aggBest;
		}
		switch (aggCurrent.aggGetBond().getAggSelectionMode()) {
			case BOND_COUNT:
				if (aggCurrent.getNumOfPorts() > aggBest.getNumOfPorts()){
                 			return aggCurrent;
				}
				if (aggCurrent.getNumOfPorts() < aggBest.getNumOfPorts()){
                 			return aggBest;
				}
			case BOND_STABLE:
			case BOND_BANDWIDTH:
				if (aggCurrent.getAggBandwidth() > aggBest.getAggBandwidth()){
					return aggCurrent;
				}
				break;
			default:
				log.info("Impossible agg select mode");
               break;
       	      }
	   return aggBest;
	}
	
	
	void setAggBond(LacpBond bond) {
		
		initAgg();
		setAggMacAddress(Arrays.copyOf(bond.getSysMacAddr(), LacpConst.ETH_ADDR_LEN));
		this.setBond(bond);
		this.isActive = 0;
		this.setNumOfPorts((short)0);	
		this.reselect = false;
	}
	
	
	static void copyAggfromOriginAgg(LacpAggregator dest,LacpAggregator origin) {
		
		dest.setIndiv(origin.isIndiv());
		dest.setActorAdminAggregatorKey(origin.getActorAdminAggregatorKey());
		dest.setActorOperAggKey(origin.getActorOperAggKey());
		dest.setPartnerSystem(origin.getPartnerSystem());
		dest.setPartnerSystemPriority(origin.getPartnerSystemPriority());
		dest.setPartnerOperAggKey(origin.getPartnerOperAggKey());
		dest.setReceiveState(origin.getReceiveState());
		dest.setTransmitState(origin.getTransmitState());
		dest.isActive = origin.isActive;
		dest.setNumOfPorts(origin.getNumOfPorts());	
		dest.getLagPortList().clear();
		dest.reselect = false;
		
		for (LacpPort port : origin.getLagPortList()) {
			dest.getLagPortList().add(port);
			port.portSetAggregator(dest);
			port.portSetActorPortAggregatorIdentifier(dest.getAggId());
		}
		Collections.sort(dest.getLagPortList());
	}

	@Override
	public int compareTo(LacpAggregator arg0) {
		if (arg0 == null){
		 return -1;
		}
		if (this.getAggLagId() == arg0.getAggLagId()){
		 return 0;
		}
		if (arg0.getAggLagId() == null){
		 return -1;
		}
		if (this.getAggLagId() == null){
		 return 1;
		}
		return this.getAggLagId().compareTo(arg0.getAggLagId());
	}

	
	/*
	public LacpPort findCandidateFromSelList(LacpPort port) {
		
		short port_number;
		int count = 0;
		LacpPort last = null;
		
		if (port == null) return null;
		port_number = port.getActorPortNumber();
		for (LacpPort entry : this.getLagPortList()) {
			if ((port_number & 0xf000) == (entry.getActorPortNumber() & 0xf000)) {
				count++;
				last = entry;
			}
		}
		if (count >= this.aggGetBond().bondGetMaxLink()) {
			if (port.portGetLagId().compareTo(last.portGetLagId()) < 0)
				return last;
		}
		return null;
	}
	*/

	public boolean IsPortReachMaxCount(LacpPort port) {

		short portNumber;
		int count = 0;
		
		if (port == null) return false;
		portNumber = port.getActorPortNumber();
		for (LacpPort entry : this.getLagPortList()) {
			if ((portNumber & 0xf000) == (entry.getActorPortNumber() & 0xf000)) {
				count++;
				if (count >= this.aggGetBond().bondGetMaxLink()) {
					log.debug("Exitng IsPortReachMaxCount - returned true");
					return true;
				}
			}
		}
		log.debug("IsPortReachMaxCount - returned false");
		return false;
	}
	
	
	public boolean canMoveToSelList(LacpPort port) {
		if (port == null)  return false;
		if ((port.getStateMachineBitSet() & LacpConst.PORT_STANDBY) == 0){
			log.debug("canMoveToSelList returned false, PORT_STANDBY is false");
			return false;
		}
		if (!this.getStandByPorts().contains(port)){
			log.debug("canMoveToSelList returned false, port not in standby list");
			return false;
		}
		if (port.portGetLagId().compareToPartial(this.getAggLagId()) !=0){
			log.debug("canMoveToSelList returned false, port lag id and agg lag id do not match");
			return false;
		}
		if (!IsPortReachMaxCount(port)){
			log.debug("canMoveToSelList returned true, aggregator has not reached max count");
			return true;
		}
		/*
		 * the below one is for when port priority is more important over port stability
		 * port stability means that we respects port's current membership into aggregator
		 * 
		if (this.findCandidateFromSelList(port)!=null)
			return true;
		if (port.portGetLagId().compareTo(this.lagPortList.get(numOfPorts-1).portGetLagId()) < 0)
			return true;
		*/
			
		return false;
		
	}
	
	public boolean existPortwithDist() {
		for (LacpPort port: this.getLagPortList()) {
			if (port.isPortAttDist()){
				log.debug("existPortwithDist returned true, one of the ports is in collecting/distributing");
				return true;
			}
		}
		return false;
	}


	public byte[] getAggMacAddress() {
		return aggMacAddress;
	}


	public void setAggMacAddress(byte[] aggMacAddress) {
		this.aggMacAddress = Arrays.copyOf(aggMacAddress, LacpConst.ETH_ADDR_LEN);
	}


	public void setAggId(short aggId) {
		this.aggId = aggId;
	}


	public boolean isIndiv() {
		return isIndiv;
	}


	public void setIndiv(boolean isIndiv) {
		this.isIndiv = isIndiv;
	}


	public short getActorAdminAggregatorKey() {
		return actorAdminAggKey;
	}


	public void setActorAdminAggregatorKey(short key) {
		this.actorAdminAggKey = key;
	}


	public short getActorOperAggKey() {
		return actorOperAggKey;
	}


	public void setActorOperAggKey(short key) {
		this.actorOperAggKey = key;
	}


	public byte[] getPartnerSystem() {
		return partnerSystem;
	}


	public void setPartnerSystem(byte[] sys) {
		this.partnerSystem = Arrays.copyOf(sys, LacpConst.ETH_ADDR_LEN);
	}


	public int getPartnerSystemPriority() {
		return partnerSystemPriority;
	}


	public void setPartnerSystemPriority(int pri) {
		this.partnerSystemPriority = pri;
	}


	public short getPartnerOperAggKey() {
		return partnerOperAggKey;
	}


	public void setPartnerOperAggKey(short key) {
		this.partnerOperAggKey = key;
	}


	public short getReceiveState() {
		return receiveState;
	}


	public void setReceiveState(short receive_state) {
		this.receiveState = receive_state;
	}


	public short getTransmitState() {
		return transmitState;
	}


	public void setTransmitState(short transmit_state) {
		this.transmitState = transmit_state;
	}


	public List<LacpPort> getLagPortList() {
		return lagPortList;
	}


	public void setLagPortList(List<LacpPort> lagPortList) {
		this.lagPortList = lagPortList;
	}


	public List<LacpPort> getStandByPorts() {
		return standByPorts;
	}


	public void setStandbyPorts(List<LacpPort> standByPorts) {
		this.standByPorts = standByPorts;
	}


	public LagId getAggLagId() {
		return aggLagId;
	}


	public void setAggLagId(LagId aggLagId) {
		this.aggLagId = aggLagId;
	}


	public void setNumOfPorts(short numOfPorts) {
		this.numOfPorts = numOfPorts;
	}


	public short getNumOfStandbyPort() {
		return numOfStandbyPort;
	}


	public void setNumOfStandbyPort(short numOfStandbyPort) {
		this.numOfStandbyPort = numOfStandbyPort;
	}


	public LacpBond getBond() {
		return bond;
	}


	public void setBond(LacpBond bond) {
		this.bond = bond;
	}
}
