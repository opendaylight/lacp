/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.state;


import java.util.Arrays;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpPort.PortParams;
import org.opendaylight.lacp.core.LacpBpduInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.Utils.*;


public class RxCurrentState extends RxState {

	private static final Logger LOG = LoggerFactory.getLogger(RxCurrentState.class);

	public RxCurrentState(){
		stateFlag = LacpConst.RX_STATES.RX_CURRENT;
	}

	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		/*
		update_Selected
		update_NTT
		recordPDU
		start current_while_timer (Actor_Oper_Port_State.LACP_Timeout)
		Actor_Oper_Port_State.Expired = FALSE
		*/
		stateFlag = LacpConst.RX_STATES.RX_CURRENT;
		updateSelected(portObjRef,pdu);
		updateNTT(portObjRef,pdu);
		recordPDU(portObjRef,pdu);
		//CHECK_LATER TIMEOUT VALUE - SHOULD BE Actor_Oper_Port_State.LACP_Timeout
		portObjRef.setCurrentWhileTimer((long)LacpConst.LONG_TIMEOUT_TIME);
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				& ~LacpConst.PORT_STATE_EXPIRED));
		obj.setState(this);
		LOG.debug("Exiting RxCurrentState executeStateAction method");
	}

	void updateSelected(LacpPort portObjRef,LacpBpduInfo lacpdu)
	{
		if (lacpdu!=null) {
			final PortParams partner = portObjRef.getPartnerOper();

			if (lacpdu.getActorSystemInfo().getNodePortNum() != partner.getPortNumber() ||
					lacpdu.getActorSystemInfo().getNodePortPriority() != partner.getPortPriority() ||
					!Arrays.equals(lacpdu.getActorSystemInfo().getNodeSysAddr(), partner.getSystem()) ||
					lacpdu.getActorSystemInfo().getNodeSysPri() != partner.getSystemPriority() ||
					lacpdu.getActorSystemInfo().getNodeKey() != partner.getKey() ||
					(lacpdu.getActorSystemInfo().getNodePortState() & LacpConst.PORT_STATE_AGGREGATION) !=
									(partner.getPortState() & LacpConst.PORT_STATE_AGGREGATION)) {

				portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_SELECTED));
				LOG.info("RxCurrentState updateSelected one or more values do not match, selected is set to UNSELECTED for port={}",portObjRef.slaveGetPortId());
			}
		}
	}

	void updateNTT(LacpPort portObjRef,LacpBpduInfo lacpdu)
	{
		if (lacpdu!=null) {
			if ((lacpdu.getPartnerSystemInfo().getNodePortNum() != portObjRef.getActorPortNumber()) ||
				(lacpdu.getPartnerSystemInfo().getNodePortPriority() != portObjRef.getActorPortPriority()) ||
				(!Arrays.equals(lacpdu.getPartnerSystemInfo().getNodeSysAddr(), portObjRef.getActorSystem())) ||
				(lacpdu.getPartnerSystemInfo().getNodeSysPri() != portObjRef.getActorSystemPriority()) ||
				(lacpdu.getPartnerSystemInfo().getNodeKey() != portObjRef.getActorOperPortKey()) ||
				((lacpdu.getPartnerSystemInfo().getNodePortState() & LacpConst.PORT_STATE_LACP_ACTIVITY) !=
						(portObjRef.getActorOperPortState() & LacpConst.PORT_STATE_LACP_ACTIVITY)) ||
				((lacpdu.getPartnerSystemInfo().getNodePortState() & LacpConst.PORT_STATE_LACP_TIMEOUT) !=
						(portObjRef.getActorOperPortState() & LacpConst.PORT_STATE_LACP_TIMEOUT)) ||
				((lacpdu.getPartnerSystemInfo().getNodePortState() & LacpConst.PORT_STATE_SYNCHRONIZATION) !=
						(portObjRef.getActorOperPortState() & LacpConst.PORT_STATE_SYNCHRONIZATION)) ||
				((lacpdu.getPartnerSystemInfo().getNodePortState() & LacpConst.PORT_STATE_AGGREGATION) !=
						(portObjRef.getActorOperPortState() & LacpConst.PORT_STATE_AGGREGATION)))
				{
					portObjRef.setNtt(true);
					LOG.info("RxCurrentState updateNTT one or more values do not match, settting ntt to true for port={}",portObjRef.slaveGetPortId());
				}

		}
	}

	void recordPDU(LacpPort portObjRef,LacpBpduInfo lacpdu)
	{
		if (lacpdu!=null) {
			PortParams partner = portObjRef.getPartnerOper();

			chooseMatched(portObjRef,lacpdu);
			partner.setPortNumber(lacpdu.getActorSystemInfo().getNodePortNum());
			partner.setPortPriority(lacpdu.getActorSystemInfo().getNodePortPriority());
			partner.setSystem(Arrays.copyOf(lacpdu.getActorSystemInfo().getNodeSysAddr(), LacpConst.ETH_ADDR_LEN));
			partner.setSystemPriority(lacpdu.getActorSystemInfo().getNodeSysPri());
			partner.setKey(lacpdu.getActorSystemInfo().getNodeKey());
                        short pstate = LacpPort.toUnsigned(lacpdu.getActorSystemInfo().getNodePortState());
			partner.setPortState(pstate);
			portObjRef.portSetLagId();
			portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState() & ~LacpConst.PORT_STATE_DEFAULTED));

			if (((portObjRef.getStateMachineBitSet() & LacpConst.PORT_MATCHED)>0)
					&& ((lacpdu.getActorSystemInfo().getNodePortState() & LacpConst.PORT_STATE_SYNCHRONIZATION)>0)){

				partner.setPortState((short)(partner.getPortState() | LacpConst.PORT_STATE_SYNCHRONIZATION));
				LOG.info("Setting partner SYNC to true for port={}",portObjRef.slaveGetPortId());
			}
			else{
				partner.setPortState((short)(partner.getPortState() & ~LacpConst.PORT_STATE_SYNCHRONIZATION));
				LOG.info("Setting partner SYNC to false for port={}",portObjRef.slaveGetPortId());
			}
		}
	}

	void chooseMatched(LacpPort portObjRef,LacpBpduInfo lacpdu)
	{

		if ((((lacpdu.getPartnerSystemInfo().getNodePortNum() == portObjRef.getActorPortNumber()) &&
			(lacpdu.getPartnerSystemInfo().getNodePortPriority() == portObjRef.getActorPortPriority()) &&
			Arrays.equals(lacpdu.getPartnerSystemInfo().getNodeSysAddr(), portObjRef.getActorSystem()) &&
			(lacpdu.getPartnerSystemInfo().getNodeSysPri() == portObjRef.getActorSystemPriority()) &&
			(lacpdu.getPartnerSystemInfo().getNodeKey()) == portObjRef.getActorOperPortKey()) &&
			((lacpdu.getPartnerSystemInfo().getNodePortState() & LacpConst.PORT_STATE_AGGREGATION) == (portObjRef.getActorOperPortState() & LacpConst.PORT_STATE_AGGREGATION))) ||
			((lacpdu.getActorSystemInfo().getNodePortState() & LacpConst.PORT_STATE_AGGREGATION) == 0)
				) {
			portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() | LacpConst.PORT_MATCHED));
			LOG.info("RxCurrentState chooseMatched Moving port id={} state to MATCHED",portObjRef.slaveGetPortId());
		} else {
			portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_MATCHED));

				LOG.info(
						"Matched BPDU Mismatch [Port={},POPRI={},ADDR={},SYSPRI={},KEY={},STATE={}], MY [Port={},POPRI={},ADDR={},SYSPRI={},KEY={},STATE={}]",
						new Object[] { String.format("%04x",lacpdu.getPartnerSystemInfo().getNodePortNum()),
								String.format("%04x",lacpdu.getPartnerSystemInfo().getNodePortPriority()),
								HexEncode.bytesToHexString(lacpdu.getPartnerSystemInfo().getNodeSysAddr()),
								String.format("%04x",lacpdu.getPartnerSystemInfo().getNodeSysPri()),
								String.format("%04x",lacpdu.getPartnerSystemInfo().getNodeKey()),
								portObjRef.getPortStateString(lacpdu.getPartnerSystemInfo().getNodePortState()),
								String.format("%04x",portObjRef.getActorPortNumber()),
								String.format("%04x",portObjRef.getActorPortPriority()),
								HexEncode.bytesToHexString(portObjRef.getActorSystem()),
								String.format("%04x",portObjRef.getActorSystemPriority()),
								String.format("%04x",portObjRef.getActorOperPortKey()),
								portObjRef.getPortStateString(portObjRef.getActorOperPortState())
								});



		}
	}


	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}

	public void setStateFlag(LacpConst.RX_STATES state){
		stateFlag = state;
	}
}
