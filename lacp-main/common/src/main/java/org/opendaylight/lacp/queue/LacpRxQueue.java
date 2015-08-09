/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

public  class LacpRxQueue {
    private static final LacpQueue<PacketReceived> LACP_RX_QUEUE_ID = new LacpQueue<PacketReceived>();

    protected LacpRxQueue(){
    }

    /*
     * The LacpTxQueue is a singleton class.
     */
    public static LacpQueue<PacketReceived> getLacpRxQueueId(){
	return LACP_RX_QUEUE_ID;
    }

}
