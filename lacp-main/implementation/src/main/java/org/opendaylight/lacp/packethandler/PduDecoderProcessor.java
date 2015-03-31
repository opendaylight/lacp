/*
/ * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import  java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.lacp.Utils.*;



public class PduDecoderProcessor implements Runnable {

//	private final ExecutorService RSMThrExecutor = Executors.newCachedThreadPool();
	private final static Logger log = LoggerFactory.getLogger(PduDecoderProcessor.class);
	private static boolean IsLacploaded=true;
	@Override
	public void run() {
		log.info("Spawned PDU Decoder Thread");

		PduQueueHandler qh = new PduQueueHandler();
		// Check the Raw Packet Queue for any Incoming LACP PDU to be processed
		while (IsLacploaded)
		{
			qh.checkQueue();

			//TODO : Have a check to set Unload Flag
			//IsLacpUnloaded=true;
		}
	}


	public static void setLacploaded(boolean load) {
                IsLacploaded = load;
        }

}
