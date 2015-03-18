/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventoryListener;

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

public class LacpDataListener implements DataChangeListener
{
    private static final Logger log = LoggerFactory.getLogger(LacpDataListener.class);
    private final DataBroker dataService;
    private static HashSet<InstanceIdentifier<NodeConnector>> extNodeConnSet;
    private static final String CURRTOPO = "flow:1";

    public LacpDataListener (DataBroker dataBroker)
    {
        this.dataService = dataBroker;
        extNodeConnSet = new HashSet<InstanceIdentifier<NodeConnector>>();
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
     * and update the external nodeConnector set */
    public void updateInternalNodeConnectors()
    {
        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
        Topology topology = null;
        NetworkTopology networkTopology = null;

/* check if this is required or reading from the fixed flow:1 topology is sufficient -- TODO kalai */
        InstanceIdentifier<NetworkTopology> networkTopoId = InstanceIdentifier.builder(NetworkTopology.class).build();
        try
        {
            Optional<NetworkTopology> optNetwork = readTx.read(LogicalDatastoreType.OPERATIONAL, networkTopoId).get();
            if (optNetwork.isPresent())
            {
                networkTopology = optNetwork.get();
                for (Topology netTopology : networkTopology.getTopology())
                {
                    InstanceIdentifier<Topology> topoId = InstanceIdentifier.builder(NetworkTopology.class)
                                                              .child(Topology.class,
                                                                     new TopologyKey(netTopology.getTopologyId()))
                                                              .build();
                    Optional<Topology> optional = readTx.read(LogicalDatastoreType.OPERATIONAL, topoId).get();
                    if (optional.isPresent())
                    {
                        topology = optional.get();
                    }
                    if (topology == null)
                    {
                        log.debug("Topology is not yet created {}", topoId);
                        continue;
                    }
                    List<Link> links = topology.getLink();
                    if (links == null || links.isEmpty())
                    {
                        log.debug("Topology is not yet updated with the links {}", topoId);
                        continue;
                    } 
                    for (Link link : links)
                    {
                        if (link.getLinkId().getValue().contains("host"))
                        {
                            addExtNodeConnectors(link);
                        }
                    }

                }
            }
        }
        catch(Exception e)
        {
            log.error("Error reading the network topology");
            readTx.close();
        }     
        readTx.close();
    }

    public static boolean checkExternalNodeConn (InstanceIdentifier ncId)
    {
        if (extNodeConnSet.contains (ncId))
        {
            return true;
        }
        log.debug("Given port is not an external port {}", ncId);
        return false;
    }

    private void verifyAndDeleteExternalLacpPort (InstanceIdentifier ncId)
    {
        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        InstanceIdentifier nodeId = ncId.firstIdentifierOf(Node.class);
        LacpNodeExtn lacpNode = lacpSystem.getLacpNode(nodeId);
        if (lacpNode == null)
        {
            log.warn("Node cannot be retrived for the given node-connector {}", ncId);
            return;
        }
        if (lacpNode.containsPort(ncId) != LacpPortType.LACP_PORT)
        {
            log.debug("internal port {} is not an lacp port. Ignoring it", ncId);
            return;
        }
        /* post port down for lacp port */
        lacpNode.addNonLacpPort(ncId);
        log.debug("internal port {} is removed as a lacp port and added as a non-lacp port.", ncId);
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
                    if (link.getLinkId().getValue().contains("host"))
                    {
                        addExtNodeConnectors(link);
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
                    if (link.getLinkId().getValue().contains("host"))
                    {
                        removeExtNodeConnectors(link);
                    }
                }
            }
        }
    }
    private void addExtNodeConnectors(Link link)
    {
        InstanceIdentifier dest = createNCId(link.getDestination().getDestNode().getValue(),
                                             link.getDestination().getDestTp().getValue());
        InstanceIdentifier src = createNCId(link.getSource().getSourceNode().getValue(), 
                                            link.getSource().getSourceTp().getValue());
        extNodeConnSet.add(dest);
        extNodeConnSet.add(src);
        return;
    }
    private void removeExtNodeConnectors(Link link)
    {
        InstanceIdentifier dest = createNCId(link.getDestination().getDestNode().getValue(),
                                             link.getDestination().getDestTp().getValue());
        InstanceIdentifier src = createNCId(link.getSource().getSourceNode().getValue(), 
                                            link.getSource().getSourceTp().getValue());
        extNodeConnSet.remove(dest);
        extNodeConnSet.remove(src);
        /* The link is getting removed as external link,
         * if any of the edge nodeConnectors was added as a lacp port to the node
         * remove the port as a lacp port as lacp can be enabled only on external ports */
        verifyAndDeleteExternalLacpPort(dest);
        verifyAndDeleteExternalLacpPort(src);
        return;
    }

    private InstanceIdentifier createNCId(String nodeId, String nodeConnId)
    {
        return (InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(nodeId)))
                                    .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnId))).build());
    }
}
