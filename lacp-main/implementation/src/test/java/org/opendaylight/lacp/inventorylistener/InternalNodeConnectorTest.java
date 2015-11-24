/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.inventorylistener;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class InternalNodeConnectorTest {

	private LacpDataListener dataListener;
    @MockitoAnnotations.Mock
    private DataBroker dataBroker;

    @Before
    public void initMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        Link link1 = new LinkBuilder().setLinkId(new LinkId("host:1"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:1")).setDestTp(new TpId("host:1")).build())
                .build();

        Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
            .build();
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        
        List<Link> list = new ArrayList<Link>();
        list.add(link1);
        list.add(link2);
		Topology topology = new TopologyBuilder().setLink(list).build();//add a link naming it as host
        Optional<Topology> optional = Optional.of(topology);
        when(checkedFuture.get()).thenReturn(optional);
        dataListener = new LacpDataListener(dataBroker);
        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class),any(InstanceIdentifier.class),any(LacpDataListener.class),any(AsyncDataBroker.DataChangeScope.class)))
		.thenReturn(Mockito.mock(ListenerRegistration.class));
    }
    

	@Test
	public void testAddRemoveInternalNodeConnectors(){
		
		Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
	            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
	            .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1")).build())
	            .build();
	        Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
	            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
	            .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
	            .build();

	        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChange = Mockito.mock(AsyncDataChangeEvent.class);
	        Map<InstanceIdentifier<?>, DataObject> create = new HashMap<InstanceIdentifier<?>, DataObject>();
	        InstanceIdentifier<Link> id1 = InstanceIdentifier.create(Link.class);
	        InstanceIdentifier<Link> id2 = InstanceIdentifier.create(Link.class);

	        
	        create.put(id1, link1);
	        create.put(id2, link2);
	        when(dataChange.getCreatedData()).thenReturn(create);
	        when(dataChange.getRemovedPaths()).thenReturn(null);
	        when(dataChange.getOriginalData()).thenReturn(null);
	        dataListener.onDataChanged(dataChange);
	        
	        Map<InstanceIdentifier<?>, DataObject> original = new HashMap<InstanceIdentifier<?>, DataObject>();
	        original.put(id1, link1);
	        original.put(id2, link2);
	        Set<InstanceIdentifier<?>> remove = new HashSet<InstanceIdentifier<?>>();
	        remove.add(id1);
	        remove.add(id2);
	        when(dataChange.getCreatedData()).thenReturn(null);
	        when(dataChange.getRemovedPaths()).thenReturn(remove);
	        when(dataChange.getOriginalData()).thenReturn(original);
	        dataListener.onDataChanged(dataChange);
	}
	
	@Test
	public void closeListenerTest(){
		try{
		dataListener.closeListeners();
		dataListener.registerDataChangeListener();
		dataListener.closeListeners();
		}catch(Exception e){};
	}

}
