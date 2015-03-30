/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

public class LacpUtil
{
    public static final int LACP_ETHTYPE = 34825;
    public static final MacAddress LACP_MAC = new MacAddress("01:80:c2:00:00:02");
    public static final int DEF_PERIODIC_TIME = 30;
    public static final int DEF_LACP_PRIORITY = 32768;
    private static DataBroker dataBrokerService = null;
    private static final Logger LOG = LoggerFactory.getLogger(LacpUtil.class);
    private static final String NODE_URI_PREF = "openflow:";

    private LacpUtil ()
    {
    }
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataBrokerService = dataBroker;
        return;
    }
    public static DataBroker getDataBrokerService ()
    {
        return (dataBrokerService);
    }
    public static Long getNodeSwitchId (InstanceIdentifier<Node> nodeId)
    {
        Long swId;
        NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
        String value = nId.getValue().toString();
        String num = value.replace(NODE_URI_PREF, "");
        try
        {
            swId = Long.valueOf(num);
        }
        catch (NumberFormatException e)
        {
            LOG.warn ("Unable to obtain the switch id from the node id {}, switch id {}", nodeId, num);
            swId = new Long ("-1");
        }
        return swId;
    }
}
