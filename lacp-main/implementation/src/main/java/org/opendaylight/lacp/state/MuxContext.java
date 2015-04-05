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

public class MuxContext {
	private static final Logger log = LoggerFactory.getLogger(MuxContext.class);
	private MuxState muxState;
		

	public MuxContext(){
		log.debug("Entering MuxContext constructor");
		this.muxState = new MuxState();
		log.debug("Exiting MuxContext constructor");
	}
	
	public void	setState(MuxState stateObj){
		log.debug("Entering MuxContext setState");
		muxState = stateObj;
		log.debug("Exiting MuxContext setState");
	}
	
	public MuxState getState(){
		return muxState;
	}
}
