/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

public interface LacpPDUPortStatusContainer {
    public enum MessageType {
        LACP_PDU_MSG,
        LACP_PORT_STATUS_MSG,
        LACP_NODE_DEL_MSG
    }

    MessageType getMessageType();
}
