/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.state;


import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuxState {

	private static final Logger LOG = LoggerFactory.getLogger(MuxState.class);
	protected LacpConst.MUX_STATES stateFlag;

	MuxState(){
		LOG.debug("Entering MuxState constructor");
		setStateFlag(LacpConst.MUX_STATES.MUX_DUMMY);
		LOG.debug("Exiting MuxState constructor");
	}

	public LacpConst.MUX_STATES getStateFlag(){
		return stateFlag;
	}

	public void setStateFlag(LacpConst.MUX_STATES state){
		stateFlag = state;
	}

	public void executeStateAction(MuxContext obj, LacpPort portObjRef){

	}
}
