/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.state;


import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuxWaitingState  extends MuxState {

	private static final Logger log = LoggerFactory.getLogger(MuxWaitingState.class);
	public MuxWaitingState(){
		log.debug("Entering MuxWaitingState constructor");
		stateFlag = LacpConst.MUX_STATES.MUX_WAITING;
		log.debug("Exiting MuxWaitingState constructor");
	}
	
	public void executeStateAction(MuxContext obj, LacpPort portObjRef){
		stateFlag = LacpConst.MUX_STATES.MUX_WAITING;
		log.info("Entering MuxWaitingState executeStateAction");
		System.out.println("Entering MuxWaitingState executeStateAction");
		log.info("setting Agg wait timer for port={}", portObjRef.slaveGetPortId());
		//Start wait_while_timer
		System.out.println("Setting wait while timer for port = " + portObjRef.slaveGetPortId());
		portObjRef.setWaitWhileTimer((long)LacpConst.AGGREGATE_WAIT_TIME);
		System.out.println("After registering setWaitWhileTimer for port = " + portObjRef.slaveGetPortId());
		log.info("After registering setWaitWhileTimer for port ={}" , portObjRef.slaveGetPortId());
		log.info("Exiting MuxWaitingState executeStateAction,setting Agg wait timer for port={}", portObjRef.slaveGetPortId());
		System.out.println("Exiting MuxWaitingState executeStateAction");
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.MUX_STATES state){
		log.debug("Entering MuxWaitingState setState");
		stateFlag = state;
		log.debug("Exiting MuxWaitingState setState");
	}
}
