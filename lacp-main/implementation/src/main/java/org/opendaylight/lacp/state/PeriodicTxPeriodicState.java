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
import org.opendaylight.lacp.core.LacpPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTxPeriodicState extends PeriodicTxState {
	
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxPeriodicState.class);

	public PeriodicTxPeriodicState(){
		log.debug("Entering PeriodicTxPeriodicState constructor");
		stateFlag = LacpConst.PERIODIC_STATES.PERIODIC_TX;
		log.debug("Exiting PeriodicTxPeriodicState constructor");
	}
	
	public void executeStateAction(PeriodicTxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		//NTT = TRUE
		stateFlag = LacpConst.PERIODIC_STATES.PERIODIC_TX;
		log.debug("Entering PeriodicTxPeriodicState executeStateAction, setting ntt to true for port={}",
		portObjRef.slaveGetPortId());
		if(!portObjRef.getPeriodicWhileTimer().isExpired()){
			portObjRef.getPeriodicWhileTimer().cancel();
		}
		portObjRef.setNtt(true);
		log.debug("Exiting PeriodicTxPeriodicState executeStateActionto");
	}
	
	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		log.debug("Entering PeriodicTxPeriodicState setStateFlag");
		stateFlag = state;
		log.debug("Exiting PeriodicTxPeriodicState setStateFlag");
	}
}
