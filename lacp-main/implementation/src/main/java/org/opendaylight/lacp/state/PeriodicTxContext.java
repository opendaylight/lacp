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

public class PeriodicTxContext  {
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxContext.class);
	private PeriodicTxState periodicTxState;
	
	
	public PeriodicTxContext(){
		this.periodicTxState = new PeriodicTxState();
	}
	
	public void	setState(PeriodicTxState stateObj){
		periodicTxState = stateObj;
	}
	
	public PeriodicTxState getState(){
		return periodicTxState;
	}
}

