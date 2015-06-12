/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
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

    private LacpSystem ()
    {
    }    
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
    public void readDataStore (DataBroker dataService)
    {
        InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
        Nodes nodes = null;
        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();

        try {
            Optional<Nodes> nodesOpt = null;
            nodesOpt = readTx.read(LogicalDatastoreType.OPERATIONAL, nodesBuilder.build()).get();
            if(nodesOpt.isPresent())
            {
                nodes = (Nodes) nodesOpt.get();
            }
        }
        catch(Exception e)
        {
            LOG.error("Failed to read node list from data store.", e.getMessage());
            readTx.close();
        }
        readTx.close();

        if(nodes == null)
        {
            LOG.debug("No node is connected yet to controller.");
            return;
        }
        
        LOG.debug("Reading the list of nodes connected to the controller.");
        for (Node node : nodes.getNode())
        {
            InstanceIdentifier<Node> nodeId 
                    = InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey()).build();
        
            Long switchId = LacpUtil.getNodeSwitchId(nodeId);
            if (switchId.equals(INVALID_SWITCHID))
            {
                LOG.warn ("Node obtained {} is not an openflow enabled node. Not adding it part of lacp system", nodeId);
                continue;
            }
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId); 
            if (lacpNode == null) 
            { 
                LOG.error("cannot add a lacp node for node {}", nodeId);  
                return; 
            } 
            if (addLacpNode(nodeId, lacpNode) == false)
            {
                LOG.warn ("Unable to add the node {} to the lacp system", nodeId);
                continue;
            }
            lacpNode.updateLacpNodeInfo();

            List<NodeConnector> nodeConnectors = node.getNodeConnector();
            if (nodeConnectors == null)
            {
                LOG.debug("NodeConnectors are not available with the node {}", node);
                continue;
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
}
