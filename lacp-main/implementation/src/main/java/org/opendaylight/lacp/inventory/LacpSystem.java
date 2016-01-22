/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.lacp.packethandler.TxProcessor;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.packethandler.PduDecoderProcessor;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpNodeNotif;

public class LacpSystem
{
    private static final ConcurrentHashMap<Long, LacpNodeExtn> LACPNODE_MAP = new ConcurrentHashMap<Long, LacpNodeExtn>();
    private static final LacpSystem LACP_SYSTEM = new LacpSystem();
    private static final Logger LOG = LoggerFactory.getLogger(LacpSystem.class);
    private static final Long INVALID_SWITCHID = new Long ("-1");
    private static final ArrayList MASTER_NODE_MAP = new ArrayList<InstanceIdentifier<Node>>();
    private static final ArrayList NOTIFIED_NODE_MAP = new ArrayList<InstanceIdentifier<Node>>();

    private LacpSystem () {}

    public static LacpSystem getLacpSystem ()
    {
        return LACP_SYSTEM;
    }
    public boolean addLacpNode (InstanceIdentifier nodeId, LacpNodeExtn lacpNode)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        if (swId.equals(INVALID_SWITCHID))
        {
            LOG.warn ("Invalid node id {}, could not add the node to the lacpSystem", nodeId);
            return false;
        }
        if (LACPNODE_MAP.containsKey (swId))
        {
            LOG.warn ("Node for id {} is already present in the lacpSystem", swId);
            return false;
        }
        LACPNODE_MAP.put(swId, lacpNode);
        return true;
    }
    public LacpNodeExtn removeLacpNode (InstanceIdentifier nodeId)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        return (removeLacpNode (swId));
    }
    public LacpNodeExtn removeLacpNode (Long swId)
    {
        if (swId.equals(INVALID_SWITCHID))
        {
            LOG.warn ("Invalid switch id {}, could not remove the node to the lacpSystem", swId);
            return null;
        }
        LacpNodeExtn lacpNode = LACPNODE_MAP.remove(swId);
        lacpNode.deleteLacpNode();
        return lacpNode;
    }
    public LacpNodeExtn getLacpNode (Long switchId)
    {
        if (switchId.equals(INVALID_SWITCHID))
        {
            LOG.warn ("Invalid switch id {}, could not obtain the node to the lacpSystem", switchId);
            return null;
        }
        return (LACPNODE_MAP.get(switchId));
    }
    public LacpNodeExtn getLacpNode (InstanceIdentifier nodeId)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        return (getLacpNode (swId));
    }
    public void clearLacpNodes ()
    {
        Collection<LacpNodeExtn> nodeList = LACPNODE_MAP.values();
        LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
        for (LacpNodeExtn lacpNode : nodeList)
        {
            Long swId = lacpNode.getSwitchId();
            LacpNodeNotif nodeNotif = new LacpNodeNotif();
            LOG.debug("sending node delete msg in clear LacpNodes");
            if (pduQueue.isLacpQueuePresent(swId) == true)
            {
                if (pduQueue.enqueueAtFront(swId, nodeNotif) == false)
                {
                    LOG.warn ("Failed to enqueue node deletion message to the pduQ for node {}", swId);
                }
            }
            else
            {
                LOG.debug ("RSM thread and pduQueue are not yet created for the switch {}, deleteing the node", swId);
                this.removeLacpNode (swId);
            }
        }
        return;
    }
    public void readDataStore (InstanceIdentifier<Node> nodeId)
    {
        Node node = readNodeInfo(LacpUtil.getDataBrokerService(), nodeId);
        if(node == null) {
            LOG.debug("Node {} is not available in the datastore.", nodeId);
            return;
        }

        LOG.debug("Reading the node information.");
        Long switchId = LacpUtil.getNodeSwitchId(nodeId);
        if (switchId.equals(INVALID_SWITCHID)) {
            LOG.warn ("Node obtained {} is not an openflow enabled node. Not adding it part of lacp system", nodeId);
            return;
        }
        LacpNode lNode = node.<LacpNode>getAugmentation(LacpNode.class);
        if (lNode != null) {
            /* LacpNode updated by previous master is available.
             * Reconstruct the lacpNodeExtn and the bond information from it */
            LOG.debug ("reconstructing lacp node for node id {}", nodeId);
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId, node);
            if (lacpNode == null) {
                LOG.error("cannot add a lacp node for node {}", nodeId);
                return;
            }
            LOG.debug ("adding the node {} to the lacpSystem", nodeId);
            if (addLacpNode(nodeId, lacpNode) == false) {
                LOG.warn ("Unable to add the node {} to the lacp system", nodeId);
                return;
            }
            LOG.debug("send out LACP PDUs for all lags in this node");
            lacpNode.sendLacpPDUs();
        } else {
            /* Node is freshly learnt by lacp feature */
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId);
            if (lacpNode == null)
            {
                LOG.error("cannot add a lacp node for node {}", nodeId);
                return;
            }
            if (addLacpNode(nodeId, lacpNode) == false)
            {
                LOG.warn ("Unable to add the node {} to the lacp system", nodeId);
                return;
            }
            lacpNode.updateLacpNodeInfo();

            List<NodeConnector> nodeConnectors = node.getNodeConnector();
            if (nodeConnectors == null)
            {
                LOG.debug("NodeConnectors are not available with the node {}", node);
                return;
            }
            for(NodeConnector nc : nodeConnectors)
            {
                FlowCapableNodeConnector flowConnector = nc.getAugmentation(FlowCapableNodeConnector.class);
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                {
                    continue;
                }
                InstanceIdentifier<NodeConnector> ncId = (InstanceIdentifier<NodeConnector>)
                    InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                    .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nc.getKey()).build();
                lacpNode.addNonLacpPort (ncId);
            }
        }
    }
    public int getLacpSystemNumNodes()
    {
        return (LACPNODE_MAP.size());
    }
    public void clearResources()
    {
        /* clear the node and nodeConnectors learnt */
        this.clearLacpNodes();

        // clear the Tx Processor threads
        TxProcessor.resetLacpLoaded();
        // clear the Tx queues
        LacpTxQueue lacpTxQueue = LacpTxQueue.getLacpTxQueueInstance();
        lacpTxQueue.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
        lacpTxQueue.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        // clear the pdu decoder thread
        PduDecoderProcessor.setLacploaded(false);
    }

    private Node readNodeInfo(DataBroker dataService, InstanceIdentifier<Node> id) {
        Node node = null;
        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();

        try {
            Optional<Node> nodeOpt = null;
            nodeOpt = readTx.read(LogicalDatastoreType.OPERATIONAL, id).get();
            if(nodeOpt.isPresent())
            {
                node = (Node) nodeOpt.get();
            }
        }
        catch(Exception e)
        {
            LOG.error("Failed to read node from data store.", e.getMessage());
            readTx.close();
        }
        readTx.close();
        return node;
    }

    public void addMasterNotifiedNode(InstanceIdentifier<Node> nodeId) {
        if (NOTIFIED_NODE_MAP.contains(nodeId)) {
            readDataStore(nodeId);
            NOTIFIED_NODE_MAP.remove(nodeId);
        } else {
            LOG.debug("notification is not yet received for node {}", nodeId);
            MASTER_NODE_MAP.add(nodeId);
        }
    }

    public boolean checkMasterNotificaitonForNode (InstanceIdentifier<Node> id) {
        if (MASTER_NODE_MAP.contains(id)) {
            MASTER_NODE_MAP.remove(id);
            LOG.debug("master notification received for node {}. processing it", id);
            return true;
        } else {
            LOG.debug("master notification is not received for node {}", id);
            NOTIFIED_NODE_MAP.add(id);
            return false;
        }
    }

    public void removeNodeNotificationOnNodeRemoval(InstanceIdentifier<Node> id) {
        if (NOTIFIED_NODE_MAP.remove(id) == true) {
            LOG.debug("on node removal, removing node {} from notified node map as master notification was not received for it", id);
        }
        return;
    }

    public boolean handleLacpNodeRemoval(InstanceIdentifier<Node> nodeId) {
        LacpNodeExtn lacpNode = null;

        synchronized (LacpSystem.class) {
            LOG.debug ("searching the node in the lacpSystem");
            lacpNode = this.getLacpNode(nodeId);
        }
        if (lacpNode == null) {
            LOG.debug("Node already removed from lacp. Ignoring it {}", nodeId);
            return true;
        }
        Long swId = lacpNode.getSwitchId();
        lacpNode.setLacpNodeDeleteStatus (true);
        LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
        LOG.debug("sending node delete msg");
        if (pduQueue.isLacpQueuePresent(swId) == true) {
            LacpNodeNotif nodeNotif = new LacpNodeNotif();
            if (pduQueue.enqueueAtFront(swId, nodeNotif) == false) {
                LOG.warn ("Failed to enqueue node deletion message to the pduQ for node {}", nodeId);
                return false;
            }
        } else {
            LOG.debug ("RSM thread and pduQueue are not yet created for the switch {}, deleteing the node", nodeId);
            synchronized (LacpSystem.class) {
                if (this.removeLacpNode (swId) == null) {
                    LOG.error("Unable to remove the node {} from the lacpSystem in node remove handling.", swId);
                    return false;
                } else {
                    LOG.debug ("Removed the node {} from lacpSystem in node remove handling.", swId);
                }
            }
        }
        return true;
    }
}
