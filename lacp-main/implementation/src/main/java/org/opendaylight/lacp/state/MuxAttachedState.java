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

public class MuxAttachedState extends MuxState {

	private static final Logger LOG = LoggerFactory.getLogger(MuxAttachedState.class);
	public MuxAttachedState(){
		LOG.debug("Entering MuxAttachedState constructor");
		stateFlag = LacpConst.MUX_STATES.MUX_ATTACHED;
		LOG.debug("Exiting MuxAttachedState constructor");
	}
	
	public void executeStateAction(MuxContext obj, LacpPort portObjRef){
		
	/*
 	*Attach_Mux_To_Aggregator
	*Actor.Sync = TRUE
 	*Actor.Collecting = FALSE
 	*Disable_Collecting_Distributing
 	*Actor.Distributing = FALSE
 	* NTT = TRUE
 	*/
		
		stateFlag = LacpConst.MUX_STATES.MUX_ATTACHED;
		LOG.debug("MuxAttachedState-executeStateAction Entry");
		portObjRef.attachBondToAgg();
		portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState() | LacpConst.PORT_STATE_SYNCHRONIZATION));
		try {
			portObjRef.disableCollectingDistributing(portObjRef.slaveGetPortId(),portObjRef.getPortAggregator());
		
			portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
										& ~LacpConst.PORT_STATE_COLLECTING));
			portObjRef.setActorOperPortState((byte)(portObjRef.getActorOperPortState()
										& ~LacpConst.PORT_STATE_DISTRIBUTING));

  			LOG.info("Port[{}] moves to the attached state to aggregator [ID={}, STATUS={}]",
  								String.format("%04x",portObjRef.slaveGetPortId()),
  								String.format("%04x", portObjRef.getPortAggregator().getAggId()),
  								(portObjRef.getPortAggregator().getIsActive() > 0 ? "Active" : "Ready"));
		} catch (Exception e) {
			LOG.error("MuxAttachedState bad lacp aggr");
			LOG.error(e.getMessage());
		}
		portObjRef.setNtt(true);
		LOG.debug("MuxAttachedState-executeStateAction Exit");
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.MUX_STATES state){
		LOG.debug("MuxAttachedState-setStateFlag Entry");
		stateFlag = state;
		LOG.debug("MuxAttachedState-setStateFlag Exit");
		
	}
}
