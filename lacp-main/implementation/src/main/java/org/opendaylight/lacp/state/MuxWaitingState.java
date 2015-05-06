/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.state;


import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuxWaitingState  extends MuxState {

	private static final Logger LOG = LoggerFactory.getLogger(MuxWaitingState.class);
	public MuxWaitingState(){
		LOG.debug("Entering MuxWaitingState constructor");
		stateFlag = LacpConst.MUX_STATES.MUX_WAITING;
		LOG.debug("Exiting MuxWaitingState constructor");
	}
	
	public void executeStateAction(MuxContext obj, LacpPort portObjRef){
		stateFlag = LacpConst.MUX_STATES.MUX_WAITING;
		//Start wait_while_timer
		portObjRef.setWaitWhileTimer((long)LacpConst.AGGREGATE_WAIT_TIME);
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.MUX_STATES state){
		LOG.debug("Entering MuxWaitingState setState");
		stateFlag = state;
		LOG.debug("Exiting MuxWaitingState setState");
	}
}
