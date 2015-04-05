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

public class PeriodicTxContext  {
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxContext.class);
	private PeriodicTxState periodicTxState;
	
	
	public PeriodicTxContext(){
		log.debug("Entering PeriodicTxContext constructor");
		this.periodicTxState = new PeriodicTxState();
		log.debug("Exiting PeriodicTxContext constructor");
	}
	
	public void	setState(PeriodicTxState stateObj){
		log.debug("Entering PeriodicTxContext setState");
		periodicTxState = stateObj;
		log.debug("Exiting PeriodicTxContext setState");
	}
	
	public PeriodicTxState getState(){
		return periodicTxState;
	}
}

