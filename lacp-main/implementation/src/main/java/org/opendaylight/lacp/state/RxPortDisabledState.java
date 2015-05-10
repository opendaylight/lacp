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

public class RxPortDisabledState extends RxState {
	private static final Logger LOG = LoggerFactory.getLogger(RxPortDisabledState.class);
	public RxPortDisabledState(){
		stateFlag = LacpConst.RX_STATES.RX_PORT_DISABLED;
	}
	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		LOG.debug("Entering RxPortDisabledState executeStateAction method");
		//Partner_Oper_Port_State.Synchronization = FALSE
		stateFlag = LacpConst.RX_STATES.RX_PORT_DISABLED;
		portObjRef.setStateMachineBitSet((short)(portObjRef.getStateMachineBitSet() & ~LacpConst.PORT_MATCHED));
		portObjRef.getPartnerOper().setPortState((short)((portObjRef.getPartnerOper().getPortState()) & (~LacpConst.PORT_STATE_SYNCHRONIZATION )));
		obj.setState(this);
		LOG.debug("Exiting RxPortDisabledState executeStateAction method");
	}
	
	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.RX_STATES state){
		stateFlag = state;
	}

}

