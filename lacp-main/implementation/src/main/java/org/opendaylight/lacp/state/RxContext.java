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

public class RxContext {
	private static final Logger log = LoggerFactory.getLogger(RxContext.class);
	private RxState rxState;
			
	public RxContext(){
		this.rxState = new RxState();
	}
	
	public void	setState(RxState stateObj){
		rxState = stateObj;
	}
	
	public RxState getState(){
		return rxState;
	}

}

