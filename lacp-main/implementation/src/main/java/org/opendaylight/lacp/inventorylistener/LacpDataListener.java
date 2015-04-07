/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventorylistener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.util.LacpPortType;
import org.opendaylight.lacp.Utils.NodePort;
import org.opendaylight.lacp.Utils.LacpPortProperties;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpPortStatus;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;

public class LacpDataListener implements DataChangeListener
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpDataListener.class);
    private final DataBroker dataService;
    private static HashSet<InstanceIdentifier<NodeConnector>> intNodeConnSet;
    private static final String CURRTOPO = "flow:1";

    public LacpDataListener (DataBroker dataBroker)
    {
        this.dataService = dataBroker;
        intNodeConnSet = new HashSet<InstanceIdentifier<NodeConnector>>();
        updateInternalNodeConnectors();
    }
    
    public ListenerRegistration<DataChangeListener> registerDataChangeListener()
    {
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                                                .child(Topology.class, new TopologyKey(new TopologyId(CURRTOPO)))
                                                .child(Link.class).build();
        return dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, linkInstance,
                                                     this, AsyncDataBroker.DataChangeScope.BASE);
    }
    /* If the nodes are already available, obtain the available links in the learnt topology
     * and update the internal nodeConnector set */
    public void updateInternalNodeConnectors()
    {
        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
        Topology topology = null;
        NetworkTopology networkTopology = null;

        InstanceIdentifier<Topology> topoId = InstanceIdentifier.builder(NetworkTopology.class)
                                                  .child(Topology.class,
                                                         new TopologyKey(new TopologyId(CURRTOPO)))
                                                  .build();
        try
        {
            Optional<Topology> optional = readTx.read(LogicalDatastoreType.OPERATIONAL, topoId).get();
            if (optional.isPresent())
            {
                topology = optional.get();
            }
            if (topology == null)
            {
                LOG.debug("Topology is not yet created {}", topoId);
                return;
            }
            List<Link> links = topology.getLink();
            if (links == null || links.isEmpty())
            {
                LOG.debug("Topology is not yet updated with the links {}", topoId);
                return;
            } 
            for (Link link : links)
            {
                if (!(link.getLinkId().getValue().contains("host")))
                {
                    addIntNodeConnectors(link);
                }
            }
        }
        catch(Exception e)
        {
            LOG.error("Error reading the network topology", e.getMessage());
            readTx.close();
        }     
        readTx.close();
    }

    public static boolean checkExternalNodeConn (InstanceIdentifier ncId)
    {
        if (!(intNodeConnSet.contains (ncId)))
        {
            return true;
        }
        LOG.debug("Given port is not an external port {}", ncId);
        return false;
    }

    private void verifyAndDeleteInternalLacpPort (InstanceIdentifier ncId)
    {
        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        InstanceIdentifier nodeId = ncId.firstIdentifierOf(Node.class);
        LacpNodeExtn lacpNode = lacpSystem.getLacpNode(nodeId);
        if (lacpNode == null)
        {
            LOG.warn("Node cannot be retrived for the given node-connector {}", ncId);
            return;
        }
        if (lacpNode.containsPort(ncId) != LacpPortType.LACP_PORT)
        {
            LOG.debug("internal port {} is not an lacp port. Ignoring it", ncId);
            return;
        }

        int portId = (int) NodePort.getPortId(new NodeConnectorRef(ncId)); 
        long swId = NodePort.getSwitchId(new NodeConnectorRef(ncId)); 
        NodeConnector nc = (NodeConnector) InstanceIdentifier.keyOf(ncId); 
        int portFeaturesResult = LacpPortProperties.mapSpeedDuplexFromPortFeature(nc);
 
        LacpPDUPortStatusContainer pduElem = null; 
        int down = 2;
        pduElem = new LacpPortStatus(swId, portId, down, portFeaturesResult, ncId); 
        LacpPDUQueue pduQueue = LacpPDUQueue.getLacpPDUQueueInstance(); 

        if ((pduQueue!= null) && !(pduQueue.enqueue(swId, pduElem)))
        {
            LOG.debug("Failed to enque port status object for port={}, switch {}",portId, swId); 
        } 
        lacpNode.addNonLacpPort(ncId);
        LOG.debug("internal port {} is removed as a lacp port and added as a non-lacp port.", ncId);
        return;
    }
    
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent)
    {
        if(dataChangeEvent == null)
        {
            return;
        }
        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Set<InstanceIdentifier<?>> removedData = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
        if ((createdData != null) && !(createdData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> linkset = createdData.keySet();
            for (InstanceIdentifier<?> linkId : linkset)
            {
                if (Link.class.isAssignableFrom(linkId.getTargetType()))
                {
                    Link link = (Link) createdData.get(linkId);
                    if (!(link.getLinkId().getValue().contains("host")))
                    {
                        addIntNodeConnectors(link);
                    }
                }
            }
        }
        if ((removedData != null) && (!removedData.isEmpty()) && (originalData != null) && (!originalData.isEmpty()))
        {
            for (InstanceIdentifier<?> instanceId : removedData)
            {
                if (Link.class.isAssignableFrom(instanceId.getTargetType()))
                {
                    Link link = (Link) originalData.get(instanceId);
                    if (!(link.getLinkId().getValue().contains("host")))
                    {
                        removeIntNodeConnectors(link);
                    }
                }
            }
        }
    }
    private void addIntNodeConnectors(Link link)
    {
        InstanceIdentifier id;
        if (link.getDestination().getDestTp().getValue().contains("host"))
        {
            id = createNCId(link.getSource().getSourceNode().getValue(), 
                            link.getSource().getSourceTp().getValue());
        }
        else
        {
            id = createNCId(link.getDestination().getDestNode().getValue(),
                            link.getDestination().getDestTp().getValue());
        }
        intNodeConnSet.add(id);
        LOG.debug ("added port {} to lacp internal port list", id);
        /* The link is getting added as an internal link,
         * if any of the edge nodeConnectors was added as a lacp port to the node
         * remove the port as a lacp port as lacp can be enabled only on external ports */
        verifyAndDeleteInternalLacpPort(id);
        return;
    }
    private void removeIntNodeConnectors(Link link)
    {
        InstanceIdentifier id;
        if (link.getDestination().getDestTp().getValue().contains("host"))
        {
            id = createNCId(link.getSource().getSourceNode().getValue(), 
                            link.getSource().getSourceTp().getValue());
        }
        else
        {
            id = createNCId(link.getDestination().getDestNode().getValue(),
                            link.getDestination().getDestTp().getValue());
        }
        intNodeConnSet.remove(id);
        LOG.debug ("removed port {} from lacp internal port list", id);
        return;
    }

    private InstanceIdentifier createNCId(String nodeId, String nodeConnId)
    {
        return (InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(nodeId)))
                                    .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnId))).build());
    }
}
