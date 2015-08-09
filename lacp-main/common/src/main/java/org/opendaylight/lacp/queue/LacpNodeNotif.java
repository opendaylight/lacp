/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;

public class LacpNodeNotif implements LacpPDUPortStatusContainer
{
    public MessageType getMessageType()
    {
        return LacpPDUPortStatusContainer.MessageType.LACP_NODE_DEL_MSG;
    }
}

