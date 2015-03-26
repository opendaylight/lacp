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

public class LacpSystem 
{
    private static final ConcurrentHashMap<Long, LacpNodeExtn> lacpNodeMap = new ConcurrentHashMap<Long, LacpNodeExtn>();
    private static final LacpSystem lacpSystem = new LacpSystem();
    private static final Logger log = LoggerFactory.getLogger(LacpSystem.class);
    private static final Long INVALID_SWITCHID = new Long ("-1");

    private LacpSystem ()
    {
    }    
    public static LacpSystem getLacpSystem ()
    {
        return lacpSystem;
    }    
    public boolean addLacpNode (InstanceIdentifier nodeId, LacpNodeExtn lacpNode)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        if (swId.equals(INVALID_SWITCHID))
        {
            log.warn ("Invalid node id {}, could not add the node to the lacpSystem", nodeId);
            return false;
        }
        lacpNodeMap.put(swId, lacpNode);
        return true;
    }
    public LacpNodeExtn removeLacpNode (InstanceIdentifier nodeId)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        if (swId.equals(INVALID_SWITCHID))
        {
            log.warn ("Invalid node id {}, could not remove the node to the lacpSystem", nodeId);
            return null;
        }
        LacpNodeExtn lacpNode = lacpNodeMap.remove(swId);
        lacpNode.deleteLacpNode(false);
        return lacpNode;
    }
    public LacpNodeExtn getLacpNode (Long switchId)
    {
        if (switchId.equals(INVALID_SWITCHID))
        {
            log.warn ("Invalid switch id {}, could not obtain the node to the lacpSystem", switchId);
            return null;
        }
        return (lacpNodeMap.get(switchId));
    }
    public LacpNodeExtn getLacpNode (InstanceIdentifier nodeId)
    {
        Long swId = LacpUtil.getNodeSwitchId(nodeId);
        return (getLacpNode (swId));
    }
    public void clearLacpNodes ()
    {
        Collection<LacpNodeExtn> nodeList = lacpNodeMap.values();
        for (LacpNodeExtn lacpNode : nodeList)
        {
            lacpNode.deleteLacpNode(true);
        }
        lacpNodeMap.clear();
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
                nodes = (Nodes) nodesOpt.get();
        }
        catch(Exception e)
        {
            log.error("Failed to read node list from data store.");
            readTx.close();
        }
        readTx.close();

        if(nodes == null)
        {
            log.debug("No node is connected yet to controller.");
            return;
        }
        
        log.debug("Reading the list of nodes connected to the controller.");
        for (Node node : nodes.getNode())
        {
            InstanceIdentifier<Node> nodeId 
                    = InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey()).build();
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId); 
            if (lacpNode == null) 
            { 
                log.error("cannot add a lacp node for node {}", nodeId);  
                return; 
            } 
            if (addLacpNode(nodeId, lacpNode) == false)
            {
                log.warn ("Unable to add the node {} to the lacp system", nodeId);
                continue;
            }

            List<NodeConnector> nodeConnectors = node.getNodeConnector();
            if (nodeConnectors == null)
            {
                log.debug("NodeConnectors are not available with the node {}", node);
                continue;
            }
            for(NodeConnector nc : nodeConnectors)
            {
                FlowCapableNodeConnector flowConnector = nc.getAugmentation(FlowCapableNodeConnector.class);
                PortState portState = flowConnector.getState();
                if ((portState == null) || (portState.isLinkDown()))
                    continue;
                InstanceIdentifier<NodeConnector> ncId = (InstanceIdentifier<NodeConnector>)
                                InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                                 .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nc.getKey()).build();
                lacpNode.addNonLacpPort (ncId);

            }
        }
    }
    public int getLacpSystemNumNodes()
    {
        return (lacpNodeMap.size());
    }
    public void clearResources()
    {
        /* clear the node and nodeConnectors learnt */
        this.clearLacpNodes();
    }
}
