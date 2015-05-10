/*
/ * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class RSMThrProcessor implements Runnable {

	private final static Logger LOG = LoggerFactory.getLogger(RSMThrProcessor.class);
	private final AtomicLong txNum = new AtomicLong();

	public String getNewTransactionId() {
                return "RSM-" + txNum.getAndIncrement();
        }

	@Override
	public void run() {
		boolean isNodeActive=true;
		LOG.info("Spawned RSMThrProcessor Thread");
		LOG.info("Spawned RSMThrProcessor Thread");
		while (isNodeActive)
		{
			//Dequeue
			//TODO : Add condition to un-set isNodeActive flag
			isNodeActive=false;
		}
	}
}
