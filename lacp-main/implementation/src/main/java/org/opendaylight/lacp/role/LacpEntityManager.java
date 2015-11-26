/**
 * Copyright (c) 2015, 2016 Dell.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.role;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.base.MoreObjects;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpEntityManager {
	private static final Logger LOG = LoggerFactory.getLogger(LacpEntityManager.class);
	private EntityOwnershipService entityOwnershipService;
	private EntityOwnershipCandidateRegistration entityRegistration;
	private final LacpOwnershipListener ownershipListener;
	private final AtomicBoolean isListenerRegistered = new AtomicBoolean();
	private NodeId nodeId;
        //TODO-Use existing constant or have it in common file
        private String APP_NAME = new String("LACP");

	public LacpEntityManager( EntityOwnershipService entityOwnershipService ) {
		this.entityOwnershipService = entityOwnershipService;
		ownershipListener = new LacpOwnershipListener(this);
	}

	public void requestLacpEntityOwnership(String appName) {
		if (isListenerRegistered.compareAndSet(false, true)) {
			entityOwnershipService.registerListener(appName, ownershipListener);
		}
		final Entity entity = new Entity(appName, appName);

		Optional <EntityOwnershipState> entityOwnershipStateOptional = 
								entityOwnershipService.getOwnershipState(entity);

		if (entityOwnershipStateOptional != null && entityOwnershipStateOptional.isPresent()) {
			final EntityOwnershipState entityOwnershipState = entityOwnershipStateOptional.get();
			if (entityOwnershipState.hasOwner()) {
				LOG.info("requestLacpEntityOwnership: An owner exist for entity {} ", 
						entity);
				if (entityOwnershipState.isOwner()) {
					LOG.info("requestLacpEntityOwnership: Becoming Master for entity {} ",
							entity);
				} else {
					LOG.info("requestLacpEntityOwnership: Becoming Slave for entity {} ",
							entity);
				}
			}
		}

		try {
			LOG.info("requestLacpEntityOwnership: Before Calling registerCandidate", entity);
			entityRegistration = entityOwnershipService.registerCandidate(entity);
			LOG.info("requestLacpEntityOwnership: After Calling registerCandidate", entity);
		} catch (CandidateAlreadyRegisteredException e) {
			LOG.warn("Candidate - Entity already registered with LACP candidate ", entity, e );
		}
	}

	public void onRoleChanged(EntityOwnershipChange ownershipChange) {
		final Entity entity = ownershipChange.getEntity();
		if (ownershipChange.isOwner())
		{
			LOG.info("onRoleChanged: BECAME MASTER - {} " , entity);
		}
		else
		{
			LOG.info("onRoleChanged: BECAME SLAVE - {} ", entity);
		}
	}

	public void relinquishLacpEntityOwnership()
	{
		Entity entity = new Entity(APP_NAME, APP_NAME);
		LOG.info("relinquishLacpEntityOwnership: calling entityRegistration close method");
		entityRegistration.close();
	}
}
