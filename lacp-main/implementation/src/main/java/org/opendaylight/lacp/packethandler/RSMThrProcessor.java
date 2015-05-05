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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.Lists;

public class RSMThrProcessor implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(RSMThrProcessor.class);
	private final AtomicLong txNum = new AtomicLong();

	public String getNewTransactionId() {
                return "RSM-" + txNum.getAndIncrement();
        }

	@Override
	public void run() {
		boolean IsNodeActive=true;
		log.info("Spawned RSMThrProcessor Thread");
		log.info("Spawned RSMThrProcessor Thread");
		while (IsNodeActive)
		{
			//Dequeue
			//TODO : Add condition to un-set IsNodeActive flag
			IsNodeActive=false;
		}
	}
}
