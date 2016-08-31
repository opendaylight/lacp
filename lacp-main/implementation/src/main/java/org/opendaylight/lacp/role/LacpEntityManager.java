/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.role;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpEntityManager {
    private static final Logger LOG = LoggerFactory.getLogger(LacpEntityManager.class);
    private EntityOwnershipService entityOwnershipService;
    private EntityOwnershipListenerRegistration entityOwnershipListenerRegistration;
    private final LacpOwnershipListener ownershipListener;
    private static final String APP_NAME = "org.opendaylight.mdsal.ServiceEntityType";

    public LacpEntityManager( EntityOwnershipService entityOwnershipService ) {
        this.entityOwnershipService = Preconditions.checkNotNull(entityOwnershipService,
            "EntityOwnershipService should not be null.");
        ownershipListener = new LacpOwnershipListener(this);
        LOG.debug("LACP registering listener for {} app", APP_NAME);
        entityOwnershipListenerRegistration = entityOwnershipService.registerListener(APP_NAME, ownershipListener);
    }

    public void onRoleChanged(EntityOwnershipChange ownershipChange) {
        final Entity entity = ownershipChange.getEntity();
        InstanceIdentifier<Node> nodeId = obtainNodeIdFromEntity(entity);
        LacpSystem lacpSystem = LacpSystem.getLacpSystem();
        if (ownershipChange.isOwner()) {
            LOG.info("onRoleChanged: Lacp BECAME MASTER - {} " , entity);
            LOG.debug("starting to read from data store");
            lacpSystem.addMasterNotifiedNode(nodeId);
        } else {
            LOG.info("onRoleChanged: BECAME SLAVE - {} ", entity);
            lacpSystem.handleLacpNodeRemoval(nodeId);
        }
    }

    public void closeListeners() {
        LOG.debug("closeListeners : calling entityRegistration close method");
        if(entityOwnershipListenerRegistration != null) {
            entityOwnershipListenerRegistration.close();
        }
    }

    private InstanceIdentifier<Node> obtainNodeIdFromEntity(Entity entity) {
        LOG.debug ("in obtainNodeIdFromEntity for entity {}", entity.getId());
        NodeIdentifierWithPredicates node = (NodeIdentifierWithPredicates)entity.getId().getLastPathArgument();
        String nodeValue = (String)node.getKeyValues().values().iterator().next();
        InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(nodeValue))).toInstance();
        LOG.debug("nodeid returned for entity {}", nodeId);
        return nodeId;
    }

}
