/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.lacp.core.LacpConst;
import java.util.Random;
import java.math.BigInteger;

public class LacpUtil
{
    public static final int LACP_ETHTYPE = 34825;
    public static final MacAddress LACP_MAC = new MacAddress("01:80:c2:00:00:02");
    public static final int DEF_PERIODIC_TIME = 30;
    public static final int DEF_LACP_PRIORITY = 32768;
    private static DataBroker dataBrokerService = null;
    private static final Logger LOG = LoggerFactory.getLogger(LacpUtil.class);
    private static final String NODE_URI_PREF = "openflow:";
    private static SalGroupService salGroupService;
    private static final long LOG_PORT_NUM = 5000;
    private static final Random RAND_GRP_GEN = new Random();

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
        Long swId = -1L;
        NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
        String value = nId.getValue().toString();
        boolean result = value.startsWith(NODE_URI_PREF);
        if (result == false)
        {
            LOG.info ("Couldn't parse the node {}", value);
            return swId;
        }
        String num = value.replace(NODE_URI_PREF, "");
        try
        {
            swId = Long.valueOf(num);
        }
        catch (NumberFormatException e)
        {
            LOG.warn ("Unable to obtain the switch id from the node id {}, switch id {}", nodeId, num);
        }
        return swId;
    }
    public static void setSalGroupService(SalGroupService salGrpService)
    {
	salGroupService = salGrpService;
    }
    public static SalGroupService getSalGroupService()
    {
	return salGroupService;
    }
    public static long getLogPortNum ()
    {
        return LOG_PORT_NUM;
    }
    public static Long getNextGroupId()
    {
        int value = RAND_GRP_GEN.nextInt(65535);
        Long id = Long.valueOf(value);
        return id;
    }

     public static String macToString(String srcstr) {

                if(srcstr == null) {
                        return "null";
                }

                StringBuffer buf = new StringBuffer();
                for(int i = 0; i < srcstr.length(); i=i+3) {
                        buf.append(srcstr.charAt(i));
                        buf.append(srcstr.charAt(i+1));
                }
                return buf.toString();
        }

        public static String bytetoString(byte[] tb)
        {
                StringBuffer tsb = new StringBuffer();
                  for (int i=0;i<tb.length;i++) {
                          tsb.append(Integer.toHexString((int) tb[i]));
                 }
                return tsb.toString();

        }

        public static byte[] convertStringtoByte(String srcstr)
        {
                byte[] destb = new BigInteger(srcstr,16).toByteArray();
                bytetoString(destb);
                return (destb);
        }

        public static byte[] hexStringToByteArray(String s) {
                int len = s.length();
                byte[] data = null;
                if(len == 1){
                        data = new byte[1];
                        data[0] = (byte) Character.digit(s.charAt(0), 16);
                }
                else
                {
                        data = new byte[len / 2];
                        for (int i = 0; i < len; i += 2) {
                        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
                        }
                }
                return data;
        }

        public static boolean isState(short portState, byte expectedState) {
            return (portState & expectedState) == expectedState;
        }

        public static boolean isFast(short portState) {
            return isState(portState, LacpConst.PORT_STATE_LACP_TIMEOUT);
        }
}
