/*
 *  * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   * This program and the accompanying materials are made available under the
 *    * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *     * and is available at http://www.eclipse.org/legal/epl-v10.html
 *      *
 *       */

package org.opendaylight.lacp.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTxContext  {
	private static final Logger LOG = LoggerFactory.getLogger(PeriodicTxContext.class);
	private PeriodicTxState periodicTxState;
	
	
	public PeriodicTxContext(){
		LOG.debug("Entering PeriodicTxContext constructor");
		this.periodicTxState = new PeriodicTxState();
		LOG.debug("Exiting PeriodicTxContext constructor");
	}
	
	public void	setState(PeriodicTxState stateObj){
		periodicTxState = stateObj;
	}
	
	public PeriodicTxState getState(){
		return periodicTxState;
	}
}

