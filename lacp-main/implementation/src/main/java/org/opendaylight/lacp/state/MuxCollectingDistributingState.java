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
	private static final Logger LOG = LoggerFactory.getLogger(MuxCollectingDistributingState.class);
	
	public MuxCollectingDistributingState(){
		stateFlag = LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING;
	}
	
	public void executeStateAction(MuxContext obj, LacpPort portObjRef){
		
	/*
    	Actor.Distributing = TRUE
	Actor.Collecting = TRUE
	Enable_Collecting_Distributing
	NTT = TRUE
	*/
		
	stateFlag = LacpConst.MUX_STATES.MUX_COLLECTING_DISTRIBUTING;
	portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_COLLECTING));
	portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
				| LacpConst.PORT_STATE_DISTRIBUTING));
	try {

		LOG.info(
			"Port[{}] moves to the collect & dist state to aggregator [ID={}, STATUS={}]",
			 String.format("%04x",portObjRef.slaveGetPortId()),
			 String.format("%04x", portObjRef.getPortAggregator().getAggId()),
			 ( portObjRef.getPortAggregator().getIsActive() > 0 ? "Active" : "Ready"));

			portObjRef.enableCollectingDistributing(portObjRef.slaveGetPortId(),portObjRef.getPortAggregator());

		}catch (Exception e) {
			LOG.error("MuxCollectingDistributingState bad lacp aggr");
			LOG.error(e.getMessage());
		}
		portObjRef.setNtt(true);
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.MUX_STATES state){
		stateFlag = state;
	}
}
