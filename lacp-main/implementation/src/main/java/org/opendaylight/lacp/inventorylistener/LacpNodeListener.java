/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventorylistener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.queue.LacpNodeNotif;
import org.opendaylight.lacp.util.LacpPortType;


enum EventType
{
    UPDATED,
    DELETED,
    STATUS_UPDATE;
}

public class LacpNodeListener implements OpendaylightInventoryListener
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpNodeListener.class);
    private final ExecutorService lacpServiceNode = Executors.newCachedThreadPool();
    private final ExecutorService lacpServiceNodeConnector = Executors.newCachedThreadPool();

    private static LacpSystem lacpSystem;
    private static final LacpNodeListener LIST_INSTANCE = new LacpNodeListener();

    private LacpNodeListener ()
    {}

    public static void setLacpSystem (LacpSystem lacpSys)
    {
        lacpSystem = lacpSys;
    }
    public static LacpNodeListener getNodeListenerInstance()
    {
        return LIST_INSTANCE;
    }
    public void removeNodeConnector (InstanceIdentifier<NodeConnector> ncId, NodeConnector nc)
    {
        LOG.info("got a node connec removed in lacp {} ", ncId);

        NodeConnectorId id = InstanceIdentifier.keyOf(ncId).getId();
        if (id.getValue().contains("LOCAL"))
        {
            /* LOCAL port used for communicating with the controller is removed.
             * that is the node is removed from the controller. If the node
             *  deletion notification is not yet received, set the deleteStatus
             *  flag here so that nodeconnector deletion and aggregator deletion
             *  are not written to the datastore. Node Deletion message will be
             *  posted on receiving the node removal notification */
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = null;
            synchronized (LacpSystem.class)
            {
                lacpNode = lacpSystem.getLacpNode(nodeId);
            }
            if ((lacpNode != null) && (lacpNode.getLacpNodeDeleteStatus() != true))
            {
                lacpNode.setLacpNodeDeleteStatus (true);
            }
        }
        else
        {
            lacpServiceNodeConnector.submit(new LacpNodeConnectorUpdate(EventType.DELETED, ncId, nc));
        }
    }

    public void updateNodeConnector (InstanceIdentifier<NodeConnector> ncId, NodeConnector nc)
    {
        LOG.info("got a node connec Updated {} in lacp ", ncId);
        lacpServiceNodeConnector.submit(new LacpNodeConnectorUpdate(EventType.UPDATED, ncId, nc));
    }

    public void removeNode (InstanceIdentifier<Node> nodeId)
    {
        LOG.info("got a node removed {} in lacp ", nodeId);
        /* for node removal notification, only message is posted to the RSM thread.
         * Message posting is done in the md-sal worker thread context itself as to
         * avoid waiting for spawing a new thread for node deletion */
        new LacpNodeUpdate(nodeId, null, EventType.DELETED).handleNodeDeletion(nodeId);
    }

    public void updateNode (InstanceIdentifier<Node> nodeId, Node node)
    {
        LOG.info("got a node updated {} ", node);
        lacpServiceNode.submit(new LacpNodeUpdate(nodeId, node, EventType.UPDATED));
    }
    @Override
    public void onNodeConnectorRemoved (NodeConnectorRemoved nodeConnectorRemoved)
    {
        //do Nothing for node removal. it is handled via the datachange listener
    }
    @Override
    public void onNodeConnectorUpdated (NodeConnectorUpdated nodeConnectorUpdated)
    {
        if (nodeConnectorUpdated == null)
        {
            return;
        }
        LOG.debug("got a node connec Updated from inventory listener {} in lacp ", nodeConnectorUpdated);
        InstanceIdentifier<NodeConnector> instanceId = (InstanceIdentifier<NodeConnector>)nodeConnectorUpdated.getNodeConnectorRef().getValue();
        lacpServiceNodeConnector.submit(new LacpNodeConnectorUpdate(EventType.STATUS_UPDATE, instanceId,
                                                       nodeConnectorUpdated));
    }
    @Override
    public void onNodeRemoved (NodeRemoved nodeRemoved)
    {
        //do Nothing for node removal. it is handled via the datachange listener
    }
    @Override
    public void onNodeUpdated (NodeUpdated nodeUpdated)
    {
        //do Nothing for node updation. it is handled via the datachange listener
    }
    public void releaseThreadPool()
    {
        lacpServiceNodeConnector.shutdown();
        lacpServiceNode.shutdown();
    }

    private class LacpNodeUpdate implements Runnable
    {
        private InstanceIdentifier<Node> lNode;
        private Node node;
        private EventType event;

        public LacpNodeUpdate(InstanceIdentifier<Node> nodeId, Node lacpNode, EventType evt)
        {
            lNode = nodeId;
            node = lacpNode;
            event = evt;
        }
        @Override
        public void run ()
        {
            if (event.equals(EventType.UPDATED) == true)
            {
                handleNodeUpdate(lNode, node);
            }
            else
            {
                handleNodeDeletion(lNode);
            }
        }

        public void handleNodeUpdate (InstanceIdentifier<Node> lNode, Node node)
        {
            boolean addResult = false;
            LOG.debug ("entering handleNodeUpdate");
            InstanceIdentifier<Node> nodeId = lNode;
            LacpNodeExtn lacpNode = null;
            synchronized (LacpSystem.class)
            {
                LOG.debug ("verifying node is already available");
                lacpNode = lacpSystem.getLacpNode(nodeId);
            }
            if (lacpNode != null)
            {
                LOG.debug ("Node already notified to lacp. Ignoring it {}", nodeId);
                return;
            }
            lacpNode = new LacpNodeExtn (nodeId);
            if (lacpNode == null)
            {
                LOG.error("cannot add a lacp node for node {}", nodeId);
                return;
            }
            synchronized (LacpSystem.class)
            {
                LOG.debug ("adding the node to the lacpSystem");
                addResult = lacpSystem.addLacpNode(nodeId, lacpNode);
                LOG.debug ("added node for nodeId {}", nodeId);
            }
            if (addResult == false)
            {
                LOG.error ("Could not add the node {} to the lacpSystem", nodeId);
                return;
            }
            lacpNode.updateLacpNodeInfo();
            // if the node-connector notification have been received before the node update
            // notification, then those notifications will be lost. So get the list of
            // node-connectors available for the node and process it
            List<NodeConnector> nodeConnectors = node.getNodeConnector();
            if(nodeConnectors == null)
            {
                LOG.debug ("NodeConnectors are not available for the node. Returning");
                return;
            }
            for(NodeConnector nc : nodeConnectors)
            {
                FlowCapableNodeConnector flowConnector = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
                long portNum = flowConnector.getPortNumber().getUint32();
                if (portNum > LacpUtil.getLogPortNum())
                {
                    LOG.debug ("avoiding notifications for the logical port {}", portNum);
                    continue;
                }
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                {
                    continue;
                }
                InstanceIdentifier<NodeConnector> ncId =
                    InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                      .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nc.getKey()).build();
                LOG.debug ("adding non-lacp for port {} in NodeUpdate", ncId);
                synchronized (lacpNode)
                {
                    lacpNode.addNonLacpPort(ncId);
                }
            }
        }

        public void handleNodeDeletion (InstanceIdentifier<Node> lNode)
        {
            LOG.debug ("entering handleNodeDelete");
            InstanceIdentifier <Node> nodeId = lNode;
            LacpNodeExtn lacpNode = null;

            synchronized (LacpSystem.class)
            {
                LOG.debug ("searching the node in the lacpSystem");
                lacpNode = lacpSystem.getLacpNode(nodeId);
            }
            if (lacpNode == null)
            {
                LOG.debug("Node already removed from lacp. Ignoring it {}", nodeId);
                return;
            }
            Long swId = lacpNode.getSwitchId();
            lacpNode.setLacpNodeDeleteStatus (true);
            LacpNodeNotif nodeNotif = new LacpNodeNotif();
            LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
            LOG.debug("sending node delete msg");
            if (pduQueue.isLacpQueuePresent(swId) == true)
            {
                if (pduQueue.enqueueAtFront(swId, nodeNotif) == false)
                {
                    LOG.warn ("Failed to enqueue node deletion message to the pduQ for node {}", nodeId);
                }
            }
            else
            {
                LOG.debug ("RSM thread and pduQueue are not yet created for the switch {}, deleteing the node", nodeId);
                LacpSystem lacpSystem = LacpSystem.getLacpSystem();
                synchronized (LacpSystem.class)
                {
                    if (lacpSystem.removeLacpNode (swId) == null)
                    {
                        LOG.error("Unable to remove the node {} from the lacpSystem in node remove handling.", swId);
                    }
                    else
                    {
                        LOG.debug ("Removed the node {} from lacpSystem in node remove handling.", swId);
                    }
                }
            }
        }
    }

    private class LacpNodeConnectorUpdate implements Runnable
    {
        private EventType event;
        private NodeConnector nc;
        private InstanceIdentifier<NodeConnector> ncId;
        private NodeConnectorUpdated ncUpdated;

        public LacpNodeConnectorUpdate (EventType evt, InstanceIdentifier<NodeConnector> nodeConnId, NodeConnector nodeConn)
        {
            this.event = evt;
            this.nc = nodeConn;
            this.ncId = nodeConnId;
        }
        public LacpNodeConnectorUpdate (EventType evt, InstanceIdentifier<NodeConnector> nodeConnId, NodeConnectorUpdated nodeConnUpdated)
        {
            this.event = evt;
            this.ncUpdated = nodeConnUpdated;
            this.ncId = nodeConnId;
        }

        @Override
        public void run ()
        {
            InstanceIdentifier<NodeConnector> lNodeCon;
            lNodeCon = ncId;
            if (event.equals(EventType.UPDATED) == true)
            {
                FlowCapableNodeConnector flowConnector = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
                long portNum = flowConnector.getPortNumber().getUint32();
                if (portNum > LacpUtil.getLogPortNum())
                {
                    LOG.debug ("avoiding notifications for the logical port {}", portNum);
                    return;
                }
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                {
                   // port is in linkdown state, remove the port from the node.
                    handlePortDelete(lNodeCon, true);
                }
                else
                {
                    // port is in linkup state, add the port to the node.
                    handlePortUpdate(lNodeCon);
                }
            }
            else if(event.equals(EventType.DELETED))
            {
                FlowCapableNodeConnector flowConnector = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
                long portNum = flowConnector.getPortNumber().getUint32();
                if (portNum > LacpUtil.getLogPortNum())
                {
                    LOG.debug ("avoiding notifications for the logical port {}", portNum);
                    return;
                }
                handlePortDelete(lNodeCon, false);
            }
            else
            {
                InstanceIdentifier<Node> nodeId = lNodeCon.firstIdentifierOf(Node.class);
                LacpNodeExtn lacpNode = null;
                LacpPortType portType;
                synchronized (LacpSystem.class)
                {
                    lacpNode = lacpSystem.getLacpNode(nodeId);
                }
                if (lacpNode == null)
                {
                    return;
                }
                synchronized (lacpNode)
                {
                    portType = lacpNode.containsPort(lNodeCon);
                }
                FlowCapableNodeConnectorUpdated flowConnector
                        = ncUpdated.<FlowCapableNodeConnectorUpdated>getAugmentation(FlowCapableNodeConnectorUpdated.class);
                long portNum = flowConnector.getPortNumber().getUint32();
                if (portNum > LacpUtil.getLogPortNum())
                {
                    LOG.debug ("avoiding notifications for the logical port {}", portNum);
                    return;
                }
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                {
                    // port is in linkdown state, remove the port from the node.
                    if (portType != LacpPortType.NONE)
                    {
                        handlePortDelete(lNodeCon, true);
                    }
                }
                else
                {
                    // port is in linkup state, add the port to the node.
                    if (portType == LacpPortType.NONE)
                    {
                        handlePortUpdate(lNodeCon);
                    }
                }
            }
        }


	private boolean enqueuePortStatus (InstanceIdentifier<NodeConnector> ncId, int upDown, boolean hardReset)
    {
                boolean result = false;
                if (ncId != null){
                        short portId = NodePort.getPortId(new NodeConnectorRef(ncId));
                        long swId = NodePort.getSwitchId(new NodeConnectorRef(ncId));
                        LacpPDUPortStatusContainer pduElem = null;
                        pduElem = new LacpPortStatus(swId,portId,upDown, ncId, hardReset);
                        LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
                        if((pduQueue!= null) && !(pduQueue.enqueue(swId,pduElem))){
                                LOG.debug("Failed to enque port status object for port={}",portId);
                                result = false;
                        }
                        result = true;
                }
                return result;
        }

        private void handlePortUpdate(InstanceIdentifier<NodeConnector> ncId)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = null;


            int LOOP_COUNT = 6;
            int TIMEOUT = 500;

            try{
               for(int i= 0; i<LOOP_COUNT ; i++){
                 synchronized (LacpSystem.class)
                 {
                     lacpNode = lacpSystem.getLacpNode(nodeId);
                 }
                 if (lacpNode == null)
                 {
                     LOG.debug("got a a nodeConnector updation for non-existing node {}, hence retrying with sleep... ", nodeId);
                     Thread.sleep(TIMEOUT);
                     continue;
                 }else{
                     Long swId = lacpNode.getSwitchId();
                     LOG.debug("node connector update thread woke up from sleep for node {} and is now procesing md-sal write",swId);
                     break;
                 }
                }
            }catch(InterruptedException e){
              LOG.error("Interrupted Exception received in handlePortUpdate while thread is in sleep" + e.toString());
            }

            if (lacpNode != null)
            {
                synchronized (lacpNode)
                {
                    if (lacpNode.addNonLacpPort (ncId) == false)
                    {
                        LOG.debug("port already available with lacp node. Ignoring it {}", ncId);
                        return;
                    }
                }
            }
            else
            {
                LOG.error("got a a nodeConnector updation for non-existing node {} ", nodeId);
            }
        }
        private void handlePortDelete (InstanceIdentifier<NodeConnector> ncId, boolean hardReset)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = null;
            synchronized (LacpSystem.class)
            {
                lacpNode = lacpSystem.getLacpNode(nodeId);
            }
            if (lacpNode != null)
            {
                if(enqueuePortStatus(ncId, 2, hardReset))
                {
                    LOG.debug("port {} with state DOWN is enqued succesfully for port state procesing", ncId);
                }else{
                    LOG.warn("port {} enque failed", ncId);
                }
            }
            else
            {
                LOG.warn("got a a nodeConnector removal for non-existing node {} ", nodeId);
            }
        }
    }
}
