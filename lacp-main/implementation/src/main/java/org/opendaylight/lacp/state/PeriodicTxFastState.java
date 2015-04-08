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

public class PeriodicTxFastState extends PeriodicTxState {
	
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxFastState.class);
	public PeriodicTxFastState(){
		stateFlag = LacpConst.PERIODIC_STATES.FAST_PERIODIC;
	}
	public void executeStateAction(PeriodicTxContext obj, LacpPort portObjRef,LacpBpduInfo pdu){
		//Start periodic_timer (fast_periodic_time)
		
		stateFlag = LacpConst.PERIODIC_STATES.FAST_PERIODIC;
		//portObjRef.setPeriodicWhileTimer(LacpConst.FAST_PERIODIC_TIME);
		//overriding as we currently support only slow timeout
		System.out.println("PeriodicTxFastState - before calling setPeriodicWhileTimer with slow_periodic_time");
		portObjRef.setPeriodicWhileTimer(LacpConst.SLOW_PERIODIC_TIME);
		System.out.println("PeriodicTxFastState - after calling setPeriodicWhileTimer with slow_periodic_time");
		obj.setState(this);
	}
	
	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		stateFlag = state;
	}
}
