/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.state;

import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RxLacpDisabledState extends RxState {
	
	private static final Logger LOG = LoggerFactory.getLogger(RxLacpDisabledState.class);
	public RxLacpDisabledState(){
		stateFlag = LacpConst.RX_STATES.RX_LACP_DISABLED;
	}
	
	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		/*
		1. Selected = UNSELECTED
		2. recordDefault
		3. Partner_Oper_Port_State.Aggregation = FALSE
		4. Actor_Oper_Port_State.Expired = FALSE
		*/
		
		stateFlag = LacpConst.RX_STATES.RX_LACP_DISABLED;
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_SELECTED));
		recordDefault(portObjRef);
		portObjRef.getPartnerOper().setPortState((short)((portObjRef.getPartnerOper().getPortState()) & (~LacpConst.PORT_STATE_AGGREGATION)));
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() | LacpConst.PORT_MATCHED));
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				& ~LacpConst.PORT_STATE_EXPIRED));
		obj.setState(this);
	}

	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.RX_STATES state){
		stateFlag = state;
	}
}

