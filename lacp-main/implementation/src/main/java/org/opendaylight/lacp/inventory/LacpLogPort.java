/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUniBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpBond;
import org.opendaylight.lacp.Utils.LacpPortProperties;
import org.opendaylight.lacp.Utils.HexEncode;

public class LacpLogPort
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpLogPort.class);
    private static NotificationProviderService notify;

    public static void setNotificationService (NotificationProviderService notificationService)
    {
        notify = notificationService;
    }
    public static void createLogicalPort (LacpBond lacpBond)
    {
        LacpNodeExtn lacpNode = lacpBond.getLacpNode();
        InstanceIdentifier<Node> nodeId = lacpNode.getNodeId();

        NodeId nId = nodeId.firstKeyOf(Node.class, NodeKey.class).getId();
        String nodePath = nId.getValue().toString();
        long portNum = LacpUtil.getLogPortNum() + lacpBond.getBondInstanceId();
        String portPath = nodePath + ":" + portNum;
        NodeConnectorId portId = new NodeConnectorId(portPath);
        InstanceIdentifier<NodeConnector> id = InstanceIdentifier.builder(Nodes.class)
                                                 .child (Node.class, new NodeKey (nId))
                                                 .child (NodeConnector.class, new NodeConnectorKey (portId)).toInstance();
        NodeConnectorRef ncRef = new NodeConnectorRef(id);

        List<LacpPort> portList = lacpBond.getActivePortList();
        LacpPort lPort = portList.get(0);

        NodeConnector nc = LacpPortProperties.getNodeConnector(LacpUtil.getDataBrokerService(), lPort.getNodeConnectorId());
        FlowCapableNodeConnector flowNC = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);

        FlowCapableNodeConnectorUpdatedBuilder flowCap = new FlowCapableNodeConnectorUpdatedBuilder()
                                                         .setConfiguration(flowNC.getConfiguration())
                                                         .setCurrentFeature(flowNC.getCurrentFeature())
                                                         .setCurrentSpeed(flowNC.getCurrentSpeed())
                                                         .setHardwareAddress(new MacAddress(HexEncode.bytesToHexStringFormat(lacpBond.getSysMacAddr())))
                                                         .setMaximumSpeed(flowNC.getMaximumSpeed())
                                                         .setName("bond"+lacpBond.getBondInstanceId())
                                                         .setPortNumber(PortNumberUniBuilder.getDefaultInstance(Long.valueOf(portNum).toString()))
                                                         .setState(new StateBuilder().setLinkDown(false).setBlocked(false).setLive(false).build());

        NodeConnectorUpdatedBuilder builder = new NodeConnectorUpdatedBuilder()
                                                  .setId(portId)
                                                  .setNodeConnectorRef(ncRef)
                                                  .addAugmentation(FlowCapableNodeConnectorUpdated.class, flowCap.build());
        notify.publish(builder.build());
        LOG.info ("Notified logical port {} created for aggregator {}", id, lacpBond.getBondInstanceId());

        LOG.debug ("setting the logical port reference for aggregator {}", lacpBond.getBondInstanceId());
        lacpBond.setLogicalNCRef(ncRef);
        for (LacpPort port : portList)
        {
            LOG.debug ("setting the logical port reference to the port {}", port.getNodeConnectorId());
            port.setLogicalNCRef(ncRef);
        }
        return;
    }

    public static void deleteLogicalPort (LacpBond lacpBond)
    {
        LacpNodeExtn lacpNode = lacpBond.getLacpNode();
        if (lacpNode.getLacpNodeDeleteStatus() == true)
        {
            LOG.debug ("Node for {} is deleted. Skipping the removal of the logical port {}", lacpNode.getNodeId(), lacpBond.getBondInstanceId());
            return;
        }

        NodeConnectorRemovedBuilder builder = new NodeConnectorRemovedBuilder()
                                                .setNodeConnectorRef(lacpBond.getLogicalNCRef());
        notify.publish(builder.build());
        LOG.info ("Revoked logical port {} created for aggregator {}", lacpBond.getLogicalNCRef(), lacpBond.getBondInstanceId());
        return;
    }
}
