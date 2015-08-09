/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

public class LacpPortInfo
{
    private long swId;
    private int portId;

    public LacpPortInfo (long switchId, int port)
    {
        this.swId = switchId;
        this.portId = port;
    }

    public long getSwitchId()
    {
        return swId;
    }
    public int getPortId()
    {
        return portId;
    }
}

