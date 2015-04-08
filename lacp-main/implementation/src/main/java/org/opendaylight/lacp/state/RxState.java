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

public class RxState {
	
	private static final Logger log = LoggerFactory.getLogger(RxState.class);
	protected LacpConst.RX_STATES stateFlag;
	
	RxState(){
		log.debug("Entering RxState constructor");
		setStateFlag(LacpConst.RX_STATES.RX_DUMMY);
		log.debug("Exiting RxState constructor");
	}
	
	public LacpConst.RX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.RX_STATES state){
		log.debug("Entering RxState setStateFlag");
		stateFlag = state;
		log.debug("Exiting RxState setStateFlag");
	}

	public void executeStateAction(RxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
				
	}

	public void recordDefault(LacpPort portObjRef){
		log.info("Entering RxState recordDefault for port={}", portObjRef.slaveGetPortId());
		portObjRef.getPartnerOper().setValue(portObjRef.getPartnerAdmin());
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_DEFAULTED));
		portObjRef.portSetLagId();
		log.info("Exiting RxState recordDefault for port={}", portObjRef.slaveGetPortId());
	}
}

