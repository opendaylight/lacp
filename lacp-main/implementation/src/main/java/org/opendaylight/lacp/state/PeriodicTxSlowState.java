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

public class PeriodicTxSlowState extends PeriodicTxState {
	
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxSlowState.class);
	public PeriodicTxSlowState(){
		log.debug("Entering PeriodicTxSlowState constructor");
		stateFlag = LacpConst.PERIODIC_STATES.SLOW_PERIODIC;
		log.debug("Exiting PeriodicTxSlowState constructor");
	}
	public void executeStateAction(PeriodicTxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		//Start periodic_timer (slow_periodic_time)

		log.debug("Entering PeriodicTxSlowState executeStateAction, setting periodic while timer for port={} with slow periodic time",portObjRef.slaveGetPortId());
			
		
		stateFlag = LacpConst.PERIODIC_STATES.SLOW_PERIODIC;
		portObjRef.setPeriodicWhileTimer(LacpConst.SLOW_PERIODIC_TIME);
		obj.setState(this);
		log.debug("Exiting PeriodicTxSlowState executeStateAction");
	}
	
	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		log.debug("Entering PeriodicTxSlowState setStateFlag");
		stateFlag = state;
		log.debug("Exiting PeriodicTxSlowState setStateFlag");
	}

}
