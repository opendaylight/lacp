/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.state;

import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.util.LacpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTxPeriodicState extends PeriodicTxState {

	private static final Logger LOG = LoggerFactory.getLogger(PeriodicTxPeriodicState.class);

	public PeriodicTxPeriodicState(){
		stateFlag = LacpConst.PERIODIC_STATES.PERIODIC_TX;
	}

	public void executeStateAction(PeriodicTxContext obj, LacpPort portObjRef,LacpBpduInfo pdu)
    {
	    LacpConst.PERIODIC_STATES flag = LacpConst.PERIODIC_STATES.SLOW_PERIODIC;

		LOG.debug("Entering PeriodicTxPeriodicState executeStateAction, setting ntt to true for port={}", portObjRef.slaveGetPortId());
		if(!portObjRef.getPeriodicWhileTimer().isExpired()){
			portObjRef.getPeriodicWhileTimer().cancel();
		}
		portObjRef.setNtt(true);
	if (!LacpUtil.isFast(portObjRef.portPartnerOperGetPortState()))
        {
            flag = LacpConst.PERIODIC_STATES.SLOW_PERIODIC;
        }
        else
        {
            flag = LacpConst.PERIODIC_STATES.FAST_PERIODIC;
        }
        obj.setState(portObjRef.getPeriodicTxState(flag));
        obj.getState().executeStateAction(obj, portObjRef, pdu);
	}

	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}

	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		stateFlag = state;
	}
}
