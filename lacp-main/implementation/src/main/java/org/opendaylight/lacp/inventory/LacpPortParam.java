/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpPortParam
{
    private static final Logger log = LoggerFactory.getLogger(LacpPortParam.class);
    short portNo;
    
    public LacpPortParam (LacpPortParam param)
    {
        portNo = param.portNo;
    }
    public short getPortNumber ()
    {
        return portNo;
    }
}
