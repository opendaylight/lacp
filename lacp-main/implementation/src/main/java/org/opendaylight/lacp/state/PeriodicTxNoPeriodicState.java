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
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTxNoPeriodicState extends PeriodicTxState {

	private static final Logger log = LoggerFactory.getLogger(PeriodicTxNoPeriodicState.class);
	
	public PeriodicTxNoPeriodicState(){
		stateFlag = LacpConst.PERIODIC_STATES.NO_PERIODIC;
	}
	public void executeStateAction(PeriodicTxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		//Stop periodic_timer	
		
		stateFlag = LacpConst.PERIODIC_STATES.NO_PERIODIC;
		if(!portObjRef.getPeriodicWhileTimer().isExpired()){
			portObjRef.getPeriodicWhileTimer().cancel();
		}
		obj.setState(this);
		//UCT to fast-periodic
		obj.setState(portObjRef.periodicTxFastState);
		obj.getState().executeStateAction(obj, portObjRef);
	}
	
	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		stateFlag = state;
	}
}

