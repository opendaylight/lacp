/*
 *  Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;


import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import org.opendaylight.lacp.state.*;
import org.opendaylight.lacp.timer.*;
import org.opendaylight.lacp.timer.TimerFactory.LacpWheelTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import java.util.concurrent.Future;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnectorBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.util.LacpPortType;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
 
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.AggRef;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.core.LagId;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LagIdElem;

public class LacpPort implements Comparable<LacpPort> {
		
	private short actorPortNumber;
	private int actorPortPriority;
	private short actorPortAggregatorIdentifier;
	private boolean ntt;
	private short actorAdminPortKey;
	private short actorOperPortKey;
	private PortParams partnerAdmin;
	private PortParams partnerOper;
	private byte actorAdminPortState;
	private byte actorOperPortState;
	private boolean isLacpEnabled;
  	private LacpGroupTbl groupTbl;	

	private static int id = 1;
	private short lacpPortId;
	private long swId;       
	private short portId;  
	//private short realPortId; 	
	private byte duplex;	
	private int speed;  
	private short instanceId;
	private byte link;
	
	boolean isInitialized;
	private int portPriority;	
	private LagId  lagId;
	private byte[] actorSystem;              
	private int actorSystemPriority;       
	private boolean isEnabled;
	protected Date activeSince;
    
	protected LacpBond bond;
	private LacpAggregator portAggregator;
	private LacpBpduInfo portTxLacpdu;
	private LacpPacketPdu portTxLacpPacketPdu;
    
	private short stateMachineBitSet;   
	
	private ReentrantLock portLock;	
	
        //Context objexts for respective state machines
        private RxContext rxContext;
        private MuxContext muxContext;
        private PeriodicTxContext periodicTxContext;
    
        //Rx States
        private RxCurrentState rxCurrentState;
        private RxDefaultedState rxDefaultedState; 
        private RxExpiredState rxExpiredState;
        private RxInitializeState rxInitializeState;
        private RxLacpDisabledState rxLacpDisabledState;
        public  RxPortDisabledState rxPortDisabledState;
    
        //Periodic States
        public  PeriodicTxFastState periodicTxFastState;
        private PeriodicTxNoPeriodicState periodicTxNoPeriodicState;
        private PeriodicTxPeriodicState periodicTxPeriodicState;
        private PeriodicTxSlowState periodicTxSlowState;;
    
        //Mux States
        private MuxDetachedState muxDetachedState;
        private MuxWaitingState muxWaitingState;
        private MuxAttachedState muxAttachedState;
        private MuxCollectingDistributingState muxCollectingDistributingState;
     
        //Timers
        private PortCurrentWhileTimerRegister currentWhileTimer;
        private PortPeriodicTimerRegister periodicTimer;
        private PortWaitWhileTimerRegister waitWhileTimer;
    
        //TimeoutHandle to cancel the respective timers
        private Timeout currWhileTimeout;
        private Timeout periodicTimeout;
        private Timeout waitWhileTimeout;
    
        private static final Logger log = LoggerFactory.getLogger(LacpPort.class);
	private LacpNodeConnectorBuilder lacpNCBuilder;
	private LacpAggregator lacpAggRef;
	private InstanceIdentifier ncId;
	private static DataBroker dataService;
 
 	public class PortParams {

		byte[] system;
		int systemPriority;
		short key;
		short portNumber;
		int portPriority;
		short portState;
		public PortParams() {
			system = new byte[LacpConst.ETH_ADDR_LEN];
		}

		public PortParams(byte[] system, int systemPriority, short key,
				short portNumber, int portPriority, short portState) {
			super();
			this.system = system;
			this.systemPriority = systemPriority;
			this.key = key;
			this.portNumber = portNumber;
			this.portPriority = portPriority;
			this.portState = portState;
		}

		@Override
		public String toString() {
			return "PortParams [system=" + HexEncode.bytesToHexString(system) + ", systemPriority=" + 
						String.format("%04x", this.systemPriority) + ", key=" + String.format("%04x", this.key) + 
						", portNumber=" + String.format("%04x", this.portNumber) + ", portPriority="
						 + String.format("%04x", this.portPriority) + ", portState=" + 
						getPortStateString((byte)this.portState) + "]";
		}
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + key;
			result = prime * result + portNumber;
			result = prime * result + portPriority;
			result = prime * result + Arrays.hashCode(system);
			result = prime * result + systemPriority;
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PortParams))
				return false;
			PortParams other = (PortParams) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (key != other.key)
				return false;
			if (portNumber != other.portNumber)
				return false;
			if (portPriority != other.portPriority)
				return false;
			if (!Arrays.equals(system, other.system))
				return false;
			if (systemPriority != other.systemPriority)
				return false;
			return true;
		}
		


		public void intializePortParams() {
			systemPriority = (int) 0x0000ffff;
			key = 1;
			portNumber =1;
			portPriority = (int)0x000000ff;
			portState = 1;
			Arrays.fill(system, (byte) 0);
		}
		
		public void setValue(PortParams params) {
			this.systemPriority = params.systemPriority;
			this.key = params.key;
			this.portNumber=params.portNumber;
			this.portPriority=params.portPriority;
			this.portState=params.portState;
			this.system = Arrays.copyOf(params.system, LacpConst.ETH_ADDR_LEN);
		}

		private LacpPort getOuterType() {
			return LacpPort.this;
		}
		
		public byte[] getSystem() {
			return system;
		}

		public int getSystemPriority() {
			return systemPriority;
		}

		public short getKey() {
			return key;
		}

		public short getPortNumber() {
			return portNumber;
		}

		public int getPortPriority() {
			return portPriority;
		}

		public short getPortState() {
			return portState;
		}

		public void setSystem(byte[] system) {
			this.system = system;
		}

		public void setSystemPriority(int systemPriority) {
			this.systemPriority = systemPriority;
		}

		public void setKey(short key) {
			this.key = key;
		}

		public void setPortNumber(short portNumber) {
			this.portNumber = portNumber;
		}

		public void setPortPriority(int portPriority) {
			this.portPriority = portPriority;
		}

		public void setPortState(short portState) {
			this.portState = portState;
		}
	} // end of PortParams class

 	//partner admin
	public byte[] portPartnerAdminGetSystem() {
		return this.partnerAdmin.system;
	}
	
	public void portPartnerAdminSetSystem(byte[] system) {
		this.partnerAdmin.system = Arrays.copyOf(system,LacpConst.ETH_ADDR_LEN);
	}
	
	public int portPartnerAdminGetSystemPriority() {
		return this.partnerAdmin.systemPriority;
	}
	public void portPartnerAdminsetSystemPriority(int val) {
		this.partnerAdmin.systemPriority = val;
	}
	
	public short portPartnerAdminGetKey() {
		return this.partnerAdmin.key;
	}
	public void portPartnerAdminSetKey(short val) {
		this.partnerAdmin.key = val;
	}
	public short portPartnerAdminGetPortNumber() {
	
		return this.partnerAdmin.portNumber;
	}
	public void portPartnerAdminSetPortNumber(short val) {
		this.partnerAdmin.portNumber = val;
	}
	public int portPartnerAdminGetPortPriority() {
		return this.partnerAdmin.portPriority;
	}
	public void portPartnerAdminSetPortPriority(int val) {
		this.partnerAdmin.portPriority = val;
	}
	
	public short portPartnerAdminGetPortState() {
		return this.partnerAdmin.portState;
	}
	public void portPartnerAdminSetPortState(short val) {
		this.partnerAdmin.portState = val;
	}	
 	//partner admin
	
	//partner oper
	public byte[] portPartnerOperGetSystem() {
		return this.partnerOper.system;
	}
	
	public void portPartnerOperSetSystem(byte[] system) {
		this.partnerOper.system = Arrays.copyOf(system,LacpConst.ETH_ADDR_LEN);
	}
	
	public int portPartnerOperGetSystemPriority() {
		return this.partnerOper.systemPriority;
	}
	public void portPartnerOperSetSystemPriority(int val) {
		this.partnerOper.systemPriority = val;
	}
	
	public short portPartnerOperGetKey() {
		return this.partnerOper.key;
	}
	
	public void portPartnerOperSetKey(short val) {
		this.partnerOper.key = val;
	}
	
	public short portPartnerOperGetPortNumber() {
			return this.partnerOper.portNumber;
	}
	public void portPartnerOperSetPortNumber(short val) {
		this.partnerOper.portNumber = val;
	}
	public int portPartnerOperGetPortPriority() {
		return this.partnerOper.portPriority;
	}
	public void portPartnerOperSetPortPriority(int val) {
		this.partnerOper.portPriority = val;
	}
	
	public short portPartnerOperGetPortState() {
		return this.partnerOper.portState;
	}
	public void portPartnerOperSetPortState(short val) {
		this.partnerOper.portState = val;
	}	
	//partner oper
	
	 
    public short getLacpPortId() {
		return lacpPortId;
	}

	public void setLacpPortId(short id) {
		this.lacpPortId = id;
	}
	
	public short getActorPortNumber() {
		return actorPortNumber;
	}
	
	public void setActorPortNumber(short portNumber) {
		this.actorPortNumber = portNumber;
	}
	
	public int getActorPortPriority() {
		return actorPortPriority;
	}
	
	public void setActorPortPriority(int portPriority) {
		actorPortPriority = portPriority;
	}
	
	public short getactorPortAggregatorIdentifier(){
		return actorPortAggregatorIdentifier;
	}
	
	public void setactorPortAggregatorIdentifier(short aggIdentifier){
		actorPortAggregatorIdentifier = aggIdentifier;
	}
	
	public short getActorAdminPortKey() {
		return actorAdminPortKey;
	}

	public void setActorAdminPortKey(short actorAdminPortKey) {
		this.actorAdminPortKey = actorAdminPortKey;
	}
	
	public short getActorOperPortKey() {
		return actorOperPortKey;
	}

	public void setActorOperPortKey(short actorOperPortKey) {
		this.actorOperPortKey = actorOperPortKey;
	}
	
	public byte getActorAdminPortState() {
		return actorAdminPortState;
	}

	public void setActorAdminPortState(byte actorAdminPortState) {
		this.actorAdminPortState = actorAdminPortState;
	}

	public void setActorOperPortState(byte actorOperPortState) {
		this.actorOperPortState = actorOperPortState;
	}

	int checkAggSelectionTimer() {
		if (bond != null){
			return ((LacpBond)bond).checkAggSelectTimer();
		}
		return 0;
	}	
	
	public boolean getEnabled(boolean isEnabled) {
		return isEnabled;
	}
	
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isLacpEnabled(){
		return isLacpEnabled;
	}

	void setIsLacpEnabled(boolean enabled) {
		log.info("Entering setIsLacpEnabled method");
		if (this.isLacpEnabled()!= enabled) {
			
			if (enabled) {
				this.setLacpEnabled(true);
	        		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_LACP_ENABLED));
				log.info("Setting port={} to PORT_LACP_ENABLED to true", portId);
				
			} else {
				this.setLacpEnabled(false);
				setStateMachineBitSet((short)(getStateMachineBitSet() & ~LacpConst.PORT_LACP_ENABLED));
				log.info("Setting port={} to PORT_LACP_ENABLED to false", portId);
			}
	        	setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));				
			log.info("Setting port={} to PORT_BEGIN", portId);
		}	
		log.info("Exiting setIsLacpEnabled method");
	}
	
	public LagId portGetLagId() {
    		return lagId;
    	}
	
	public LacpAggregator getPortAggregator() {
		return portAggregator;
	}
	
	public void portSetAggregator(LacpAggregator agg) {
		this.setPortAggregator(agg);
		
	}
	
	public void setPortAggregator(LacpAggregator portAggregator) {
		this.portAggregator = portAggregator;
	}
	
	private short getPortAggregatorId() {
		log.info("Entering getPortAggregatorId");
		if (this.portAggregator != null){
			log.info("getPortAggregatorId returning agg id={}",portAggregator.getAggId());
			return this.portAggregator.getAggId();
		}
		else{
			log.info("getPortAggregatorId returning agg id as 0");
			return 0;
		}
	}

	public short getActorPortAggregatorIdentifier() {
		return actorPortAggregatorIdentifier;
	}

	public void portSetActorPortAggregatorIdentifier(short val) {
		this.setActorPortAggregatorIdentifier(val);
	}
	
	public void setActorPortAggregatorIdentifier(short actorPortAggregatorIdentifier) {
		this.actorPortAggregatorIdentifier = actorPortAggregatorIdentifier;
	}

	public byte slaveGetduplex() {
		return getDuplex();
	}
	public void slaveSetDuplex(byte duplex) {
		setDuplex(duplex);
	}
	
	public byte getDuplex() {
		return duplex;
	}

	public void setDuplex(byte duplex) {
		this.duplex = duplex;
	}
	
	public byte getActorOperPortState() {
		return actorOperPortState;
	}


	public void setLacpEnabled(boolean isLacpEnabled) {
		this.isLacpEnabled = isLacpEnabled;
	}

	public short getStateMachineBitSet() {
		return stateMachineBitSet;
	}

	public void setStateMachineBitSet(short stateMachineBitSet) {
		this.stateMachineBitSet = stateMachineBitSet;
	}
	
	public byte[] getActorSystem() {
		return actorSystem;
	}

	public void setActorSystem(byte[] actorSystem) {
		this.actorSystem = actorSystem;
	}

	public int getActorSystemPriority() {
		return actorSystemPriority;
	}

	public void setActorSystemPriority(int actorSystemPriority) {
		this.actorSystemPriority = actorSystemPriority;
	}

	public boolean isNtt() {
		return ntt;
	}

	public void setNtt(boolean ntt) {
		this.ntt = ntt;
		
	}

	public short getInstanceId() {
		return this.instanceId;
	}

	
	public int getPortPriority() {
		return portPriority;
	}

	public void setPortPriority(int portPriority) {
		this.portPriority = portPriority;
	}

	public int getSpeed() {
		return speed;
	}
	
	
	public void slaveSetSpeed(int speed) {
		setSpeed(speed);
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setInstanceId(short instanceId) {
		this.instanceId = instanceId;
	}

	public byte getLink() {
		return link;
	}

	
	public LacpBond slaveGetBond() {
		return this.bond;
	}
	

	public void slaveSetBond(LacpBond bond) {
		this.bond = bond;
	}

	public long slaveGetSwId() {
		return swId;
	}

	public long getLacpSwId(){
		return swId;
	}
	
	public short slaveGetPortId() {
		return portId;
	}

	public void setLink(byte link) {
		this.link = link;
	}

	public ReentrantLock getPortLock() {
		return portLock;
	}

	public void setPortLock(ReentrantLock portLock) {
		this.portLock = portLock;
	}

	private LacpPort(long swId, short portId, LacpBond bond, int port_priority, LacpBpduInfo bpduInfo) {

		log.info("Entering LacpPort constructor for switchid={} port={}",portId, swId);

		this.lacpPortId = (short)(id);
		if (++id > 0xffff){
			id = 1;
		}

		this.swId = swId;
		this.bond = bond;
		this.portId = portId;
		this.setInstanceId((short)(bond.getSlaveCnt()+1));
		this.setLink(LacpConst.BOND_LINK_DOWN);
		this.setDuplex((byte)0);
		this.activeSince = null;

		bond.setSlaveCnt(getInstanceId());
		setPortLock(new ReentrantLock());
		
		this.portPriority = port_priority;
		this.setActorSystem(new byte[LacpConst.ETH_ADDR_LEN]);
		this.partnerAdmin = new PortParams();
		this.partnerOper = new PortParams();		
		this.setNtt(false);
		portTxLacpdu = bpduInfo;		

		rxContext = new RxContext();
		muxContext = new MuxContext();
		periodicTxContext = new PeriodicTxContext(); 
		
		currentWhileTimer = new PortCurrentWhileTimerRegister(portId,swId);
		waitWhileTimer = new PortWaitWhileTimerRegister(portId,swId);
		periodicTimer = new PortPeriodicTimerRegister(portId,swId);
			
		
	        rxCurrentState = new RxCurrentState();
	        rxDefaultedState = new RxDefaultedState();
	        rxExpiredState = new RxExpiredState();
	        rxInitializeState = new RxInitializeState();
	        rxLacpDisabledState = new RxLacpDisabledState();
	        rxPortDisabledState = new RxPortDisabledState();
	    
	        periodicTxFastState = new PeriodicTxFastState();
	        periodicTxNoPeriodicState = new PeriodicTxNoPeriodicState();
	        periodicTxPeriodicState = new PeriodicTxPeriodicState();
	        periodicTxSlowState = new PeriodicTxSlowState();
	    
	        muxDetachedState = new MuxDetachedState();
	        muxWaitingState = new MuxWaitingState();
	        muxAttachedState = new MuxAttachedState();
	        muxCollectingDistributingState =  new MuxCollectingDistributingState();
	     
	       /*
	       setCurrentWhileTimer(LacpConst.LONG_TIMEOUT_TIME , TimeUnit.SECONDS);
	       setWaitWhileTimer(LacpConst.AGGREGATE_WAIT_TIME , TimeUnit.SECONDS);
	       setPeriodicWhileTimer(LacpConst.SLOW_PERIODIC_TIME , TimeUnit.SECONDS);
	       */
		
	       portAssignSlave(bond.getSysMacAddr(), bond.getLacpFast(), bond.bondGetSysPriority(), this.portPriority, bond.getAdminKey());			
               ncId = bpduInfo.getNCRef().getValue();
               lacpNCBuilder = new LacpNodeConnectorBuilder();
               lacpNCBuilder.setActorPortNumber(this.actorPortNumber);
               lacpNCBuilder.setPeriodicTime(LacpUtil.DEF_PERIODIC_TIME);
               lacpNCBuilder.setLacpAggRef(new AggRef(bond.getLacpAggInstId()));
               updateNCLacpInfo();
               LacpSystem lacpSystem = LacpSystem.getLacpSystem();
               LacpNodeExtn lacpNode = lacpSystem.getLacpNode(swId);
               if (lacpNode == null)
               {
                    log.warn("LacpNode {} associated with this port {} is null", swId, portId);
               }
               else
               {
                    lacpNode.removeNonLacpPort(ncId);
                    lacpNode.addLacpPort(ncId, this);
               }
	       log.info("Exiting LacpPort constructor for switchid={} port={}",portId, swId);
	}
	
	
	public Timeout setCurrentWhileTimer(long delay){
	       log.info("Entering setCurrentWhileTimer for switchid={} port={}",swId,portId);

		if((currWhileTimeout!= null) && (!currWhileTimeout.isExpired())){
                           currWhileTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		currWhileTimeout=instance.registerPortForCurrentWhileTimer(currentWhileTimer,delay, TimeUnit.SECONDS);
	        log.info("Exiting setCurrentWhileTimer for switchid={} port={}",swId,portId);
		return currWhileTimeout;
	}
	
	public Timeout setWaitWhileTimer(long delay){
	       log.info("Entering setWaitWhileTimer for switchid={} port={}",swId,portId);

		if((waitWhileTimeout!= null) && (!waitWhileTimeout.isExpired())){
                           waitWhileTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.WAIT_WHILE_TIMER);
		waitWhileTimeout=instance.registerPortForWaitWhileTimer(waitWhileTimer,delay, TimeUnit.SECONDS);
	        log.info("Exiting setWaitWhileTimer for switchid={} port={}",swId,portId);
		return waitWhileTimeout;
	}
	
	public Timeout setPeriodicWhileTimer(long delay){

	        log.info("Entering setPeriodicWhileTimer for switchid={} port={}",swId,portId);
		if((periodicTimeout != null) && (!periodicTimeout.isExpired())){
                           periodicTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.PERIODIC_TIMER);
	        System.out.println("Before setPeriodicWhileTimer for switchid= " + swId +  " and portId= " + portId);
		periodicTimeout=instance.registerPortForPeriodicTimer(periodicTimer,delay, TimeUnit.SECONDS);
	        System.out.println("After setPeriodicWhileTimer for switchid= " + swId +  " and portId= " + portId);
	        log.info("Exiting setPeriodicWhileTimer for switchid={} port={}",swId,portId);
		return periodicTimeout;
	}
	
	public Timeout getPeriodicWhileTimer(){
		return periodicTimeout;
	}

	
	@Override
	public int compareTo(LacpPort arg0) {
		if (this.lagId == arg0.lagId)
			return 0;
		if (arg0.lagId == null) return -1;
		if (this.lagId == null) return 1;
		return this.lagId.compareTo(arg0.lagId);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getInstanceId();
		result = prime * result + getActorAdminPortKey();
		result = prime * result + portId;
		result = prime * result + (int) (swId ^ (swId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LacpPort))
			return false;
		LacpPort other = (LacpPort) obj;
		if (getInstanceId() != other.getInstanceId())
			return false;
		if (getActorAdminPortKey() != other.getActorAdminPortKey())
			return false;
		if (portId != other.portId)
			return false;
		if (swId != other.swId)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "LacpPort [swId=" + swId + ", portId=" + portId
				+ ", duplex=" + getDuplex()
				+ ", speed=" + getSpeed() + ", instanceId=" + getInstanceId() + ", link=" + getLink()
				+ ", portLock=" + getPortLock() + ", bond=" + bond
				+ ", portPriority=" + portPriority + ", actorPortNumber="
				+ getActorPortNumber() + ", actorPortPriority=" + getActorPortPriority()
				+ ", actorSystem=" + HexEncode.bytesToHexString(getActorSystem())
				+ ", actorSystemPriority=" + getActorSystemPriority()
				+ ", actorPortAggregatorIdentifier="
				+ getActorPortAggregatorIdentifier() + ", ntt=" + isNtt()
				+ ", actorAdminPortKey=" + getActorAdminPortKey()
				+ ", actorOperPortKey=" + actorOperPortKey
				+ ", actorAdminPortState=" + getActorAdminPortState()
				+ ", actorOperPortState=" + getActorOperPortState() + ", isEnabled="
				+ isEnabled + ", isLacpEnabled=" + isLacpEnabled() + ", stateMachineBitSet="
				+ getStateMachineBitSet() + ", partnerAdmin=" + partnerAdmin + ", partnerOper="
				+ partnerOper + ", lagId=" + lagId
				+ "]";
	}
	
	public String getPortStateString(byte state) {
	log.info("Entering getPortStateString for port={}",portId);
    	byte val = state;
    	String result="";
    	String token;
    	short seq = 0;
    	while (seq < 8) {
    		token = "";
    		if ((val & 0x01)!=0) {
    			switch (seq) {
    			case 0: 
    				token = "ACT";
    				break;
    			case 1:
    				token = "TMO";
    				break;
    			case 2:
    				token = "AGG";
    				break;
    			case 3:
    				token = "SYN";
    				break;
    			case 4:
    				token = "COL";
    				break;
    			case 5:
    				token = "DIS";
    				break;
    			case 6: 
    				token = "DEF";
    				break;
    			case 7: 
    				token = "EXP";
    				break;
    			}
    			result = result+" "+token; 
    		}
    		seq++;
    		val = (byte) ((val >> 1) & 0x7f);
    	}
	log.info("Exiting getPortStateString for port={} and port state is={}",portId,result);
    	return result;
    }
	
    public String getStmStateString(short state) {
	log.info("Entering getStmStateString for port={} and state machine state is={}",portId,state);
    	short val = state;
    	String result="";
    	String token;
    	short seq = 0;
    	while (seq < 10) {
    		token = "";
    		if ((val & 0x01)!=0) {
    			switch (seq) {
    			case 0: 
    				token = "BEG";
    				break;
    			case 1:
    				token = "ENA";
    				break;
    			case 2:
    				token = "ACTC";
    				break;
    			case 3:
    				token = "PARC";
    				break;
    			case 4:
    				token = "RDY";
    				break;
    			case 5:
    				token = "RDYN";
    				break;
    			case 6: 
    				token = "MAT";
    				break;
    			case 7: 
    				token = "STA";
    				break;
    			case 8: 
    				token = "SEL";
    				break;
    			case 9: 
    				token = "MOV";
    				break;
    				
    			}
    			result = result+" "+token; 
    		}
    		seq++;
    		val = (short) ((val >> 1) & 0x7ff);
    	}
	log.info("Exiting getStmStateString for port={} and state machine state is={}",portId,result);
    	return result;
    }
    
    
    /*
    public String getStmStateFullString(short state) {
    	short val = state;
    	String result="";
    	String token;
    	short seq = 0;
    	while (seq < 10) {
    		token = "";
    		if ((val & 0x01)!=0) {
    			switch (seq) {
    			case 0: 
    				token = "PORT_BEGIN";
    				break;
    			case 1:
    				token = "PORT_LACP_ENABLED";
    				break;
    			case 2:
    				token = "PORT_ACTOR_CHURN";
    				break;
    			case 3:
    				token = "PORT_PARTNER_CHURN";
    				break;
    			case 4:
    				token = "PORT_READY";
    				break;
    			case 5:
    				token = "PORT_READY_N";
    				break;
    			case 6: 
    				token = "PORT_MATCHED";
    				break;
    			case 7: 
    				token = "PORT_STANDBY";
    				break;
    			case 8: 
    				token = "PORT_SELECTED";
    				break;
    			case 9: 
    				token = "PORT_MOVED";
    				break;
    				
    			}
    			result = result+" "+token; 
    		}
    		seq++;
    		val = (short) ((val >> 1) & 0x7ff);
    	}
    	return result;
    }
    */

	 public String getSpeedString(byte speed) {
		 log.info("Entering getSpeedString for port={} and speed is={}",portId,speed);
		 String result="";
		 byte val = speed;
		 switch (val) {
		 case LacpConst.LINK_SPEED_BITMASK_10MBPS: 
			 result = "10M";
			 break;
		 case LacpConst.LINK_SPEED_BITMASK_100MBPS: 
			 result = "100M";
			 break;
		 case LacpConst.LINK_SPEED_BITMASK_1000MBPS: 
			 result = "1G";
			 break;
		 case LacpConst.LINK_SPEED_BITMASK_10000MBPS: 
			 result = "10G";
			 break;
		 case LacpConst.LINK_SPEED_BITMASK_40000MBPS: 
			 result = "40G";
			 break;
		 case LacpConst.LINK_SPEED_BITMASK_100000MBPS: 
			 result = "100G";
			 break;
		 }
		 log.info("Exiting getSpeedString for port={} and speed is={}",portId,result);
		 return result;
	 }

		
	public PortParams getPartnerAdmin() {
		return partnerAdmin;
	}
	
	public PortParams getPartnerOper() {
		return partnerOper;
	}
	
	public void setPartnerAdmin(byte[] system, short key, int systemPriority, short portNum, int portPriority, short portState) {
		this.partnerAdmin = new PortParams( system,  systemPriority,  key, portNum,  portPriority,  portState);
	}

	public void setPartnerOper(byte[] system, short key, int systemPriority, short portNum, int portPriority, short portState)
        {
	    this.partnerOper = new PortParams( system,  systemPriority,  key, portNum,  portPriority,  portState);
            lacpNCBuilder.setPartnerPortNumber(portNum);
            /* Partner port infor is not written to md-sal now.
               It will be updated when the logicalNCRef for the port is assigned. */
	}

	public static  LacpPort newInstance(long swId, short portId, LacpBond bond, int port_priority,LacpBpduInfo bpduInfo) {
		log.info("Entering/Exiting LacpPort newInstance() method for sw={} port={} priority={}",swId,portId,port_priority);
		return new LacpPort(swId, portId, bond, port_priority,bpduInfo);
	}
	
	public void attachBondToAgg() {
		
	}
	
	public void detachBondFromAgg() {
		
	}
	
	public void lacpInitPort(int lacp_fast)
	{
		log.info("Entering lacpInitPort for port={}",portId);
		this.setActorPortNumber(this.portId);
		this.setActorPortPriority((int)0x000000ff);
		Arrays.fill(getActorSystem(), (byte)0);
		this.setActorSystemPriority((int) 0x0000ffff);
		this.setActorPortAggregatorIdentifier((short)0);
		this.setNtt(false);
		this.setActorAdminPortKey((short)1);
		this.actorOperPortKey = 1;
		this.setLacpEnabled(false);

		
		this.setActorAdminPortState((byte)(LacpConst.PORT_STATE_AGGREGATION | LacpConst.PORT_STATE_LACP_ACTIVITY ));
		this.setActorOperPortState(this.getActorAdminPortState());
		this.partnerAdmin.intializePortParams();
		this.partnerOper.intializePortParams();
		if (this.portTxLacpdu== null) {
			this.portTxLacpdu = new LacpBpduInfo();
		}
		this.isEnabled = true;

		this.setStateMachineBitSet(LacpConst.PORT_BEGIN);
		if (this.isLacpEnabled()){
			this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_LACP_ENABLED));
		}
		if (lacp_fast > 0){
			this.setActorOperPortState((byte)(this.getActorOperPortState()
					| LacpConst.PORT_STATE_LACP_TIMEOUT));
		}
		log.info("Exiting lacpInitPort for port={}",portId);
	}

	public LacpBpduInfo getLacpBpduInfo(){
		return portTxLacpdu;
	}
	
	void setPortsReady(int val) 
	{
		log.info("Entering setPortsReady for port={}",portId);
		if (val>0) {
			this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_READY));
			log.info("setting PORT_READY for port={}",portId);
		}
		else{
			this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~(LacpConst.PORT_READY)));
			log.info("setting PORT_READY to false for port={}",portId);
		}
		log.info("Exiting setPortsReady for port={}",portId);
	}

	int getPortsReady() 
	{
		if ((this.getStateMachineBitSet() & LacpConst.PORT_READY_N) == 0){
			return(0);
		}
		return (1);
	}

	public short getLinkSpeed() {
		if (getLink() != LacpConst.BOND_LINK_UP){
			return(0);
		}
		return((short)this.getSpeed());
	}
	
	public byte get_Duplex() {
		log.info("Entering get_Duplex for port={}",portId);
		if (getLink() != LacpConst.BOND_LINK_UP){
			log.info("Exiting get_Duplex for port={} link is BOND_LINK_DOWN",portId);
			return(0);
		}
		else{
			log.info("Exiting get_Duplex for port={} link is BOND_LINK_UP",portId);
			return(this.getDuplex());
		}
	}

	void setPortAdminPortKey(short val) {
		this.setActorAdminPortKey(val);
	}
	
	public static short toUnsigned(byte b) {
	    return (short) (b >= 0 ? b : 256 + b);
	}
	
	LacpPacketPdu updateLacpFromPortToLacpPacketPdu(){

		this.slavePSMLock();

		//define below in LacpConst.java 
		int reserved = 0;
		int lenType = 34825;
		short actorInfoLen = 20;
		short partnerInfoLen = 20;
		short collectorInfoLen = 16;
		short terminatorInfoLen = 0;

		LacpPacketPduBuilder obj =null;

                try {
			log.info("Entering updateLacpFromPortToLacpPacketPdu for port={}",portId);

			obj = new LacpPacketPduBuilder();		

			/*
			obj.setSwId(this.swId);
			obj.setPortId(this.portId);
			*/

			obj.setIngressPort(portTxLacpdu.getNCRef());
			MacAddress hwMac = null;
			hwMac = getSwitchHardwareAddress();
			if(hwMac != null){
				System.out.println("The hw mac is : " + hwMac.toString());
			}else{
				System.out.println("ERROR - THE HW MAC IS NULL");
			}
			obj.setSrcAddress(getSwitchHardwareAddress());
			System.out.println("The source address is : "  + obj.getSrcAddress());
			//obj.setDestAddress(portTxLacpdu.getSrcAddr());
			obj.setDestAddress( new MacAddress (LacpConst.LACP_DEST_MAC_STRING));
			System.out.println("The destination address is : " +  obj.getDestAddress());

			obj.setLenType(new Integer(lenType));//use LacpConst.LACP_ETH_TYPE later
			obj.setSubtype(SubTypeOption.SlowProtocol);
			obj.setVersion(VersionValue.LacpVersion);

			ActorInfoBuilder actorInfoBuilder = new ActorInfoBuilder();

			actorInfoBuilder.setSystemPriority(new Integer(this.getActorSystemPriority()));
			//actorInfoBuilder.setSystemId(new String(LacpUtil.byteToString(this.getActorSystem())));
			log.info("actor system id before bytesToHex conversion is :", (this.getActorSystem()));
			System.out.println("actor system id before bytesToHex conversion is :" + this.getActorSystem());
			actorInfoBuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(this.getActorSystem())));
			System.out.println("actor system id is :" + HexEncode.bytesToHexStringFormat(this.getActorSystem()));
			log.info("actor system id after bytesToHex conversion is :", HexEncode.bytesToHexString(this.getActorSystem()));
			actorInfoBuilder.setKey(new Integer(this.actorOperPortKey));
			actorInfoBuilder.setPortPriority(new Integer(this.getActorPortPriority()));
			actorInfoBuilder.setPort(new Integer(this.getActorPortNumber()));
			/*
 			//commented by rajesh on 14 apr
                        short pState = this.getActorOperPortState();
			actorInfoBuilder.setState(Short.valueOf(pState));
 			//commented by rajesh on 14 apr
			*/

			short pState = toUnsigned(this.getActorOperPortState());
			actorInfoBuilder.setState(Short.valueOf(pState));

			actorInfoBuilder.setTlvType(TlvTypeOption.ActorInfo);
			actorInfoBuilder.setInfoLen(new Short(actorInfoLen));
			actorInfoBuilder.setReserved(new Integer(reserved));
			actorInfoBuilder.setReserved1(new Short((short)reserved));
		

			final PortParams partner = this.partnerOper;
			PartnerInfoBuilder partnerInfoBuilder = new PartnerInfoBuilder();

			partnerInfoBuilder.setSystemPriority(new Integer(partner.systemPriority));
			//TODO-RAJESH
			//partnerInfoBuilder.setSystemPriority(new Integer(65535));
			log.info("partner system id before bytesToHex conversion is :", (partner.system));
			partnerInfoBuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(partner.system)));
			System.out.println("partner system id is :" + HexEncode.bytesToHexStringFormat(partner.system));
			log.info("partner system id after bytesToHex conversion is :", HexEncode.bytesToHexStringFormat(partner.system));
			partnerInfoBuilder.setKey(new Integer(partner.key));
			partnerInfoBuilder.setPortPriority(new Integer(partner.portPriority));
			//TODO-RAJESH
			//partnerInfoBuilder.setPortPriority(new Integer(255));
			partnerInfoBuilder.setPort(new Integer(partner.portNumber));

			partnerInfoBuilder.setState(Short.valueOf(partner.portState));

			partnerInfoBuilder.setTlvType(TlvTypeOption.PartnerInfo);
			partnerInfoBuilder.setInfoLen(new Short(partnerInfoLen));
			partnerInfoBuilder.setReserved(new Integer(reserved));
			partnerInfoBuilder.setReserved1(new Short((short)reserved));
		
			obj.setActorInfo(actorInfoBuilder.build());
			obj.setPartnerInfo(partnerInfoBuilder.build());

			obj.setCollectorMaxDelay(new Integer(0));
			obj.setCollectorTlvType(TlvTypeOption.CollectorInfo);
			obj.setCollectorInfoLen(new Short(collectorInfoLen));
			obj.setCollectorReserved(new BigInteger("0"));
			obj.setCollectorReserved1(new Long(reserved));

			obj.setTerminatorTlvType(TlvTypeOption.Terminator);
			obj.setTerminatorInfoLen(new Short(terminatorInfoLen));
			obj.setTerminatorReserved(new String("0"));
			obj.setFCS(0L);

			System.out.println("The PDU object to be enqued onto Tx queue ActorInfo: "+ obj.getActorInfo());
			System.out.println("The PDU object to be enqued onto Tx queue PartnerInfo: "+ obj.getPartnerInfo());
			log.info("Exiting updateLacpFromPortToLacpPacketPdu for port={}",portId);
		}finally{
			this.slavePSMUnlock();
		}
		return obj.build();
	}

	int lacpduSend(LacpPacketPdu obj, LacpTxQueue.QueueType qType)
	{
		log.info("Entering lacpduSend for port={}", portId);
		if (isInitialized) {
			slaveSendBpdu(obj,qType);
		}
		log.info("Exiting lacpduSend for port={}", portId);
		return 0;
	}
	
	boolean isPortAttDist() {
		log.info("Entering isPortAttDist for port={}",portId);
		if (((this.getActorOperPortState() & LacpConst.PORT_STATE_COLLECTING) > 0) || ((this.getActorOperPortState() & LacpConst.PORT_STATE_DISTRIBUTING) > 0)){
			log.info("isPortAttDist is returning true");
			return true;
		}
		log.info("isPortAttDist is returning false");
		log.info("Exiting isPortAttDist for port={}",portId);
		return false;
	}
	
	boolean isPortSelected() {
		log.info("Entering isPortSelected for port={}",portId);
		if ((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0){
			log.info("isPortSelected is returning PORT_SELECTED=true");
			return true;
		}
		log.info("isPortSelected is returning PORT_SELECTED=false");
		return false;			
	}
	
	
	public void slavePSMLock() {
		getPortLock().lock();
	}
	
	public void slavePSMUnlock() {
		getPortLock().unlock();
	}
		
	public LacpAggregator slaveGetFreeAgg() {
		return (((LacpBond)bond).bondGetFreeAgg());
	}

	public void slaveSetLacpPortEnabled(boolean enabled) {
		this.slavePSMLock();
		try {
			setIsLacpEnabled(enabled);
		} finally {
			this.slavePSMUnlock();
		}
	}
	
	public void portSetActorOperPortState(byte val) {
		this.setActorOperPortState(val);
	}
	
	public byte portGetActorOperPortState() {
		return this.getActorOperPortState();
	}
	
	public short portGetActorPortAggregatorIdentifier() {
		return getActorPortAggregatorIdentifier();
	}
	
	public int portGetActorPortPriority() {
		return getActorPortPriority();
	}

	public void portSetActorPortPriority(int val) {
		this.setActorPortPriority(val);
	}
	
	public int portGetActorSystemPriority() {
		return getActorSystemPriority();
	}

	public void portSetActorSystemPriority(int val) {
		this.setActorSystemPriority(val);
	}
	
	void lacpDisablePort() 
	{	 
		log.info("Entering lacpDisablePort for port={}",portId);
		 boolean select_new_active_agg = false;

		 if (!isInitialized) return;

		 LacpAggregator aggregator = this.getPortAggregator();
		 
		 portSetActorOperPortState((byte)(this.portGetActorOperPortState() & (~LacpConst.PORT_STATE_AGGREGATION)));

		/*
		 LacpPacketPdu obj = updateLacpFromPortToLacpPacketPdu();
		 lacpduSend(obj,LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
		*/
		 
		 if (aggregator != null && aggregator.aggHasPort(this)) {
			 if (aggregator.getNumOfPorts() == 1){
				 select_new_active_agg = aggregator.getIsActive() > 0 ? true :  false;
			 }
			 aggregator.rmPortFromAgg(this);
			 if (select_new_active_agg){ 
				 bond.bondAggSelectionLogic();
			 }
			 
		 } else {
			 log.error("lacpDisbalePort: bad aggregator = {}", aggregator);
		 }
		isInitialized = false;
		log.info("Exiting lacpDisablePort for port={}",portId);
	}
			 
	void slavePortPriorityChange(int priority) {

		log.info("Entering slavePortPriorityChange for port={}",portId);
		slavePSMLock();
		 try {
		 	if (portGetActorPortPriority() == priority){
				return;
		 	}
		 	portPortPriorityChange(priority);
		 } finally {
		 	slavePSMUnlock();
		 }
		log.info("Exiting slavePortPriorityChange for port={}",portId);
	 }
	 
	 void slaveSystemPriorityChange(int priority) {
		log.info("Entering slaveSystemPriorityChange for port={}",portId);
		 slavePSMLock();
		 try {
		 	if (portGetActorSystemPriority() == priority){
				return;
		 	}
		 	portSystemPrioriyChange(priority);
		 } finally {
		 	slavePSMUnlock();
		 }
		log.info("Exiting slaveSystemPriorityChange for port={}",portId);
	 }

	public void slaveHandleLinkChange(byte link)
	{
		log.info("Entering slaveHandleLinkChange for port={}",portId);
		if (this.getLink()!= link) {
			this.setLink(link);
			portHandleLinkChange(link);
		}
		log.info("Exiting slaveHandleLinkChange for port={}",portId);
	}
	

	public int slaveRxLacpBpduReceived(LacpBpduInfo lacpdu)
	{
		log.info("Entering slaveRxLacpBpduReceived for port={}",portId);
		System.out.println("Entering slaveRxLacpBpduReceived for port={}" + portId);
		int ret = LacpConst.RX_HANDLER_DROPPED;

		if (this.getLink() != LacpConst.BOND_LINK_UP){
			  log.warn("in method slaveRxLacpBpduReceived() - lacp pdu not processed as link is down for switch {} port {}", lacpdu.getSwId(), lacpdu.getPortId());
			 return ret;
		}
	
		if (!this.isInitialized) {
			  log.warn("in method slaveRxLacpBpduReceived() - lacp pdu not processed as port is not initialized for switch {} port {}", lacpdu.getSwId(), lacpdu.getPortId());
			return ret;
		}
		ret = LacpConst.RX_HANDLER_CONSUMED;

		TimerExpiryMessage obj = null;
		log.info("Calling runProtocolStateMachine for port={}",portId);
		System.out.println("Calling runProtocolStateMachine for port={}" + portId);
		runProtocolStateMachine(lacpdu,obj);

		log.info("Exiting slaveRxLacpBpduReceived for port={}",portId);
		return ret;
	}	
	
	public void slaveUpdateLacpRate(int lacp_fast) {
		
		log.info("Entering slaveUpdateLacpRate for port={}",portId);
		if ( !isInitialized){
			return;
		}

		slavePSMLock();
		try {

			if (lacp_fast>0){
				portSetActorOperPortState((byte)(portGetActorOperPortState() | LacpConst.PORT_STATE_LACP_TIMEOUT));
				log.info("setting actor oper state to PORT_STATE_LACP_TIMEOUT");
			}	
        		else{
				portSetActorOperPortState((byte)(portGetActorOperPortState() & (~LacpConst.PORT_STATE_LACP_TIMEOUT)));
				log.info("setting actor oper state to ~PORT_STATE_LACP_TIMEOUT");
			}
		} finally {
			slavePSMUnlock();
		}
		log.info("Exiting slaveUpdateLacpRate for port={}",portId);
        }
	
	public void portPortPriorityChange(int priority) {
		log.info("Entering portPortPriorityChange for port={}",portId);
		if (!isInitialized){
			return;
		}
		this.setActorPortPriority(priority);
		portSetLagId();			 
		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));				
		log.info("setting state machine vars to PORT_BEGIN for port={}",portId);
		log.info("Exiting portPortPriorityChange for port={}",portId);
	}		
	 
	 
	void portSystemPrioriyChange(int priority)
	{

		log.info("Entering portSystemPrioriyChange for port={}",portId);
		if (!isInitialized){
			return;
		}
		portSetActorSystemPriority(priority);
		portSetLagId();			 
		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));
		log.info("setting state machine vars to PORT_BEGIN for port={}",portId);
		log.info("Exiting portSystemPrioriyChange for port={}",portId);
	}
	
	public int slaveSendBpdu(LacpPacketPdu bpdu, LacpTxQueue.QueueType qType) {
		log.info("Entering slaveSendBpdu for port={}",portId);
		if (bpdu == null) return -1;
		if (this.getLink() != LacpConst.BOND_LINK_UP){
			log.info("slaveSendBpdu did not put the LacpPacketPdu onto queue as port={} link is down ",portId);
			System.out.println("slaveSendBpdu did not put the LacpPacketPdu onto queue as port={} link is down " + portId);
			return -1;
		}
		System.out.println("Bpdu is :" + bpdu);
		LacpTxQueue lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();
		lacpTxQueue.enqueue(qType,bpdu);

		//Commented by Rajesh on 4/7/15
		/*
		LacpPacketPdu tempPdu = lacpTxQueue.dequeue(qType);
		System.out.println("Bpdu is : and qType is " + tempPdu + " " + qType);
		System.out.println("QueueInstance is : " + lacpTxQueue);
		lacpTxQueue.enqueue(qType,bpdu);
		*/


		log.info("slaveSendBpdu sucessfully put the LacpPacketPdu onto queue for port={}",portId);
		log.info("Exiting slaveSendBpdu for port={}",portId);
		return 0;
	}
	
	public void runProtocolStateMachine(LacpBpduInfo lacpBpdu, TimerExpiryMessage tmExpMsg) {
		
		log.info("Entering runProtocolStateMachine for port={}",portId);
		System.out.println("Entering runProtocolStateMachine for port={}" + portId);

		if((lacpBpdu != null) || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.CURRENT_WHILE_TIMER))){
			this.portRxStateMachine(lacpBpdu,tmExpMsg);
		}
		
		if((lacpBpdu != null) || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER))){

			this.portPeriodicStateMachine(lacpBpdu,tmExpMsg);
		}
		
		//if(lacpBpdu != null){
			this.portSelectionLogic();
		//}
		if((lacpBpdu != null) || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER))){
		
			this.portMuxStateMachine(tmExpMsg);
		}
		
		//timer expiry is passed to Tx state m/c just to check on which queue the port object needs to be enqued
		this.portTxStateMachine(tmExpMsg);
		
		// turn off the BEGIN bit, since we already handled it
	        if ((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
                     this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~LacpConst.PORT_BEGIN));
        	}

		log.info("Exiting runProtocolStateMachine for port={}",portId);
		System.out.println("Exting runProtocolStateMachine for port={}" + portId);
	}
	
	public void disableCollectingDistributing(short portId, LacpAggregator agg) {
		log.info("Entering disableCollectingDistributing for port={}",portId);
		this.activeSince = null;
		//do we need to call below method? - CHECKLATER
		//agg.setIsActive((short)0);
                bond.removeActivePort(this);
		log.info("Exiting disableCollectingDistributing for port={}",portId);
	}

	public void enableCollectingDistributing(short portId, LacpAggregator agg) {
		log.info("Entering enableCollectingDistributing for port={}",portId);
		if (agg.getIsActive() == 0) {
			if (agg.aggGetBond()!=null) {
				log.info("Calling bondAggSelectionLogic for port={}",portId);
				agg.aggGetBond().bondAggSelectionLogic();
			}
		}
		if (agg.getIsActive()>0) {
			this.activeSince = new Date();
		}
                bond.addActivePort(this);
		log.info("Exiting enableCollectingDistributing for port={}",portId);
	}

	public void portRxStateMachine(LacpBpduInfo lacpdu,TimerExpiryMessage timerExpired ){
		log.info("Entering portRxStateMachine for port={}",portId);
		System.out.println("LacpPort: Entering portRxStateMachine for port={}" + portId);
		boolean timerExpiredFlag = false;
		LacpConst.RX_STATES lastState;
		lastState = rxContext.getState().getStateFlag();
		
		if((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
				rxContext.setState(rxInitializeState);
				log.info("portRxStateMachine setting port={} to rxInitializeState",portId);
		}
		else if ((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) == 0 && !this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_MOVED)==0)){
				rxContext.setState(rxPortDisabledState);
				log.info("portRxStateMachine setting port={} to rxPortDisabledState",portId);
		}else if((lacpdu!=null) 
				 && ((rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_EXPIRED)
				 ||  (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_DEFAULTED) 
				 ||  (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_CURRENT))){
				
			if(!currWhileTimeout.isExpired()){
				currWhileTimeout.cancel();
			}
			setCurrentWhileTimer(LacpConst.LONG_TIMEOUT_TIME);
			rxContext.setState(rxCurrentState);
			log.info("portRxStateMachine setting port={} to rxCurrentState",portId);
		}else if((timerExpired !=null && timerExpired.getTimerWheelType() == Utils.timerWheeltype.CURRENT_WHILE_TIMER)){
					log.info("portRxStateMachine - current while timer has expired for port={}",portId);
					timerExpiredFlag = true;
					if(rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_EXPIRED){
						rxContext.setState(rxDefaultedState);
						log.info("portRxStateMachine setting port={} to rxDefaultedState",portId);
					}else if (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_CURRENT){
						rxContext.setState(rxExpiredState);
						log.info("portRxStateMachine setting port={} to rxExpiredState",portId);
					}else{
						//log
					}
		}else { 
			switch (rxContext.getState().getStateFlag()) {
			case RX_PORT_DISABLED:
				if ((this.getStateMachineBitSet() & LacpConst.PORT_MOVED)>0){
					rxContext.setState(rxInitializeState);
					log.info("portRxStateMachine setting port={} to rxInitializeState",portId);
				}else if (this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)>0)) {
					rxContext.setState(rxExpiredState);      
					log.info("portRxStateMachine setting port={} to rxExpiredState",portId);
					log.info("RX Machine Port=" + this.portId + " lastState=" + lastState +" state setting to RX_EXPIRED");
				} else if (this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)== 0))
					rxContext.setState(rxLacpDisabledState);
					log.info("portRxStateMachine setting port={} to rxLacpDisabledState",portId);
				break;
			default:    
				break;
			}
		}
		
		if ((rxContext.getState().getStateFlag() != lastState) || (lacpdu!=null) || (timerExpiredFlag == true)) {
			log.info("portRxStateMachine - calling executeStateAction method");
			rxContext.getState().executeStateAction(rxContext, this,lacpdu);
			slaveGetBond().setDirty(true);
		}
		
		log.info("RX Machine Port=" + this.portId + " lastState=" + lastState + " currentState=" + rxContext.getState().getStateFlag() + " PDU = "+ lacpdu);
		log.info("Exiting portRxStateMachine for port={}" + portId);
		System.out.println("LacpPort: Exiting portRxStateMachine for port={}" + portId);
	}



	void portPeriodicStateMachine(LacpBpduInfo lacpdu,TimerExpiryMessage timerExpired)
	{
	
		log.info("Entering portPeriodicStateMachine for port={}",portId);
		System.out.println("Entering portPeriodicStateMachine for port={}" + portId);
		LacpConst.PERIODIC_STATES lastState;
		// keep current state machine state to compare later if it was changed
		lastState = periodicTxContext.getState().getStateFlag();
		
		if ((((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0) || ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)==0)) ||
				(((this.getActorOperPortState() & LacpConst.PORT_STATE_LACP_ACTIVITY)==0) && 
						((this.partnerOper.portState & LacpConst.PORT_STATE_LACP_ACTIVITY)==0))) 
		{
			periodicTxContext.setState(periodicTxNoPeriodicState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxNoPeriodicState",portId);
			System.out.println("portPeriodicStateMachine setting port={} to periodicTxNoPeriodicState" + portId + "and the flag is : " + periodicTxContext.getState().getStateFlag());
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.FAST_PERIODIC) &&
						(this.partnerOper.getPortState() & LacpConst.LONG_TIMEOUT)==0){
			periodicTxContext.setState(periodicTxSlowState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxSlowState",portId);
			System.out.println("portPeriodicStateMachine setting port={} to periodicTxSlowState" + portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.FAST_PERIODIC) &&
				((timerExpired != null) && (timerExpired.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER))){
			periodicTxContext.setState(periodicTxPeriodicState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxPeriodicState",portId);
			System.out.println("portPeriodicStateMachine setting port={} to periodicTxPeriodicState" + portId);
		}
		else if ((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.SLOW_PERIODIC) &&
						((this.partnerOper.getPortState() & LacpConst.SHORT_TIMEOUT)==0) || 
								(((timerExpired != null) && (timerExpired.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER)))){
			periodicTxContext.setState(periodicTxPeriodicState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxPeriodicState",portId);
			System.out.println("portPeriodicStateMachine setting port={} to periodicTxPeriodicState" + portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.PERIODIC_TX) &&
				((this.partnerOper.getPortState() & LacpConst.SHORT_TIMEOUT)==0)){
			periodicTxContext.setState(periodicTxFastState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxFastState",portId);
			System.out.println("portPeriodicStateMachine setting port={} to periodicTxFastState" + portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.PERIODIC_TX) &&
				((this.partnerOper.getPortState() & LacpConst.LONG_TIMEOUT)==0)){
			periodicTxContext.setState(periodicTxSlowState);
			log.info("portPeriodicStateMachine setting port={} to periodicTxSlowState",portId);
		}
		log.info("portPeriodicStateMachine - calling executeStateAction method and the stateFlag is={}", periodicTxContext.getState().getStateFlag());
		System.out.println("portPeriodicStateMachine - calling executeStateAction method and the stateFlag is : " + periodicTxContext.getState().getStateFlag());
		if(lastState != periodicTxContext.getState().getStateFlag()){
		System.out.println("lastState != currentState for port={} " + portId + "lastState is : " + lastState + " and the current State is : " + periodicTxContext.getState().getStateFlag());
		log.info("lastState != currentState for port={}, lastState is={} and the current State is={}", portId,lastState, periodicTxContext.getState().getStateFlag());
			periodicTxContext.getState().executeStateAction(periodicTxContext,this,lacpdu);
			slaveGetBond().setDirty(true);
  		}else{
		System.out.println("lastState == currentState for port={} " + portId + "lastState is : " + lastState + " and the current State is : " + periodicTxContext.getState().getStateFlag());
		log.info("lastState == currentState for port={}, lastState is={} and the current State is={}", portId,lastState, periodicTxContext.getState().getStateFlag());
		}
		log.info("Exiting portPeriodicStateMachine for port={}",portId);
		System.out.println("Exiting portPeriodicStateMachine for port={}" + portId);
	}
	
	void portSelectionLogic()
	{

		log.info("Entering portSelectionLogic for port={}",portId);
		System.out.println("Entering portSelectionLogic for port={}" + portId);

		LacpAggregator freeAgg = null;
		LacpAggregator tempAgg = null;
		LacpAggregator foundAgg = null;
		
		if ((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0){
			log.info("portSelectionLogic is returing without further processing as the port={} is already PORT_SELECTED",portId);
			return;
		}

		foundAgg =  (bond!= null ? bond.findLacpAggByFitPort(this) : null);
		
		if (this.getPortAggregator() !=null) {
			if (foundAgg!=null && foundAgg == this.getPortAggregator() && ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY)>0)) {
				if (!foundAgg.canMoveToSelList(this)) {
					return;
				}
				if (this.getPortAggregator().isReselect()){
					this.getPortAggregator().setReselect(false);
					log.info(
							"Standby Port[{}] found aggregator[ID={}] ",
							HexEncode.longToHexString((long)this.portId),
									HexEncode.longToHexString((long)foundAgg.getAggId()));
				}
			}
			tempAgg = this.getPortAggregator();
			if (tempAgg.aggHasPort(this)) {
				if (tempAgg.getIsActive() > 0){
					tempAgg.setReselect(true);
				}
				tempAgg.rmPortFromAgg(this);
				this.setPortAggregator(null);
				this.setActorPortAggregatorIdentifier((short)0);
			} else if (tempAgg.aggHasStandbyPort(this)) {
				tempAgg.rmPortFromAggStandBy(this);
				this.setPortAggregator(null);
				this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & (~LacpConst.PORT_STANDBY)));
				this.setActorPortAggregatorIdentifier((short)0);					
			}
		}
		slaveGetBond().setDirty(true);
		if (foundAgg != null ) {
		      if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0) && ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) == 0)) {
		      		if (foundAgg.IsPortReachMaxCount(this)) {
							log.info(
							"Port[{}] is moved to Standby for Aggregator[ID={}] ",
							 HexEncode.longToHexString((long)this.portId),
									HexEncode.longToHexString((long)foundAgg.getAggId()));
	    		  		this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_STANDBY));
	    		  		this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & (~LacpConst.PORT_SELECTED)));
					foundAgg.addPortToAggStandBy(this);
			  		this.setPortAggregator(foundAgg);
			  		this.setActorPortAggregatorIdentifier(foundAgg.getAggId());
		      		}
		      		else {
					log.info(
							"Port[{}] selects Aggregator[ID={}] ",
							HexEncode.longToHexString((long)this.portId),
				  					HexEncode.longToHexString((long)foundAgg.getAggId()));				    		  
			  		this.setPortAggregator(foundAgg);
			  		this.setActorPortAggregatorIdentifier(foundAgg.getAggId());
			  		this.getPortAggregator().addPortToAgg(this);
		    	  		this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_SELECTED));
		      		}	
		      } else {
				freeAgg = this.slaveGetFreeAgg();
				if (freeAgg!=null) {
					this.setPortAggregator(freeAgg);
					this.setActorPortAggregatorIdentifier(this.getPortAggregator().getAggId());
					this.getPortAggregator().copyAggInfoFromPort(this);
			                this.getPortAggregator().addPortToAgg(this);
	        		        this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_SELECTED));
					log.info(
							"Port[{}] joins in new Aggregator[ID={}]",
							HexEncode.longToHexString((long)this.portId),
							HexEncode.longToHexString((long)this.getActorPortAggregatorIdentifier()));
				} else {
					log.info(
							"Port[{}] can't find suitable aggregator",
							HexEncode.longToHexString((long)this.portId));
					return;
				}
		     } 
		} else {
			freeAgg = this.slaveGetFreeAgg();
			if (freeAgg!=null) {
				this.setPortAggregator(freeAgg);
				this.setActorPortAggregatorIdentifier(this.getPortAggregator().getAggId());
				this.getPortAggregator().copyAggInfoFromPort(this);
                		this.getPortAggregator().addPortToAgg(this);
                		this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_SELECTED));
				log.info(
						"Port[{}] joins in new Aggregator[ID={}] ",
						HexEncode.longToHexString((long)this.portId),
								HexEncode.longToHexString((long)this.getActorPortAggregatorIdentifier()));
			} else {
				log.info(
						"Port[{}] can't find suitable aggregator",
						 HexEncode.longToHexString((long)this.portId));
				return;
			}						
		}

		this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());

		if ((isInitialized) && slaveGetBond()!=null){
			log.info("portSelectionLogic calling bondAggSelectionLogic for port={}",portId);
			slaveGetBond().bondAggSelectionLogic();
		}
		log.info("Exiting portSelectionLogic for port={}",portId);
		System.out.println("Exiting portSelectionLogic for port={}" + portId);
	}
	
	
	void portMuxStateMachine(TimerExpiryMessage timerExpired){
		
		log.info("Entering portMuxStateMachine for port={}",portId);
		System.out.println("Entering portMuxStateMachine for port={}" + portId);

		LacpConst.MUX_STATES lastState;
		lastState = muxContext.getState().getStateFlag();
		
		if((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
			muxContext.setState(muxDetachedState);
			log.info("portMuxStateMachine setting port={} to muxDetachedState",portId);
		}
		else {
			if((muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_DETACHED)){
				if((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED)>0 || (this.getStateMachineBitSet() & LacpConst.PORT_STANDBY)>0){
					muxContext.setState(muxWaitingState);
					log.info("portMuxStateMachine setting port={} to muxWaitingState",portId);
				}
			}
			else if((muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_WAITING)){
				
				if((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0){
					this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~(LacpConst.PORT_READY_N)));
					this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
					muxContext.setState(muxDetachedState);
					log.info("portMuxStateMachine setting port={} to muxDetachedState",portId);
				}else{					
					
					System.out.println("Mux waiting state and port " + portId + " is in SELECTED state");
					//TODO - CHECK THIS LOGIC ONCE AGAIN
					if(timerExpired!= null){
						if(timerExpired.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER){
							System.out.println("Calling setAggPortsReady for port" + portId );
							//this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
							//TODO - RAJESH TO CHECK WHETHER THIS LOGIC IS OK
							//this.getPortAggregator().setAggPortsReady(1);
							this.setPortsReady(1);
						}
					}
					// if the waitWhileTimer expired, and the port is in READY state, move to ATTACHED state
					if (((this.getStateMachineBitSet() & LacpConst.PORT_READY) > 0)
							&& ((timerExpired != null) && timerExpired.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER)) {
						muxContext.setState(muxAttachedState); 
						log.info("portMuxStateMachine wait while timer expired for port={}",portId);
						log.info("portMuxStateMachine setting port={} to muxAttachedState",portId);
						System.out.println("portMuxStateMachine wait while timer expired for port= " + portId);
						System.out.println("portMuxStateMachine setting port = " + portId + "to muxAttachedState");
					}
				}
			}else if (muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_ATTACHED){
			
				if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0) && ((this.partnerOper.portState & LacpConst.PORT_STATE_SYNCHRONIZATION) > 0) 
						/*&& checkAggSelectionTimer()==0*/) {
					    
						muxContext.setState(muxCollectingDistributingState);
						log.info("portMuxStateMachine setting port={} to muxCollectingDistributingState",portId);
					System.out.println("portMuxStateMachine setting port= " + portId +  "to muxCollectingDistributingState");
					
				} else if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0) || ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) > 0)) {    
						this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~LacpConst.PORT_READY_N));

						this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
						muxContext.setState(muxDetachedState);
						log.info("portMuxStateMachine setting port={} to muxDetachedState",portId);
				}
			}else if (muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING){
				if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED)==0) || ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) > 0) ||
						((this.partnerOper.portState & LacpConst.PORT_STATE_SYNCHRONIZATION))==0) {
					 
						muxContext.setState(muxAttachedState);
						log.info("portMuxStateMachine setting port={} to muxAttachedState",portId);
						short aggrId = 0;
						
						try {
							LacpAggregator lacpAggr = this.getPortAggregator();
							aggrId = lacpAggr.getAggId();
							log.info("Port[{}] AGG  [ID={}, STATUS={}] will move Collecting to Attached State because SmVar= {}, Partner State={}",
							this.portId, aggrId,
							(this.getPortAggregator().getIsActive() > 0 ? "Active" : "Ready"), this.getStmStateString(this.getStateMachineBitSet()),
							this.getPortStateString((byte)this.partnerOper.portState));
						} catch (Exception e) {
							String errStr = "last_lacp_sm_state :mux_coll_dist Bad lacpAggr";
							log.error(errStr);
							e.printStackTrace();
						}
				} 
			}else{
				//no-op
			}
			
		}
		
		if(muxContext.getState().getStateFlag() != lastState) {
			log.info("portMuxStateMachine calling executeStateAction for port={}",portId);
			System.out.println("The Mux State Flag is : " + muxContext.getState().getStateFlag());
			log.info("The Mux State Flag is : " + muxContext.getState().getStateFlag());
			muxContext.getState().executeStateAction(muxContext,this);
			slaveGetBond().setDirty(true);
		}
		System.out.println("Exiting portMuxStateMachine for port={}" + portId);
	}
		

	void portTxStateMachine(TimerExpiryMessage timerExpired)
	{
		log.info("Entering portTxStateMachine for port={}",portId);
		System.out.println("Entering portTxStateMachine for port={}" + portId);
		LacpTxQueue.QueueType qType = LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE;

		if(timerExpired != null){

			if(timerExpired.getTimerWheelType() == Utils.timerWheeltype.CURRENT_WHILE_TIMER){
				qType = LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE;
			}else if(timerExpired.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER){
				qType = LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE;
			}else if(timerExpired.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER){
				qType = LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE;
			}else{
				//log
			}
		}else{
			qType = LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE;
		}

		/*
		// check if tx timer expired, to verify that we do not send more than 3 packets per second
		// this logic only applies when we are operating in fast periodic time...
		// currently we operate only in slow periodic time....
		*/
	 
		// check if there is something to put on tx queue
		if ((this.isNtt()) && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)>0)) {
				log.info("portTxStateMachine putting port={} on to tx queue",portId);
				System.out.println("portTxStateMachine putting port={} on to tx queue and qType is={}" + portId + qType);
				LacpPacketPdu obj = updateLacpFromPortToLacpPacketPdu();
				lacpduSend(obj,qType);
				this.setNtt(false);
		}
		
		log.info("Exiting portTxStateMachine for port={}",portId);
		System.out.println("Exiting portTxStateMachine for port={}" + portId);
	}
		

	void portHandleLinkChange(byte link)
	{
				
		log.info("Entering portHandleLinkChange for port={}", portId);
		if (link == LacpConst.BOND_LINK_UP) {
			isEnabled = true;
			log.info("portHandleLinkChange port={} , link is BOND_LINK_UP", portId);
			actorOperPortKey = getActorAdminPortKey();
        	} else {
			log.info("portHandleLinkChange port={} , link is BOND_LINK_DOWN", portId);
                	/* link has failed */
                	isEnabled = false;
                	actorOperPortKey = getActorAdminPortKey();
                 	if (this.isPortAttDist()) {
                	 	log.info("Port is distribution mode for delete before sending update");
                	 	disableCollectingDistributing(this.portId,this.getPortAggregator());
                 	}	

        	}
		portSetLagId();
        	setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));
		log.info("Exiting portHandleLinkChange for port={}", portId);
	}
	
	void portAssignSlave(byte[] sys_mac_addr, int lacp_fast, int sys_priority, int port_priority, short admin_key) {
		
		log.info("Entering portAssignSlave for port={}",portId);
		lacpInitPort(lacp_fast);
		this.isInitialized = true;
		
		setActorPortNumber(portId);
		setPortAdminPortKey(admin_key);
		actorOperPortKey = getActorAdminPortKey();
		this.setActorSystemPriority(sys_priority);
		this.setActorPortPriority(port_priority);

		if (!this.isLacpEnabled()) {
			setStateMachineBitSet((short)(getStateMachineBitSet() & ~LacpConst.PORT_LACP_ENABLED));
			log.info("portAssignSlave - setting port={} to PORT_LACP_ENABLED FALSE",portId);
		}
		
		setActorSystem(Arrays.copyOf(sys_mac_addr, LacpConst.ETH_ADDR_LEN));
		System.out.println("portAssignSlave - actorSystem mac is set to : " + getActorSystem());
		portSetLagId();
		log.info("Exiting portAssignSlave for port={}",portId);
	}
	
	public byte portGetPortStatus() {
		return (this.getLink());
	}
	
	public void portSetLagId() {
		
		log.info("Entering portSetLagId for port={}",portId);
		int result;
		LagId current;
		LagIdElem self = new LagIdElem(this.getActorSystemPriority(),this.getActorSystem(), this.actorOperPortKey, 
				this.getActorPortPriority(), this.getActorPortNumber());
		LagIdElem partner = new  LagIdElem(this.partnerOper.systemPriority,this.partnerOper.system, 
				this.partnerOper.key, this.partnerOper.portPriority, this.partnerOper.portNumber);
		result = self.compareTo(partner);
		if (result!=0) {
				current = new LagId(self,partner);
				this.lagId = current;
				log.info("portSetLagId setting port={} with lagid={}", portId,lagId.toString());
		}
		else {
			log.info("LagIdElement information duplicate between myself and partner : LagIdElement={}", self);
		}
		log.info("Exiting portSetLagId for port={}",portId);
	}
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataService = dataBroker;
    }
    public void updateNCLacpInfo ()
    {
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNodeConnector lacpNC;
        lacpNC  = lacpNCBuilder.build();
        InstanceIdentifier<LacpNodeConnector> lacpNCId = ncId.augmentation(LacpNodeConnector.class);
        write.merge(LogicalDatastoreType.OPERATIONAL, lacpNCId, lacpNC, true);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess (Object o)
            {
                log.debug("LacpNodeConnector updation write success for txt {}", write.getIdentifier());
                System.out.println("LacpNodeConnector updation write success for txt {}"+ write.getIdentifier());
            }

            @Override
            public void onFailure (Throwable throwable)
            {
                log.error("LacpNodeConnector updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
                System.out.println("LacpNodeConnector updation write failed for tx {}"+ write.getIdentifier());
            }
        });
    }
    public InstanceIdentifier getNodeConnectorId(){
        return this.ncId;
    }
    public void setLogicalNCRef (NodeConnectorRef ref)
    {  
        lacpNCBuilder.setLogicalNodeconnectorRef(ref);
        updateNCLacpInfo();
    }

	public MacAddress getSwitchHardwareAddress(){
		 NodeConnector portRef = null;
		 MacAddress hwMac = null;
		 DataBroker ds = LacpUtil.getDataBrokerService();
                 if(ds != null){
                 portRef = LacpPortProperties.getNodeConnector(ds, portTxLacpdu.getNCRef());
		 if(portRef != null){
                	FlowCapableNodeConnector flowCapNodeConn = portRef.getAugmentation(FlowCapableNodeConnector.class);
			hwMac = flowCapNodeConn.getHardwareAddress();

		 }else{
                     log.error("getHardwareAddress - Unable to get the DataBroker service,NOT processing the lacp pdu");
		 }
		}
		return hwMac;
	}

        public void lacpPortCleanup(){
                if(this.currWhileTimeout != null){
                        this.currWhileTimeout.cancel();
                }

                if(this.periodicTimeout != null){
                        this.periodicTimeout.cancel();
                }

                if(this.waitWhileTimeout != null){
                        this.waitWhileTimeout.cancel();
                }
                this.detachBondFromAgg();
        }
}
