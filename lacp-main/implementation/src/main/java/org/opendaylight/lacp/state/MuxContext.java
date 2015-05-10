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
	private static final Logger LOG = LoggerFactory.getLogger(MuxContext.class);
	private MuxState muxState;
		

	public MuxContext(){
		LOG.debug("Entering MuxContext constructor");
		this.muxState = new MuxState();
		LOG.debug("Exiting MuxContext constructor");
	}
	
	public void	setState(MuxState stateObj){
		muxState = stateObj;
	}
	
	public MuxState getState(){
		return muxState;
	}
}
