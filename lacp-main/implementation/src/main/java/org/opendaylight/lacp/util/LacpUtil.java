/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

public class LacpUtil
{
    public static final int LACP_ETHTYPE = 34825;
    public static final MacAddress LACP_MAC = new MacAddress("01:80:c2:00:00:02");
    public static final int DEF_PERIODIC_TIME = 30;
    public static final int DEF_LACP_PRIORITY = 32768;
}
