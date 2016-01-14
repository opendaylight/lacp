/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.role;

import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LacpEntityManagerTest {
    @MockitoAnnotations.Mock
    private EntityOwnershipService service;
    @MockitoAnnotations.Mock
    private DataBroker dataBroker;
    LacpEntityManager entityManager;
    EntityOwnershipListenerRegistration registration;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        LacpUtil.setDataBrokerService(dataBroker);
        registration = Mockito.mock(EntityOwnershipListenerRegistration.class);
        when (service.registerListener(any(String.class),any(LacpOwnershipListener.class))).thenReturn(registration);
        entityManager = new LacpEntityManager(service);
    }

    @Test
    public void testLacpOwnership() throws Exception {
        LacpEntityManager entityMgr = Mockito.mock(LacpEntityManager.class);
        EntityOwnershipChange change = Mockito.mock(EntityOwnershipChange.class);
        LacpOwnershipListener listener = new LacpOwnershipListener(entityMgr);
        listener.ownershipChanged(change);
        verify(entityMgr).onRoleChanged(change);
    }

    @Test
    public void testCloseListener() throws Exception {
        entityManager.closeListeners();
        verify(registration).close();
    }

    @Test
    public void testRoleChanged() throws Exception {
        EntityOwnershipChange change = Mockito.mock(EntityOwnershipChange.class);
        Entity entity = new Entity("openflow", "openflow:1");
        when(change.getEntity()).thenReturn(entity);
        when(change.isOwner()).thenReturn(false);
        entityManager.onRoleChanged(change);

        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(null);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(change.getEntity()).thenReturn(entity);
        when(change.isOwner()).thenReturn(true);
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, new NodeKey(new NodeId("openflow:1"))).toInstance();

        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        lacpSystem.checkMasterNotificaitonForNode(nodeId);
        entityManager.onRoleChanged(change);
        verify(dataBroker).newReadOnlyTransaction();
    }
}
