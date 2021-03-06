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

public class RxExpiredState extends RxState {
	private static final Logger LOG = LoggerFactory.getLogger(RxExpiredState.class);
	public RxExpiredState(){
		stateFlag = LacpConst.RX_STATES.RX_EXPIRED;
	}
	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		/*
		1. Partner_Oper_Port_State.Synchronization = FALSE
		2. Partner_Oper_Port_State.LACP_Timeout = Short Timeout
		3. start current_while_timer(Short Timeout)
		4. Actor_Oper_Port_State.Expired = TRUE
		*/


		LOG.debug("Entering RxExpiredState executeStateAction");
		stateFlag = LacpConst.RX_STATES.RX_EXPIRED;
		portObjRef.getPartnerOper().setPortState((short)(portObjRef.getPartnerOper().getPortState() & ~LacpConst.PORT_STATE_SYNCHRONIZATION));
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_MATCHED));
		portObjRef.getPartnerOper().setPortState((short)(portObjRef.getPartnerOper().getPortState() | LacpConst.PORT_STATE_LACP_ACTIVITY));
                // in expired state, starting current_while timer with the short timeout value.
		portObjRef.setCurrentWhileTimer((long)LacpConst.SHORT_TIMEOUT_TIME);
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_EXPIRED));
		obj.setState(this);
		LOG.debug("Exiting RxExpiredState executeStateAction");
	}

	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}
	public void setStateFlag(LacpConst.RX_STATES state){
		stateFlag = state;
	}
}
