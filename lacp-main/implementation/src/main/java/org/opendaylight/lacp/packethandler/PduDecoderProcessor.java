/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.packethandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.Utils.*;



public class PduDecoderProcessor implements Runnable {

	private final static Logger LOG = LoggerFactory.getLogger(PduDecoderProcessor.class);
	private static boolean IsLacploaded=true;
	@Override
	public void run() {
		boolean isNewNode=false;
		LOG.info("Spawned PDU Decoder Thread");

		PduQueueHandler qh = new PduQueueHandler();
		// Check the Raw Packet Queue for any Incoming LACP PDU to be processed
		while (IsLacploaded)
		{
			qh.checkQueue();
		}
	}


	public static void setLacploaded(boolean load) {
                IsLacploaded = load;
        }

}
