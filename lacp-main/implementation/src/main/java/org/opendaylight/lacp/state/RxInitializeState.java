/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.state;

import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RxInitializeState extends RxState {

	private static final Logger LOG = LoggerFactory.getLogger(RxInitializeState.class);

	public RxInitializeState(){
		stateFlag = LacpConst.RX_STATES.RX_INITIALIZE;
	}

	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){

		/*
		1. Selected = UNSELECTED
		2. recordDefault
		3. Actor_Oper_Port_State.Expired = FALSE
		4. port_moved = FALSE
		*/

		stateFlag = LacpConst.RX_STATES.RX_INITIALIZE;
		if (!portObjRef.isLacpEnabled()){
			portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & (~LacpConst.PORT_LACP_ENABLED)));
			LOG.debug("RxInitializeState setting lacp enabled to FALSE for port{}", portObjRef.slaveGetPortId());
		}
		else{
			portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() | LacpConst.PORT_LACP_ENABLED));
		}
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_SELECTED));
		recordDefault(portObjRef);
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				& ~LacpConst.PORT_STATE_EXPIRED));
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_MOVED));
		obj.setState(this);
		//UCT to next state
		obj.setState(portObjRef.rxPortDisabledState);
		obj.getState().executeStateAction(obj, portObjRef, pdu);
	}

	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}

	public void setStateFlag(LacpConst.RX_STATES state){
		stateFlag = state;  //next state

	}
}

