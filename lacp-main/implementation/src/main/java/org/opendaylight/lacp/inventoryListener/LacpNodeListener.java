/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventoryListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;

enum EventType
{
    Updated,
    Deleted;
}

public class LacpNodeListener implements OpendaylightInventoryListener
{
    private static final Logger log = LoggerFactory.getLogger(LacpNodeListener.class);
    private final ExecutorService lacpService = Executors.newCachedThreadPool();
    private LacpSystem lacpSystem;

    public LacpNodeListener (LacpSystem lacpSys)
    {
        lacpSystem = lacpSys;
    }
    @Override
    public void onNodeConnectorRemoved (NodeConnectorRemoved nodeConnectorRemoved)
    {
        if (nodeConnectorRemoved == null)
            return;
        log.info("got a node connec removed in lacp {} ", nodeConnectorRemoved);
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.Deleted, nodeConnectorRemoved));
    }

    @Override
    public void onNodeConnectorUpdated (NodeConnectorUpdated nodeConnectorUpdated)
    {
        if (nodeConnectorUpdated == null)
            return;
        log.info("got a node connec Updated {} in lacp ", nodeConnectorUpdated);
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.Updated, nodeConnectorUpdated));
    }

    @Override
    public void onNodeRemoved (NodeRemoved nodeRemoved)
    {
        if (nodeRemoved == null)
            return;
        log.info("got a node removed {} in lacp ", nodeRemoved);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeRemoved.getNodeRef().getValue();
        lacpService.submit(new LacpNodeUpdate(nodeId, EventType.Deleted));
    }

    @Override
    public void onNodeUpdated (NodeUpdated nodeUpdated)
    {
        if (nodeUpdated == null)
            return;
        log.info("got a node updated {} ", nodeUpdated);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeUpdated.getNodeRef().getValue();
        lacpService.submit(new LacpNodeUpdate(nodeId, EventType.Updated));
    }

    private class LacpNodeUpdate implements Runnable
    {
        private InstanceIdentifier<Node> lNode;
        private EventType event;

        public LacpNodeUpdate(InstanceIdentifier<Node> node, EventType evt)
        {
            lNode = node;
            event = evt;
        }
        @Override
        public void run ()
        {  
            if (event.equals(EventType.Updated) == true)
            {
                handleNodeUpdate(lNode);
            }
            else
            {
                handleNodeDeletion(lNode);
            }
        }

        private void handleNodeUpdate (InstanceIdentifier<Node> lNode)
        {
            InstanceIdentifier<Node> nodeId = lNode;
            if (lacpSystem.getLacpNode(nodeId) != null)
            {
                log.debug ("Node already notified to lacp. Ignoring it {}", nodeId);
                return;
            }
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId);
            if (lacpNode == null)
            {
                log.error("cannot add a lacp node for node {}", nodeId); 
                return;
            }
            lacpSystem.addLacpNode(nodeId, lacpNode);
        }

        private void handleNodeDeletion (InstanceIdentifier<Node> lNode)
        {
            InstanceIdentifier <Node> nodeId = lNode;
            if (lacpSystem.getLacpNode(nodeId) == null)
            {
                log.debug("Node already removed from lacp. Ignoring it {}", nodeId);
                return;
            }
            LacpNodeExtn lacpNode = lacpSystem.removeLacpNode(nodeId);
            if (lacpNode == null)
            {
                log.error("lacpNode could not be removed for node {}", nodeId); 
                return;
            }
        }
    }

    private class LacpNodeConnectorUpdate implements Runnable
    {
        private EventType event;
        private NodeConnectorUpdated ncUpdated;
        private NodeConnectorRemoved ncRemoved;

        public LacpNodeConnectorUpdate (EventType evt, NodeConnectorUpdated nc)
        {
            event = evt;
            this.ncUpdated = nc;
            this.ncRemoved = null;
        }
        public LacpNodeConnectorUpdate (EventType evt, NodeConnectorRemoved nc)
        {
            event = evt;
            this.ncRemoved = nc;
            this.ncUpdated = null;
        }

        @Override
        public void run ()
        { 
            InstanceIdentifier<NodeConnector> lNodeCon;

            if (event.equals(EventType.Updated) == true)
            {
                lNodeCon = (InstanceIdentifier<NodeConnector>)ncUpdated.getNodeConnectorRef().getValue();
                FlowCapableNodeConnectorUpdated flowConnector = ncUpdated.<FlowCapableNodeConnectorUpdated>getAugmentation(FlowCapableNodeConnectorUpdated.class);
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                   // port is in linkdown state, remove the port from the node.
                    handlePortDelete(lNodeCon, true);
                else
                    // port is in linkup state, add the port to the node.
                    handlePortUpdate(lNodeCon);
            }
            else
            {
                lNodeCon = (InstanceIdentifier<NodeConnector>)ncRemoved.getNodeConnectorRef().getValue();
                handlePortDelete(lNodeCon, false);
            }
        }
        private void handlePortUpdate(InstanceIdentifier<NodeConnector> ncId)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = lacpSystem.getLacpNode(nodeId);
            if (lacpNode != null)
            {
                if (lacpNode.addNonLacpPort (ncId) == false)
                {
                    log.debug("port already available with lacp node. Ignoring it {}", ncId);
                    return;
                }
            }
            else
            {
                log.error("got a a nodeConnector updation for non-existing node {} ", nodeId);
            }
        }
        private void handlePortDelete (InstanceIdentifier<NodeConnector> ncId, boolean hardReset)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = lacpSystem.getLacpNode(nodeId);
            if (lacpNode != null)
            {
                if (lacpNode.deletePort (ncId, hardReset) == false)
                {
                    log.debug("port not present with the lacp node. Ignoring it {}", ncId) ;
                    return;
                }
            }
            else
            {
                log.error("got a a nodeConnector removal for non-existing node {} ", nodeId);
            }
        }
    }
}
