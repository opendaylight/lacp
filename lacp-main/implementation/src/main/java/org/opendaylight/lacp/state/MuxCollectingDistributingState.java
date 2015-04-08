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


public class MuxCollectingDistributingState  extends MuxState {
	private static final Logger log = LoggerFactory.getLogger(MuxCollectingDistributingState.class);
	
	public MuxCollectingDistributingState(){
		log.debug("Entering MuxCollectingDistributingState constructor");
		stateFlag = LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING;
		log.debug("Exiting MuxCollectingDistributingState constructor");
	}
	
	public void executeStateAction(MuxContext obj, LacpPort portObjRef){
		
	/*
    	Actor.Distributing = TRUE
	Actor.Collecting = TRUE
	Enable_Collecting_Distributing
	NTT = TRUE
	*/
		
	log.info("Entering MuxCollectingDistributingState executeStateAction");
		
	stateFlag = LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING;
	portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_COLLECTING));
	portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_DISTRIBUTING));
	try {

		log.info(
			"Port[{}] moves to the collect & dist state to aggregator [ID={}, STATUS={}]",
			 String.format("%04x",portObjRef.slaveGetPortId()),
			 String.format("%04x", portObjRef.getPortAggregator().getAggId()),
			 ( portObjRef.getPortAggregator().getIsActive() > 0 ? "Active" : "Ready"));

			portObjRef.enableCollectingDistributing(portObjRef.slaveGetPortId(),portObjRef.getPortAggregator());

		}catch (Exception e) {
			log.error("MuxCollectingDistributingState bad lacp aggr");
			e.printStackTrace();
		}
		portObjRef.setNtt(true);
		log.info("Exiting MuxCollectingDistributingState executeStateAction");
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.MUX_STATES state){
		log.debug("Entering MuxCollectingDistributingState setStateFlag");
		stateFlag = state;
		log.debug("Exiting MuxCollectingDistributingState setStateFlag");
		
	}
}
