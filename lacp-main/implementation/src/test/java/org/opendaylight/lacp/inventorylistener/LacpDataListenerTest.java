/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.inventorylistener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class LacpDataListenerTest {
    private LacpDataListener listener;
    @MockitoAnnotations.Mock 
    private DataBroker dataBroker;

    @Before
    public void initMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        Topology topology = new TopologyBuilder().setLink(null).build();
        Optional<Topology> optional = Optional.of(topology);
        when(checkedFuture.get()).thenReturn(optional);
        listener = new LacpDataListener(dataBroker);
    }
    @Test
    public void testRegisterDataChangeListener() throws Exception
    {
        listener.registerDataChangeListener();
        verify(dataBroker, times(1)).registerDataChangeListener(any(LogicalDatastoreType.class),
                                       any(InstanceIdentifier.class), any(DataChangeListener.class), any(DataChangeScope.class));
    }
    @Test
    public void testCheckExternalNodeConn() throws Exception
    {
        InstanceIdentifier id = Mockito.mock(InstanceIdentifier.class);
        boolean res = listener.checkExternalNodeConn(id);
        assertEquals(true, res);
    }
    @Test
    public void testOnDataChanged() throws Exception
    {
        listener.onDataChanged(null);
    }
    @Test
    public void testOnDataChanged1() throws Exception
    {
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChange = Mockito.mock(AsyncDataChangeEvent.class);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getRemovedPaths()).thenReturn(null);
        when(dataChange.getOriginalData()).thenReturn(null);
        listener.onDataChanged(dataChange);
    }
    @Test
    public void testOnDataChanged2() throws Exception
    {
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
        listener.onDataChanged(dataChange);
    }
    @Test
    public void testOnDataChanged3() throws Exception
    {
        Link link1 = new LinkBuilder().setLinkId(new LinkId("host:1"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:1")).setDestTp(new TpId("host:1")).build())
            .build();
        Link link2 = new LinkBuilder().setLinkId(new LinkId("host:2"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:2")).setDestTp(new TpId("host:2")).build())
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
        listener.onDataChanged(dataChange);
    }
    @Test
    public void testOnDataChanged4() throws Exception
    {
        Link link1 = new LinkBuilder().setLinkId(new LinkId("host:1"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:1")).setDestTp(new TpId("host:1")).build())
            .build();
        Link link2 = new LinkBuilder().setLinkId(new LinkId("host:2"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:2")).setDestTp(new TpId("host:2")).build())
            .build();
        
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChange = Mockito.mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> original = new HashMap<InstanceIdentifier<?>, DataObject>();
        InstanceIdentifier<Link> id1 = InstanceIdentifier.create(Link.class);
        InstanceIdentifier<Link> id2 = InstanceIdentifier.create(Link.class);
        original.put(id1, link1);
        original.put(id2, link2);
        Set<InstanceIdentifier<?>> remove = new HashSet<InstanceIdentifier<?>>();
        remove.add(id1);
        remove.add(id2);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getRemovedPaths()).thenReturn(remove);
        when(dataChange.getOriginalData()).thenReturn(original);
        listener.onDataChanged(dataChange);
    }
    @Test
    public void testOnDataChanged5() throws Exception
    {
        Link link1 = new LinkBuilder().setLinkId(new LinkId("host:1"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:1")).setDestTp(new TpId("host:1")).build())
            .build();
        Link link2 = new LinkBuilder().setLinkId(new LinkId("host:2"))
            .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId("host:2")).setDestTp(new TpId("host:2")).build())
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
        listener.onDataChanged(dataChange);

        Map<InstanceIdentifier<?>, DataObject> original = new HashMap<InstanceIdentifier<?>, DataObject>();
        original.put(id1, link1);
        Set<InstanceIdentifier<?>> remove = new HashSet<InstanceIdentifier<?>>();
        remove.add(id1);
        when(dataChange.getCreatedData()).thenReturn(null);
        when(dataChange.getRemovedPaths()).thenReturn(remove);
        when(dataChange.getOriginalData()).thenReturn(original);
        listener.onDataChanged(dataChange);
    }




}

