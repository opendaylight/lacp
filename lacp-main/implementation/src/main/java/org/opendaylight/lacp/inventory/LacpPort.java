/*
 *  Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;


import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.util.Timeout;

import org.opendaylight.lacp.state.*;
import org.opendaylight.lacp.timer.*;
import org.opendaylight.lacp.timer.TimerFactory.LacpWheelTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.ActorInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.PartnerInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnectorBuilder;


import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;

import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
 
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.AggRef;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.queue.LacpPortInfo;
import org.opendaylight.lacp.Utils.*;
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

	private static int id = 1;
	private short lacpPortId;
	private long swId;       
	private short portId;  
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
    
        private static final Logger LOG = LoggerFactory.getLogger(LacpPort.class);
	private LacpNodeConnectorBuilder lacpNCBuilder;
	private InstanceIdentifier ncId;
	private static DataBroker dataService;
    private boolean operUpStatus; // to inform lag port timeout status to state machine
    private MacAddress ncMac = null;
    private boolean resetStatus;

	public enum portStateEnum {
		ACT(0),TMO(1),AGG(2),SYN(3),COL(4),DIS(5),DEF(6),EXP(7);
		private int state;
		portStateEnum(int st){
			state = st;
		}
		public int getPortEnumState(){
			return state;
		}
	}
	public enum stmStateEnum  {
		BEG(0),ENA(1),ACTC(2),PARC(3),RDY(4),RDYN(5),MAT(6),STA(7),SEL(8),MOV(9);
		private int state;
		stmStateEnum(int st){
			state  = st;
		}
		public int getStmEnumState(){
			return state;
		}
	}
 
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
			this.system = Arrays.copyOf(system, LacpConst.ETH_ADDR_LEN);
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
			if (this == obj){
				return true;
			}
			if (obj == null){
				return false;
			}
			if (!(obj instanceof PortParams)){
				return false;
			}
			PortParams other = (PortParams) obj;
			if (!getOuterType().equals(other.getOuterType())){
				return false;
			}
			if (key != other.key){
				return false;
			}
			if (portNumber != other.portNumber){
				return false;
			}
			if (portPriority != other.portPriority){
				return false;
			}
			if (!Arrays.equals(system, other.system)){
				return false;
			}
			if (systemPriority != other.systemPriority){
				return false;
			}
			return true;
		}
		


		public void intializePortParams() {
			systemPriority = (int) LacpConst.SYSTEM_PRIORITY;
			key = 1;
			portNumber =1;
			portPriority = (int)LacpConst.PORT_PRIORITY;
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
			this.system = Arrays.copyOf(system, LacpConst.ETH_ADDR_LEN);
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
		if (this.isLacpEnabled()!= enabled) {
			
			if (enabled) {
				this.setLacpEnabled(true);
	        		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_LACP_ENABLED));
				LOG.debug("Setting port={} to PORT_LACP_ENABLED to true", portId);
				
			} else {
				this.setLacpEnabled(false);
				setStateMachineBitSet((short)(getStateMachineBitSet() & ~LacpConst.PORT_LACP_ENABLED));
				LOG.debug("Setting port={} to PORT_LACP_ENABLED to false", portId);
			}
	        	setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));				
			LOG.debug("Setting port={} to PORT_BEGIN", portId);
		}	
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
		if (this.portAggregator != null){
			LOG.debug("getPortAggregatorId returning agg id={}",portAggregator.getAggId());
			return this.portAggregator.getAggId();
		}
		else{
			LOG.debug("getPortAggregatorId returning agg id as 0");
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
		this.actorSystem = Arrays.copyOf(actorSystem, LacpConst.ETH_ADDR_LEN);
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

	private LacpPort(long swId, short portId, LacpBond bond, int portPriority, LacpBpduInfo bpduInfo) {

		LOG.debug("Entering LacpPort constructor for switchid={} port={}",portId, swId);

		this.lacpPortId = (short)(id);
		if (++id > LacpConst.PORT_ID_MAX){
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
		setPortLock(new ReentrantLock(true));
		
		this.portPriority = portPriority;
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
	     
	       portAssignSlave(bond.getBondSystemId(), bond.getLacpFast(), bond.bondGetSysPriority(), this.portPriority, bond.getAdminKey());			
               ncId = bpduInfo.getNCRef().getValue();
               lacpNCBuilder = new LacpNodeConnectorBuilder();
               lacpNCBuilder.setActorPortNumber(this.actorPortNumber);
               lacpNCBuilder.setPeriodicTime(LacpUtil.DEF_PERIODIC_TIME);
               lacpNCBuilder.setLacpAggRef(new AggRef(bond.getLacpAggInstId()));
               LacpSystem lacpSystem = LacpSystem.getLacpSystem();
               LacpNodeExtn lacpNode = lacpSystem.getLacpNode(swId);
               if (lacpNode == null)
               {
                    LOG.warn("LacpNode {} associated with this port {} is null", swId, portId);
               }
               else
               {
                    synchronized (lacpNode)
                    {
                        lacpNode.removeNonLacpPort(ncId);
                        lacpNode.addLacpPort(ncId, this);
                    }
               }
        operUpStatus = true;
        resetStatus = true;
        DataBroker ds = LacpUtil.getDataBrokerService();
        NodeConnector portNC = LacpPortProperties.getNodeConnector(ds, ncId);
        if (portNC == null)
        {
            LOG.error ("Unable to read the nodeConnector for {}", ncId);
            return;
        }
        int result = LacpPortProperties.mapSpeedDuplexFromPortFeature(portNC);
        int speed = (result >> LacpConst.DUPLEX_KEY_BITS);
        byte duplex = (byte) (result & LacpConst.DUPLEX_KEY_BITS);
        this.slaveSetSpeed(speed);
        this.slaveSetDuplex(duplex);
        short key = (short)(result >> LacpConst.DUPLEX_KEY_BITS);
        this.setActorAdminPortKey(key);
        
        FlowCapableNodeConnector flowCapNodeConn = portNC.getAugmentation(FlowCapableNodeConnector.class);
        ncMac = flowCapNodeConn.getHardwareAddress();
        LOG.debug("Exiting LacpPort constructor for switchid={} port={}",portId, swId);
	}

        public RxContext getRxContextObject(){
               return rxContext;
        }

	public Timeout setCurrentWhileTimer(long delay){
	       LOG.debug("Entering setCurrentWhileTimer for switchid={} port={}",swId,portId);

		if((currWhileTimeout!= null) && (!currWhileTimeout.isExpired())){
                           currWhileTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		currWhileTimeout=instance.registerPortForCurrentWhileTimer(currentWhileTimer,delay, TimeUnit.SECONDS);
	        LOG.debug("Exiting setCurrentWhileTimer for switchid={} port={}",swId,portId);
		return currWhileTimeout;
	}
	
	public Timeout setWaitWhileTimer(long delay){
	       LOG.debug("Entering setWaitWhileTimer for switchid={} port={}",swId,portId);

		if((waitWhileTimeout!= null) && (!waitWhileTimeout.isExpired())){
                           waitWhileTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.WAIT_WHILE_TIMER);
		waitWhileTimeout=instance.registerPortForWaitWhileTimer(waitWhileTimer,delay, TimeUnit.SECONDS);
	        LOG.debug("Exiting setWaitWhileTimer for switchid={} port={}",swId,portId);
		return waitWhileTimeout;
	}
	
	public Timeout setPeriodicWhileTimer(long delay){

	        LOG.debug("Entering setPeriodicWhileTimer for switchid={} port={}",swId,portId);
		if((periodicTimeout != null) && (!periodicTimeout.isExpired())){
                           periodicTimeout.cancel();
                }

		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.PERIODIC_TIMER);
		periodicTimeout=instance.registerPortForPeriodicTimer(periodicTimer,delay, TimeUnit.SECONDS);
	        LOG.debug("Exiting setPeriodicWhileTimer for switchid={} port={}",swId,portId);
		return periodicTimeout;
	}
	
	public Timeout getPeriodicWhileTimer(){
		return periodicTimeout;
	}

	
	@Override
	public int compareTo(LacpPort arg0) {
		if (this.lagId == arg0.lagId){
			return 0;
		}
		if (arg0.lagId == null){
		 return -1;
		}
		if (this.lagId == null){
		 return 1;
		}
		return this.lagId.compareTo(arg0.lagId);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		int val = 32;
		result = prime * result + getInstanceId();
		result = prime * result + getActorAdminPortKey();
		result = prime * result + portId;
		result = prime * result + (int) (swId ^ (swId >>> val));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (!(obj instanceof LacpPort)){
			return false;
		}
		LacpPort other = (LacpPort) obj;
		if (getInstanceId() != other.getInstanceId()){
			return false;
		}
		if (getActorAdminPortKey() != other.getActorAdminPortKey()){
			return false;
		}
		if (portId != other.portId){
			return false;
		}
		if (swId != other.swId){
			return false;
		}
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
    	byte val = state;
    	String result="";
    	String token;
	portStateEnum seq = portStateEnum.ACT;
	portStateEnum seqEnd = portStateEnum.EXP;
	byte value = 0x7f;
	int i = seq.getPortEnumState();

    	while (i < (seqEnd.getPortEnumState() + 1)) {
    		token = "";
    		if ((val & 0x01)!=0) {
    			switch (seq) {
    			case ACT: 
    				token = "ACT";
    				break;
    			case TMO:
    				token = "TMO";
    				break;
    			case AGG:
    				token = "AGG";
    				break;
    			case SYN:
    				token = "SYN";
    				break;
    			case COL:
    				token = "COL";
    				break;
    			case DIS:
    				token = "DIS";
    				break;
    			case DEF: 
    				token = "DEF";
    				break;
    			case EXP: 
    				token = "EXP";
    				break;
    			}
    			result = result+" "+token; 
    		}
    		i++;
    		val = (byte) ((val >> 1) & value);
    	}
    	return result;
    }
	
    public String getStmStateString(short state) {
    	short val = state;
    	String result="";
    	String token;
	int value = 0x7ff;
	stmStateEnum seq = stmStateEnum.BEG;
	stmStateEnum seqEnd = stmStateEnum.MOV;
	int i = seq.getStmEnumState();

    	while (i < (seqEnd.getStmEnumState()+1)) {
    		token = "";
    		if ((val & 0x01)!=0) {
    			switch (seq) {
    			case BEG: 
    				token = "BEG";
    				break;
    			case ENA:
    				token = "ENA";
    				break;
    			case ACTC:
    				token = "ACTC";
    				break;
    			case PARC:
    				token = "PARC";
    				break;
    			case RDY:
    				token = "RDY";
    				break;
    			case RDYN:
    				token = "RDYN";
    				break;
    			case MAT: 
    				token = "MAT";
    				break;
    			case STA: 
    				token = "STA";
    				break;
    			case SEL: 
    				token = "SEL";
    				break;
    			case MOV: 
    				token = "MOV";
    				break;
    				
    			}
    			result = result+" "+token; 
    		}
    		i++;
    		val = (short) ((val >> 1) & value);
    	}
    	return result;
    }
    
	 public String getSpeedString(byte speed) {
		 LOG.debug("Entering getSpeedString for port={} and speed is={}",portId,speed);
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
		 LOG.debug("Exiting getSpeedString for port={} and speed is={}",portId,result);
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
               It will be updated when the LOG.calNCRef for the port is assigned. */
	}

	public static  LacpPort newInstance(long swId, short portId, LacpBond bond, int portPri,LacpBpduInfo bpduInfo) {
		LOG.debug("Entering/Exiting LacpPort newInstance() method for sw={} port={} priority={}",swId,portId,portPri);
		return new LacpPort(swId, portId, bond, portPri,bpduInfo);
	}
	
	public void attachBondToAgg() {
		
	}
	
	public void detachBondFromAgg() {
		
	}
	
	public void lacpInitPort(int lacpFast)
	{
		LOG.debug("Entering lacpInitPort for port={}",portId);
		this.setActorPortNumber(this.portId);
		this.setActorPortPriority((int)LacpConst.PORT_PRIORITY);
		Arrays.fill(getActorSystem(), (byte)0);
		this.setActorSystemPriority((int) LacpConst.SYSTEM_PRIORITY);
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
		if (lacpFast > 0){
			this.setActorOperPortState((byte)(this.getActorOperPortState()
					| LacpConst.PORT_STATE_LACP_TIMEOUT));
		}
		LOG.debug("Exiting lacpInitPort for port={}",portId);
	}

	public LacpBpduInfo getLacpBpduInfo(){
		return portTxLacpdu;
	}
	
	void setPortsReady(int val) 
	{
		LOG.debug("Entering setPortsReady for port={}",portId);
		if (val>0) {
			this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_READY));
			LOG.debug("setting PORT_READY for port={}",portId);
		}
		else{
			this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~(LacpConst.PORT_READY)));
			LOG.debug("setting PORT_READY to false for port={}",portId);
		}
		LOG.debug("Exiting setPortsReady for port={}",portId);
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
		LOG.debug("Entering get_Duplex for port={}",portId);
		if (getLink() != LacpConst.BOND_LINK_UP){
			LOG.debug("Exiting get_Duplex for port={} link is BOND_LINK_DOWN",portId);
			return(0);
		}
		else{
			LOG.debug("Exiting get_Duplex for port={} link is BOND_LINK_UP",portId);
			return(this.getDuplex());
		}
	}

	void setPortAdminPortKey(short val) {
		this.setActorAdminPortKey(val);
	}
	
	public static short toUnsigned(byte b) {
	    int val = 256;
	    return (short) (b >= 0 ? b : val + b);
	}
	
	public LacpPacketPdu updateLacpFromPortToLacpPacketPdu(){

		this.slavePSMLock();
		LacpPacketPduBuilder obj =null;

                try {
			LOG.debug("Entering updateLacpFromPortToLacpPacketPdu for port= {}, {}",swId, portId);

			obj = new LacpPacketPduBuilder();		

			obj.setIngressPort(portTxLacpdu.getNCRef());
			obj.setSrcAddress(getSwitchHardwareAddress());
			obj.setDestAddress( new MacAddress (LacpConst.LACP_DEST_MAC_STRING));

			obj.setLenType(Integer.valueOf((LacpConst.LEN_TYPE)));
			obj.setSubtype(SubTypeOption.SlowProtocol);
			obj.setVersion(VersionValue.LacpVersion);

			ActorInfoBuilder actorInfoBuilder = new ActorInfoBuilder();

			actorInfoBuilder.setSystemPriority(Integer.valueOf(this.getActorSystemPriority()));
			LOG.debug("actor system id before bytesToHex conversion is :", (this.getActorSystem()));
			actorInfoBuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(this.getActorSystem())));
			LOG.debug("actor system id after bytesToHex conversion is :", HexEncode.bytesToHexString(this.getActorSystem()));
			actorInfoBuilder.setKey(Integer.valueOf(this.actorOperPortKey));
			actorInfoBuilder.setPortPriority(Integer.valueOf((this.getActorPortPriority())));
			actorInfoBuilder.setPort(Integer.valueOf(this.getActorPortNumber()));

			short pState = toUnsigned(this.getActorOperPortState());
			actorInfoBuilder.setState(Short.valueOf(pState));

			actorInfoBuilder.setTlvType(TlvTypeOption.ActorInfo);
			actorInfoBuilder.setInfoLen(Short.valueOf(LacpConst.ACTOR_INFO_LEN));
			actorInfoBuilder.setReserved(Integer.valueOf(LacpConst.RESERVED));
			actorInfoBuilder.setReserved1(Short.valueOf((short)LacpConst.RESERVED));
		

			final PortParams partner = this.partnerOper;
			PartnerInfoBuilder partnerInfoBuilder = new PartnerInfoBuilder();

			partnerInfoBuilder.setSystemPriority(Integer.valueOf(partner.systemPriority));
			LOG.debug("partner system id before bytesToHex conversion is :", (partner.system));
			partnerInfoBuilder.setSystemId(new MacAddress(HexEncode.bytesToHexStringFormat(partner.system)));
			LOG.debug("partner system id after bytesToHex conversion is :", HexEncode.bytesToHexStringFormat(partner.system));
			partnerInfoBuilder.setKey(Integer.valueOf(partner.key));
			partnerInfoBuilder.setPortPriority(Integer.valueOf(partner.portPriority));
			partnerInfoBuilder.setPort(Integer.valueOf(partner.portNumber));

			partnerInfoBuilder.setState(Short.valueOf(partner.portState));

			partnerInfoBuilder.setTlvType(TlvTypeOption.PartnerInfo);
			partnerInfoBuilder.setInfoLen(Short.valueOf(LacpConst.PARTNER_INFO_LEN));
			partnerInfoBuilder.setReserved(Integer.valueOf(LacpConst.RESERVED));
			partnerInfoBuilder.setReserved1(Short.valueOf((short)LacpConst.RESERVED));
		
			obj.setActorInfo(actorInfoBuilder.build());
			obj.setPartnerInfo(partnerInfoBuilder.build());

			obj.setCollectorMaxDelay(Integer.valueOf(0));
			obj.setCollectorTlvType(TlvTypeOption.CollectorInfo);
			obj.setCollectorInfoLen(Short.valueOf(LacpConst.COLLECTOR_INFO_LEN));
			obj.setCollectorReserved(new BigInteger("0"));
			obj.setCollectorReserved1(Long.valueOf(LacpConst.RESERVED));

			obj.setTerminatorTlvType(TlvTypeOption.Terminator);
			obj.setTerminatorInfoLen(Short.valueOf(LacpConst.TERMINATOR_INFO_LEN));
			obj.setTerminatorReserved(new String("0"));
			obj.setFCS(0L);

			LOG.debug("The PDU object to be enqued onto Tx queue ActorInfo: ",  obj.getActorInfo());
			LOG.debug("The PDU object to be enqued onto Tx queue PartnerInfo: ",  obj.getPartnerInfo());
			LOG.debug("Exiting updateLacpFromPortToLacpPacketPdu for port={}, {}",swId, portId);
		}finally{
			this.slavePSMUnlock();
		}
		return obj.build();
	}

	int lacpduSend(LacpTxQueue.QueueType qType)
	{
		LOG.debug("Entering lacpduSend for port={}", portId);
		if (isInitialized) {
			slaveSendBpdu(qType);
		}
		LOG.debug("Exiting lacpduSend for port={}", portId);
		return 0;
	}
	
	boolean isPortAttDist() {
		LOG.debug("Entering isPortAttDist for port={}",portId);
		if (((this.getActorOperPortState() & LacpConst.PORT_STATE_COLLECTING) > 0) || ((this.getActorOperPortState() & LacpConst.PORT_STATE_DISTRIBUTING) > 0)){
			LOG.debug("isPortAttDist is returning true");
			return true;
		}
		LOG.debug("isPortAttDist is returning false");
		LOG.debug("Exiting isPortAttDist for port={}",portId);
		return false;
	}
	
	boolean isPortSelected() {
		LOG.debug("Entering isPortSelected for port={}",portId);
		if ((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0){
			LOG.debug("isPortSelected is returning PORT_SELECTED=true");
			return true;
		}
		LOG.debug("isPortSelected is returning PORT_SELECTED=false");
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
		LOG.debug("Entering lacpDisablePort for port={}",portId);
		 boolean selectNewActiveAgg = false;

		 if (!isInitialized){
			 return;
		 }

		 LacpAggregator aggregator = this.getPortAggregator();
		 
		 portSetActorOperPortState((byte)(this.portGetActorOperPortState() & (~LacpConst.PORT_STATE_AGGREGATION)));

		 if (aggregator != null && aggregator.aggHasPort(this)) {
			 if (aggregator.getNumOfPorts() == 1){
				 selectNewActiveAgg = aggregator.getIsActive() > 0 ? true :  false;
			 }
			 aggregator.rmPortFromAgg(this);
			 if (selectNewActiveAgg){ 
				 bond.bondAggSelectionLogic();
			 }
			 
		 } else {
			 LOG.error("lacpDisbalePort: bad aggregator = {}", aggregator);
		 }
		isInitialized = false;
		LOG.debug("Exiting lacpDisablePort for port={}",portId);
	}
			 
	void slavePortPriorityChange(int priority) {

		LOG.debug("Entering slavePortPriorityChange for port={}",portId);
		slavePSMLock();
		 try {
		 	if (portGetActorPortPriority() == priority){
				return;
		 	}
		 	portPortPriorityChange(priority);
		 } finally {
		 	slavePSMUnlock();
		 }
		LOG.debug("Exiting slavePortPriorityChange for port={}",portId);
	 }
	 
	 void slaveSystemPriorityChange(int priority) {
		LOG.debug("Entering slaveSystemPriorityChange for port={}",portId);
		 slavePSMLock();
		 try {
		 	if (portGetActorSystemPriority() == priority){
				return;
		 	}
		 	portSystemPrioriyChange(priority);
		 } finally {
		 	slavePSMUnlock();
		 }
		LOG.debug("Exiting slaveSystemPriorityChange for port={}",portId);
	 }

	public void slaveHandleLinkChange(byte link)
	{
		LOG.debug("Entering slaveHandleLinkChange for port={}",portId);
		if (this.getLink()!= link) {
			this.setLink(link);
			portHandleLinkChange(link);
		}
		LOG.debug("Exiting slaveHandleLinkChange for port={}",portId);
	}
	

	public int slaveRxLacpBpduReceived(LacpBpduInfo lacpdu)
	{
		LOG.debug("Entering slaveRxLacpBpduReceived for port={}",portId);
		int ret = LacpConst.RX_HANDLER_DROPPED;

		if (this.getLink() != LacpConst.BOND_LINK_UP){
			  LOG.warn("in method slaveRxLacpBpduReceived() - lacp pdu not processed as link is down for switch {} port {}", lacpdu.getSwId(), lacpdu.getPortId());
			 return ret;
		}
	
		if (!this.isInitialized) {
			  LOG.warn("in method slaveRxLacpBpduReceived() - lacp pdu not processed as port is not initialized for switch {} port {}", lacpdu.getSwId(), lacpdu.getPortId());
			return ret;
		}
		ret = LacpConst.RX_HANDLER_CONSUMED;

		TimerExpiryMessage obj = null;
		LOG.debug("Calling runProtocolStateMachine for port={}, {}",swId, portId);
		runProtocolStateMachine(lacpdu,obj);

		LOG.debug("Exiting slaveRxLacpBpduReceived for port={}, {}",swId, portId);
		return ret;
	}	
	
	public void slaveUpdateLacpRate(int lacpFast) {
		
		LOG.debug("Entering slaveUpdateLacpRate for port={}",portId);
		if ( !isInitialized){
			return;
		}

		slavePSMLock();
		try {

			if (lacpFast>0){
				portSetActorOperPortState((byte)(portGetActorOperPortState() | LacpConst.PORT_STATE_LACP_TIMEOUT));
				LOG.debug("setting actor oper state to PORT_STATE_LACP_TIMEOUT");
			}	
        		else{
				portSetActorOperPortState((byte)(portGetActorOperPortState() & (~LacpConst.PORT_STATE_LACP_TIMEOUT)));
				LOG.debug("setting actor oper state to ~PORT_STATE_LACP_TIMEOUT");
			}
		} finally {
			slavePSMUnlock();
		}
		LOG.debug("Exiting slaveUpdateLacpRate for port={}",portId);
        }
	
	public void portPortPriorityChange(int priority) {
		LOG.debug("Entering portPortPriorityChange for port={}",portId);
		if (!isInitialized){
			return;
		}
		this.setActorPortPriority(priority);
		portSetLagId();			 
		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));				
		LOG.debug("setting state machine vars to PORT_BEGIN for port={}",portId);
		LOG.debug("Exiting portPortPriorityChange for port={}",portId);
	}		
	 
	 
	void portSystemPrioriyChange(int priority)
	{

		LOG.debug("Entering portSystemPrioriyChange for port={}",portId);
		if (!isInitialized){
			return;
		}
		portSetActorSystemPriority(priority);
		portSetLagId();			 
		setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));
		LOG.debug("setting state machine vars to PORT_BEGIN for port={}",portId);
		LOG.debug("Exiting portSystemPrioriyChange for port={}",portId);
	}
	
    public int slaveSendBpdu(LacpTxQueue.QueueType qType)
    {
        LOG.debug("Entering slaveSendBpdu for switch{}, port={}",this.swId, this.portId);
        if (this.getLink() != LacpConst.BOND_LINK_UP)
        {
			LOG.warn("slaveSendBpdu did not put the portId onto queue as port={} link is down ",portId);
			return -1;
        }
        LacpTxQueue lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();
        LacpPortInfo portInfo = new LacpPortInfo(this.swId, this.portId);
		lacpTxQueue.enqueue(qType, portInfo);
		LOG.debug("slaveSendBpdu sucessfully put the portId onto queue for port={}",portId);
		return 0;
	}
	
	public void runProtocolStateMachine(LacpBpduInfo lacpBpdu, TimerExpiryMessage tmExpMsg) {
		
		LOG.debug("Entering runProtocolStateMachine for port={}, {}",swId, portId);

		if((lacpBpdu != null) || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.CURRENT_WHILE_TIMER))){
			this.portRxStateMachine(lacpBpdu,tmExpMsg);
		}
		
		if((lacpBpdu != null) || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER))){
			this.portPeriodicStateMachine(lacpBpdu,tmExpMsg);
		}
		
		this.portSelectionLogic();

		if ((lacpBpdu != null)
                    || ((tmExpMsg != null) && (tmExpMsg.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER))
                    || ((this.getStateMachineBitSet() & LacpConst.PORT_AGG_RESELECT) == LacpConst.PORT_AGG_RESELECT))
                {
	             this.portMuxStateMachine(tmExpMsg);
                     if ((this.getStateMachineBitSet() & LacpConst.PORT_AGG_RESELECT) == LacpConst.PORT_AGG_RESELECT)
                     {
                          this.setStateMachineBitSet((short) (this.getStateMachineBitSet() & ~LacpConst.PORT_AGG_RESELECT));
                     }
		}
		
		//timer expiry is passed to Tx state m/c just to check on which queue the port object needs to be enqued
		this.portTxStateMachine(tmExpMsg);
		
		// turn off the BEGIN bit, since we already handled it
	        if ((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
                     this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~LacpConst.PORT_BEGIN));
        	}

		LOG.debug("Exiting runProtocolStateMachine for port={}, {}",swId, portId);
	}
	
	public void disableCollectingDistributing(short portId, LacpAggregator agg) {
		LOG.debug("Entering disableCollectingDistributing for port={}, {}",swId, portId);
		this.activeSince = null;
		//do we need to call below method? - CHECKLATER
		//agg.setIsActive((short)0);
                bond.removeActivePort(this);
		LOG.debug("Exiting disableCollectingDistributing for port={}, {}",swId, portId);
	}

	public void enableCollectingDistributing(short portId, LacpAggregator agg) {
		LOG.debug("Entering enableCollectingDistributing for port={}, {}",swId, portId);
		if (agg.getIsActive() == 0) {
			if (agg.aggGetBond()!=null) {
				LOG.debug("Calling bondAggSelectionLogic for port={}, {}",swId, portId);
				agg.aggGetBond().bondAggSelectionLogic();
			}
		}
		if (agg.getIsActive()>0) {
			this.activeSince = new Date();
		}
                bond.addActivePort(this);
		LOG.debug("Exiting enableCollectingDistributing for port={}, {}",swId, portId);
	}

	public void portRxStateMachine(LacpBpduInfo lacpdu,TimerExpiryMessage timerExpired ){
		LOG.debug("Entering portRxStateMachine for port={}, {}",swId, portId);
		boolean timerExpiredFlag = false;
		LacpConst.RX_STATES lastState;
		lastState = rxContext.getState().getStateFlag();
		
		if((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
				rxContext.setState(rxInitializeState);
				LOG.debug("portRxStateMachine setting port={} to rxInitializeState",portId);
		}
		else if ((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) == 0 && !this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_MOVED)==0)){
				rxContext.setState(rxPortDisabledState);
				LOG.debug("portRxStateMachine setting port={} to rxPortDisabledState",portId);
		}else if((lacpdu!=null) 
				 && ((rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_EXPIRED)
				 ||  (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_DEFAULTED) 
				 ||  (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_CURRENT))){
				
			if(!currWhileTimeout.isExpired()){
				currWhileTimeout.cancel();
			}
			setCurrentWhileTimer(LacpConst.LONG_TIMEOUT_TIME);
                        
                        if (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_DEFAULTED)
                        {
                            /* Lacp Pdu received in defaulted state. move to current.
                             *  change the port from non-lacp port to lacp port */
                            LacpSystem lacpSystem = LacpSystem.getLacpSystem();
                            LacpNodeExtn lacpNode = lacpSystem.getLacpNode(swId);
                            if (lacpNode == null)
                            {
                                LOG.warn("LacpNode {} associated with this port {} is null", swId, portId);
                            }
                            else
                            {
                                synchronized (lacpNode)
                                {
                                    lacpNode.removeNonLacpPort(ncId);
                                    lacpNode.addLacpPort(ncId, this);
                                }
                            }
                        }

			rxContext.setState(rxCurrentState);
			LOG.debug("portRxStateMachine setting port={} to rxCurrentState",portId);
		}else if((timerExpired !=null && timerExpired.getTimerWheelType() == Utils.timerWheeltype.CURRENT_WHILE_TIMER)){
					LOG.debug("portRxStateMachine - current while timer has expired for port={}",portId);
					timerExpiredFlag = true;
					if(rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_EXPIRED){
						rxContext.setState(rxDefaultedState);
						LOG.debug("portRxStateMachine setting port={} to rxDefaultedState",portId);
					}else if (rxContext.getState().getStateFlag() == LacpConst.RX_STATES.RX_CURRENT){
						rxContext.setState(rxExpiredState);
						LOG.debug("portRxStateMachine setting port={} to rxExpiredState",portId);
					}else{
						//log
					}
		}else { 
			switch (rxContext.getState().getStateFlag()) {
			case RX_PORT_DISABLED:
				if ((this.getStateMachineBitSet() & LacpConst.PORT_MOVED)>0){
					rxContext.setState(rxInitializeState);
					LOG.debug("portRxStateMachine setting port={} to rxInitializeState",portId);
				}else if (this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)>0)) {
					rxContext.setState(rxExpiredState);      
					LOG.debug("portRxStateMachine setting port={} to rxExpiredState",portId);
					LOG.debug("RX Machine Port=" + this.portId + " lastState=" + lastState +" state setting to RX_EXPIRED");
				} else if (this.isEnabled && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)== 0)){
					rxContext.setState(rxLacpDisabledState);
					LOG.debug("portRxStateMachine setting port={} to rxLacpDisabledState",portId);
				}
				break;
			default:    
				break;
			}
		}
		
		if ((rxContext.getState().getStateFlag() != lastState) || (lacpdu!=null) || (timerExpiredFlag == true)) {
			LOG.debug("portRxStateMachine - calling executeStateAction method");
			rxContext.getState().executeStateAction(rxContext, this,lacpdu);
			slaveGetBond().setDirty(true);
		}
		
		LOG.debug("RX Machine Port=" + this.portId + " lastState=" + lastState + " currentState=" + rxContext.getState().getStateFlag() + " PDU = "+ lacpdu);
		LOG.debug("Exiting portRxStateMachine for port={}, {}",swId, portId);
	}



	void portPeriodicStateMachine(LacpBpduInfo lacpdu,TimerExpiryMessage timerExpired)
	{
	
		LOG.debug("Entering portPeriodicStateMachine for port={}, {}",swId, portId);
		LacpConst.PERIODIC_STATES lastState;
		// keep current state machine state to compare later if it was changed
		lastState = periodicTxContext.getState().getStateFlag();
		
		if ((((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0) || ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)==0)) ||
				(((this.getActorOperPortState() & LacpConst.PORT_STATE_LACP_ACTIVITY)==0) && 
						((this.partnerOper.portState & LacpConst.PORT_STATE_LACP_ACTIVITY)==0))) 
		{
			periodicTxContext.setState(periodicTxNoPeriodicState);
			LOG.debug("portPeriodicStateMachine setting port={} to periodicTxNoPeriodicState",portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.FAST_PERIODIC) &&
						(this.partnerOper.getPortState() & LacpConst.LONG_TIMEOUT)==0){
			periodicTxContext.setState(periodicTxSlowState);
			LOG.debug("portPeriodicStateMachine setting port={} to periodicTxSlowState",portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.FAST_PERIODIC) &&
				((timerExpired != null) && (timerExpired.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER))){
			periodicTxContext.setState(periodicTxPeriodicState);
			LOG.debug("1. portPeriodicStateMachine setting port={} to periodicTxPeriodicState",portId);
		}
		else if ((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.SLOW_PERIODIC) &&
						((this.partnerOper.getPortState() & LacpConst.SHORT_TIMEOUT)==0) || 
								(((timerExpired != null) && (timerExpired.getTimerWheelType() == Utils.timerWheeltype.PERIODIC_TIMER)))){
			periodicTxContext.setState(periodicTxPeriodicState);
			LOG.debug("2. portPeriodicStateMachine setting port={} to periodicTxPeriodicState",portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.PERIODIC_TX) &&
				((this.partnerOper.getPortState() & LacpConst.SHORT_TIMEOUT)==0)){
			periodicTxContext.setState(periodicTxFastState);
			LOG.debug("portPeriodicStateMachine setting port={} to periodicTxFastState",portId);
		}else if((periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.PERIODIC_TX) &&
				((this.partnerOper.getPortState() & LacpConst.LONG_TIMEOUT)==0)){
			periodicTxContext.setState(periodicTxSlowState);
			LOG.debug("portPeriodicStateMachine setting port={} to periodicTxSlowState",portId);
		}
		LOG.debug("portPeriodicStateMachine - calling executeStateAction method and the stateFlag is={}", periodicTxContext.getState().getStateFlag());
		if(lastState != periodicTxContext.getState().getStateFlag()){
		LOG.debug("lastState != currentState for port={}, lastState is={} and the current State is={}", portId,lastState, periodicTxContext.getState().getStateFlag());
			periodicTxContext.getState().executeStateAction(periodicTxContext,this,lacpdu);
			slaveGetBond().setDirty(true);
  		}else{
		LOG.debug("lastState == currentState for port={}, lastState is={} and the current State is={}", portId,lastState, periodicTxContext.getState().getStateFlag());
		}
		LOG.debug("Exiting portPeriodicStateMachine for port={}, {}",swId, portId);
	}
	
	void portSelectionLogic()
	{

		LOG.debug("Entering portSelectionLogic for port={}, {}",swId, portId);

		LacpAggregator freeAgg = null;
		LacpAggregator tempAgg = null;
		LacpAggregator foundAgg = null;
		
		if ((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0){
			LOG.debug("portSelectionLogic is returing without further processing as the port={} is already PORT_SELECTED",portId);
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
					LOG.info(
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
                                this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_AGG_RESELECT));
			} else if (tempAgg.aggHasStandbyPort(this)) {
				tempAgg.rmPortFromAggStandBy(this);
				this.setPortAggregator(null);
				this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & (~LacpConst.PORT_STANDBY)));
				this.setActorPortAggregatorIdentifier((short)0);					
                                this.setStateMachineBitSet((short)(this.getStateMachineBitSet() | LacpConst.PORT_AGG_RESELECT));
			}
		}
		slaveGetBond().setDirty(true);
		if (foundAgg != null ) {
		      if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0) && ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) == 0)) {
		      		if (foundAgg.IsPortReachMaxCount(this)) {
							LOG.info(
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
					LOG.info(
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
					LOG.info(
							"Port[{}] joins in new Aggregator[ID={}]",
							HexEncode.longToHexString((long)this.portId),
							HexEncode.longToHexString((long)this.getActorPortAggregatorIdentifier()));
				} else {
					LOG.info(
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
				LOG.info(
						"Port[{}] joins in new Aggregator[ID={}] ",
						HexEncode.longToHexString((long)this.portId),
								HexEncode.longToHexString((long)this.getActorPortAggregatorIdentifier()));
			} else {
				LOG.info(
						"Port[{}] can't find suitable aggregator",
						 HexEncode.longToHexString((long)this.portId));
				return;
			}						
		}

		this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());

		if ((isInitialized) && slaveGetBond()!=null){
			LOG.info("portSelectionLogic calling bondAggSelectionLogic for port={}",portId);
			slaveGetBond().bondAggSelectionLogic();
		}
		LOG.debug("Exiting portSelectionLogic for port={}, {}",swId, portId);
	}
	
	
	void portMuxStateMachine(TimerExpiryMessage timerExpired){
		
		LOG.debug("Entering portMuxStateMachine for port={}, {}",swId, portId);

		LacpConst.MUX_STATES lastState;
		lastState = muxContext.getState().getStateFlag();
		
		if((this.getStateMachineBitSet() & LacpConst.PORT_BEGIN) > 0){
			muxContext.setState(muxDetachedState);
			LOG.debug("portMuxStateMachine setting port={} to muxDetachedState",portId);
		}
		else {
			if((muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_DETACHED)){
				if((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED)>0 || (this.getStateMachineBitSet() & LacpConst.PORT_STANDBY)>0){
					muxContext.setState(muxWaitingState);
					LOG.debug("portMuxStateMachine setting port={} to muxWaitingState",portId);
				}
			}
			else if((muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_WAITING)){
				
				if((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0){
					this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~(LacpConst.PORT_READY_N)));
					this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
					muxContext.setState(muxDetachedState);
					LOG.debug("portMuxStateMachine setting port={} to muxDetachedState",portId);
				}else{					
					
					if(timerExpired!= null){
						if(timerExpired.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER){
							//this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
							this.setPortsReady(1);
						}
					}
					// if the waitWhileTimer expired, and the port is in READY state, move to ATTACHED state
					if (((this.getStateMachineBitSet() & LacpConst.PORT_READY) > 0)
							&& ((timerExpired != null) && timerExpired.getTimerWheelType() == Utils.timerWheeltype.WAIT_WHILE_TIMER)) {
						muxContext.setState(muxAttachedState); 
						LOG.debug("portMuxStateMachine wait while timer expired for port={}",portId);
						LOG.debug("portMuxStateMachine setting port={} to muxAttachedState",portId);
					}
				}
			}else if (muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_ATTACHED){
			
				if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) > 0) && 
							((this.partnerOper.portState & LacpConst.PORT_STATE_SYNCHRONIZATION) > 0) ) {
					    
						muxContext.setState(muxCollectingDistributingState);
						LOG.debug("portMuxStateMachine setting port={} to muxCollectingDistributingState",portId);
					
				} else if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED) == 0) || 
								((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) > 0)) {    
						this.setStateMachineBitSet((short)(this.getStateMachineBitSet() & ~LacpConst.PORT_READY_N));

						this.getPortAggregator().setAggPortsReady(this.getPortAggregator().getAggPortsReady());
						muxContext.setState(muxDetachedState);
						LOG.debug("portMuxStateMachine setting port={} to muxDetachedState",portId);
				}
			}else if (muxContext.getState().getStateFlag() == LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING){
				if (((this.getStateMachineBitSet() & LacpConst.PORT_SELECTED)==0) || ((this.getStateMachineBitSet() & LacpConst.PORT_STANDBY) > 0) ||
						((this.partnerOper.portState & LacpConst.PORT_STATE_SYNCHRONIZATION))==0) {
					 
						muxContext.setState(muxAttachedState);
						LOG.debug("portMuxStateMachine setting port={} to muxAttachedState",portId);
						short aggrId = 0;
						
						try {
							LacpAggregator lacpAggr = this.getPortAggregator();
							aggrId = lacpAggr.getAggId();
							LOG.info("Port[{}] AGG  [ID={}, STATUS={}] will move Collecting to Attached State because SmVar= {}, Partner State={}",
							this.portId, aggrId,
							(this.getPortAggregator().getIsActive() > 0 ? "Active" : "Ready"), this.getStmStateString(this.getStateMachineBitSet()),
							this.getPortStateString((byte)this.partnerOper.portState));
						} catch (Exception e) {
							String errStr = "last_lacp_sm_state :mux_coll_dist Bad lacpAggr";
							LOG.error(errStr);
							LOG.error(e.getMessage());
						}
				} 
			}else{
				//no-op
			}
			
		}
		
		if(muxContext.getState().getStateFlag() != lastState) {
			LOG.debug("portMuxStateMachine calling executeStateAction for port={}, {}",swId, portId);
			LOG.debug("The Mux State Flag is : " + muxContext.getState().getStateFlag());
			muxContext.getState().executeStateAction(muxContext,this);
			slaveGetBond().setDirty(true);
		}
	}
		

	void portTxStateMachine(TimerExpiryMessage timerExpired)
	{
		LOG.debug("Entering portTxStateMachine for port={}, {}",swId, portId);
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
            this.updateNttForHandleBpdu();
		}

		/*
		// check if tx timer expired, to verify that we do not send more than 3 packets per second
		// this LOG.c only applies when we are operating in fast periodic time...
		// currently we operate only in slow periodic time....
		*/
	 
		// check if there is something to put on tx queue
		if ((this.isNtt()) && ((this.getStateMachineBitSet() & LacpConst.PORT_LACP_ENABLED)>0)) {
				LOG.debug("portTxStateMachine putting port={}, {} on to tx queue, setting Ntt false ",swId, portId);
				lacpduSend(qType);
				this.setNtt(false);
		}
		
		LOG.debug("Exiting portTxStateMachine for port={}, {}",swId, portId);
	}
		

	void portHandleLinkChange(byte link)
	{
				
		LOG.debug("Entering portHandleLinkChange for port={}, {}",swId, portId);
		if (link == LacpConst.BOND_LINK_UP) {
			isEnabled = true;
			LOG.info("portHandleLinkChange port={} , link is BOND_LINK_UP", portId);
			actorOperPortKey = getActorAdminPortKey();
        	} else {
			LOG.info("portHandleLinkChange port={} , link is BOND_LINK_DOWN", portId);
                	/* link has failed */
                	isEnabled = false;
                	actorOperPortKey = getActorAdminPortKey();
                 	if (this.isPortAttDist()) {
                	 	LOG.debug("Port is distribution mode for delete before sending update");
                	 	disableCollectingDistributing(this.portId,this.getPortAggregator());
                 	}	

        	}
		portSetLagId();
        	setStateMachineBitSet((short)(getStateMachineBitSet() | LacpConst.PORT_BEGIN));
		LOG.debug("Exiting portHandleLinkChange for port={}, {}",swId, portId);
	}
	
	void portAssignSlave(byte[] sysMacAddr, int lacpFast, int sysPri, int portPri, short adminKey) {
		
		LOG.debug("Entering portAssignSlave for port={}, {}",swId, portId);
		lacpInitPort(lacpFast);
		this.isInitialized = true;
		
		setActorPortNumber(portId);
		actorOperPortKey = getActorAdminPortKey();
		this.setActorSystemPriority(sysPri);
		this.setActorPortPriority(portPri);

		if (!this.isLacpEnabled()) {
			setStateMachineBitSet((short)(getStateMachineBitSet() & ~LacpConst.PORT_LACP_ENABLED));
			LOG.debug("portAssignSlave - setting port={} to PORT_LACP_ENABLED FALSE",portId);
		}
		
		setActorSystem(Arrays.copyOf(sysMacAddr, LacpConst.ETH_ADDR_LEN));
		portSetLagId();
		LOG.debug("Exiting portAssignSlave for port={}",portId);
	}
	
	public byte portGetPortStatus() {
		return (this.getLink());
	}
	
	public void portSetLagId() {
		
		LOG.debug("Entering portSetLagId for port={}",portId);
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
				LOG.debug("portSetLagId setting port={} with lagid={}", portId,lagId.toString());
		}
		else {
			LOG.debug("LagIdElement information duplicate between actor (myself) and partner : LagIdElement={}", self);
		}
		LOG.debug("Exiting portSetLagId for port={}",portId);
	}
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataService = dataBroker;
    }
    public void updateNCLacpInfo ()
    {
        if (this.resetStatus == false)
        {
            LOG.debug ("ResetStatus is disabled. not updating the ds for the port");
            return;
        }
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNodeConnector lacpNC;
        lacpNC  = lacpNCBuilder.build();
        InstanceIdentifier<LacpNodeConnector> lacpNCId = ncId.augmentation(LacpNodeConnector.class);
        write.put(LogicalDatastoreType.OPERATIONAL, lacpNCId, lacpNC);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess (Object o)
            {
                LOG.debug("LacpNodeConnector updation write success for txt {}", write.getIdentifier());
            }

            @Override
            public void onFailure (Throwable throwable)
            {
                LOG.error("LacpNodeConnector updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
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
    public void resetLacpParams()
    {
        LOG.debug ("in resetLacpParams for port {}", portId);
        short portNum = 0;
        lacpNCBuilder.setLogicalNodeconnectorRef(null);
        lacpNCBuilder.setLacpAggRef(null);
        lacpNCBuilder.setPartnerPortNumber(portNum);
        LOG.debug ("updated the reset values in the port ds");
        updateNCLacpInfo();
    }

	public MacAddress getSwitchHardwareAddress()
    {
        return this.ncMac;
    }

    public void lacpPortCleanup()
    {
        LOG.debug("in lacpPortCleanup, stopping the timer for the port {}, {}", swId, portId);

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
    public void setPortOperStatus (boolean value)
    {
        operUpStatus = value;
        return;
    }
    public boolean getPortOperStatus ()
    {
        return operUpStatus;
    }
    public void setResetStatus (boolean value)
    {
        resetStatus = value;
        return;
    }
    public PeriodicTxState getPeriodicTxState (LacpConst.PERIODIC_STATES flag)
    {
        PeriodicTxState obj = null;
        switch (flag)
        {
            case FAST_PERIODIC:
                obj = periodicTxFastState;
                break;
            case SLOW_PERIODIC:
                obj = periodicTxSlowState;
                break;
            default:
                break;
        }
        return obj;
    }
    public void updateNttForHandleBpdu ()
    {
        LOG.debug ("in updateNttForHandleBpdu for port {}, {}. Setting Ntt and restarting the periodic while timer", swId, portId);
        this.setNtt(true);
        if (periodicTxContext.getState().getStateFlag() == LacpConst.PERIODIC_STATES.FAST_PERIODIC)
        {
            this.setPeriodicWhileTimer(LacpConst.FAST_PERIODIC_TIME);
        }
        else
        {
            this.setPeriodicWhileTimer(LacpConst.SLOW_PERIODIC_TIME);
        }
        return;
    }
}
