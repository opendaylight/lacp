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
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.queue.LacpNodeNotif;


enum EventType
{
    UPDATED,
    DELETED;
}

public class LacpNodeListener implements OpendaylightInventoryListener
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpNodeListener.class);
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
        {
            return;
        }
        LOG.info("got a node connec removed in lacp {} ", nodeConnectorRemoved);
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.DELETED, nodeConnectorRemoved));
    }

    @Override
    public void onNodeConnectorUpdated (NodeConnectorUpdated nodeConnectorUpdated)
    {
        if (nodeConnectorUpdated == null)
        {
            return;
        }
        LOG.info("got a node connec Updated {} in lacp ", nodeConnectorUpdated);
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.UPDATED, nodeConnectorUpdated));
    }

    @Override
    public void onNodeRemoved (NodeRemoved nodeRemoved)
    {
        if (nodeRemoved == null)
        {
            return;
        }
        LOG.info("got a node removed {} in lacp ", nodeRemoved);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeRemoved.getNodeRef().getValue();
        lacpService.submit(new LacpNodeUpdate(nodeId, EventType.DELETED));
    }

    @Override
    public void onNodeUpdated (NodeUpdated nodeUpdated)
    {
        if (nodeUpdated == null)
        {
            return;
        }
        LOG.info("got a node updated {} ", nodeUpdated);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeUpdated.getNodeRef().getValue();
        lacpService.submit(new LacpNodeUpdate(nodeId, EventType.UPDATED));
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
            if (event.equals(EventType.UPDATED) == true)
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
            synchronized (LacpSystem.class)
            {
            if (lacpSystem.getLacpNode(nodeId) != null)
            {
                LOG.debug ("Node already notified to lacp. Ignoring it {}", nodeId);
                return;
            }
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId);
            if (lacpNode == null)
            {
                LOG.error("cannot add a lacp node for node {}", nodeId);
                return;
            }
            lacpSystem.addLacpNode(nodeId, lacpNode);
            }
        }

        private void handleNodeDeletion (InstanceIdentifier<Node> lNode)
        {
            InstanceIdentifier <Node> nodeId = lNode;
            LacpNodeExtn lacpNode = null;
            synchronized (LacpSystem.class)
            {
                lacpNode = lacpSystem.getLacpNode(nodeId);
                if (lacpNode == null)
                {
                    LOG.debug("Node already removed from lacp. Ignoring it {}", nodeId);
                    return;
                }
                Long swId = lacpNode.getSwitchId();
                lacpNode.setLacpNodeDeleteStatus (true);
                LacpNodeNotif nodeNotif = new LacpNodeNotif();
                LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
                if (pduQueue.enqueue(swId, nodeNotif) == false)
                {
                    LOG.warn ("Failed to enqueue node deletion message to the pduQ for node {}", nodeId);
                }

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

            if (event.equals(EventType.UPDATED) == true)
            {
                lNodeCon = (InstanceIdentifier<NodeConnector>)ncUpdated.getNodeConnectorRef().getValue();
                FlowCapableNodeConnectorUpdated flowConnector = ncUpdated.<FlowCapableNodeConnectorUpdated>getAugmentation(FlowCapableNodeConnectorUpdated.class);
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
            else
            {
                lNodeCon = (InstanceIdentifier<NodeConnector>)ncRemoved.getNodeConnectorRef().getValue();
                handlePortDelete(lNodeCon, false);
            }
        }


	private boolean enqueuePortStatus (InstanceIdentifier<NodeConnector> ncId, int upDown){

                boolean result = false;
                if (ncId != null){
                        short port_id = NodePort.getPortId(new NodeConnectorRef(ncId));
                        long sw_id = NodePort.getSwitchId(new NodeConnectorRef(ncId));
                        NodeConnector nc = (NodeConnector)InstanceIdentifier.keyOf(ncId);
                        int portFeaturesResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(nc);
                        LacpPDUPortStatusContainer pduElem = null;
                        pduElem = new LacpPortStatus(sw_id,port_id,upDown,portFeaturesResult, ncId);
                        LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
                        if((pduQueue!= null) && !(pduQueue.enqueue(sw_id,pduElem))){
                                LOG.debug("Failed to enque port status object for port={}",port_id);
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
            synchronized (LacpSystem.class)
            {
                lacpNode = lacpSystem.getLacpNode(nodeId);
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
          	    if(!enqueuePortStatus(ncId,1)){
                        LOG.debug("port {} with state UP is enqued succesfully for port state procesing", ncId);
                    }else{
                        LOG.error("port {}, enque failed", ncId);
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
                synchronized (lacpNode)
                {
		    if(!enqueuePortStatus(ncId,2)){
                        LOG.debug("port {} with state DOWN is enqued succesfully for port state procesing", ncId);
                    }else{
                        LOG.error("port {} enque failed", ncId);
                    }

                    if (lacpNode.deletePort (ncId, hardReset) == false)
                    {
                        LOG.debug("port not present with the lacp node. Ignoring it {}", ncId) ;
                        return;
                    }
                }
            }
            else
            {
                LOG.error("got a a nodeConnector removal for non-existing node {} ", nodeId);
            }
        }
    }
}
