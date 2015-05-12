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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.lacp.Utils.*;
import org.opendaylight.lacp.util.LacpUtil;
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

public class LacpNodeListener 
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpNodeListener.class);
    private final ExecutorService lacpService = Executors.newCachedThreadPool();
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
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.DELETED, ncId, nc));
    }

    public void updateNodeConnector (InstanceIdentifier<NodeConnector> ncId, NodeConnector nc)
    {
        LOG.info("got a node connec Updated {} in lacp ", ncId);
        lacpService.submit(new LacpNodeConnectorUpdate(EventType.UPDATED, ncId, nc));
    }

    public void removeNode (InstanceIdentifier<Node> nodeId, Node node)
    {
        LOG.info("got a node removed {} in lacp ", node);
        lacpService.submit(new LacpNodeUpdate(nodeId, EventType.DELETED));
    }

    public void updateNode (InstanceIdentifier<Node> nodeId, Node node)
    {
        LOG.info("got a node updated {} ", node);
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
            LOG.debug ("entering handleNodeUpdate");
            InstanceIdentifier<Node> nodeId = lNode;
            synchronized (LacpSystem.class)
            {
            LOG.debug ("verifying node is already available");
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
            LOG.debug ("adding the node to the lacpSystem");
            lacpSystem.addLacpNode(nodeId, lacpNode);
            LOG.debug ("added node for nodeId {}", nodeId);
            }
        }

        private void handleNodeDeletion (InstanceIdentifier<Node> lNode)
        {
            LOG.debug ("entering handleNodeDelete");
            InstanceIdentifier <Node> nodeId = lNode;
            LacpNodeExtn lacpNode = null;

            synchronized (LacpSystem.class)
            {
                LOG.debug ("searching the node in the lacpSystem");
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
                LOG.debug("sending node delete msg");
                if (pduQueue.enqueueAtFront(swId, nodeNotif) == false)
                {
                    LOG.warn ("Failed to enqueue node deletion message to the pduQ for node {}", nodeId);
                }

            }
        }
    }

    private class LacpNodeConnectorUpdate implements Runnable
    {
        private EventType event;
        private NodeConnector ncUpdated;
        private InstanceIdentifier<NodeConnector> ncId;
        private NodeConnectorRemoved ncRemoved;

        public LacpNodeConnectorUpdate (EventType evt, InstanceIdentifier<NodeConnector> nodeConnId, NodeConnector nc)
        {
            this.event = evt;
            this.ncUpdated = nc;
            this.ncId = nodeConnId;
        }

        @Override
        public void run ()
        { 
            InstanceIdentifier<NodeConnector> lNodeCon;
            lNodeCon = ncId;
            if (event.equals(EventType.UPDATED) == true)
            {
                FlowCapableNodeConnector flowConnector = ncUpdated.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
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
            else
            {
                FlowCapableNodeConnector flowConnector = ncUpdated.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
                long portNum = flowConnector.getPortNumber().getUint32();
                if (portNum > LacpUtil.getLogPortNum())
                {
                    LOG.debug ("avoiding notifications for the logical port {}", portNum);
                    return;
                }
                handlePortDelete(lNodeCon, false);
            }
        }


	private boolean enqueuePortStatus (InstanceIdentifier<NodeConnector> ncId, int upDown){

                boolean result = false;
                if (ncId != null){
                        short portId = NodePort.getPortId(new NodeConnectorRef(ncId));
                        long swId = NodePort.getSwitchId(new NodeConnectorRef(ncId));
			//TODO Need to fix this
                        //NodeConnector nc = (NodeConnector)InstanceIdentifier.keyOf(ncId);
                        int portFeaturesResult = 0;//LacpPortProperties.mapSpeedDuplexFromPortFeature(nc);
                        LacpPDUPortStatusContainer pduElem = null;
                        pduElem = new LacpPortStatus(swId,portId,upDown,portFeaturesResult, ncId);
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
		    /*
          	    if(!enqueuePortStatus(ncId,1)){
                        LOG.debug("port {} with state UP is enqued succesfully for port state procesing", ncId);
                    }else{
                        LOG.error("port {}, enque failed", ncId);
                    }
		    */
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
