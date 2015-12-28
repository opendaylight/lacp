/**
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.lacp.role;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpEntityManager {
	private static final Logger LOG = LoggerFactory.getLogger(LacpEntityManager.class);
	private EntityOwnershipService entityOwnershipService;
	private EntityOwnershipCandidateRegistration entityRegistration;
        private EntityOwnershipListenerRegistration entityOwnershipListenerRegistration;
	private final LacpOwnershipListener ownershipListener;
	private final AtomicBoolean isListenerRegistered = new AtomicBoolean();

	public LacpEntityManager( EntityOwnershipService entityOwnershipService ) {
		this.entityOwnershipService = entityOwnershipService;
		ownershipListener = new LacpOwnershipListener(this);
	}

	public void requestLacpEntityOwnership(String appName) {

		final Entity entity = new Entity(appName, appName);
                try {
                        LOG.debug("requestLacpEntityOwnership: Before Calling registerCandidate", entity);
                        entityRegistration = entityOwnershipService.registerCandidate(entity);
                        LOG.debug("requestLacpEntityOwnership: After Calling registerCandidate", entity);
                } catch (CandidateAlreadyRegisteredException e) {
                        LOG.warn("Candidate - Entity already registered with LACP candidate ", entity, e );
                }

                try {
		    if (isListenerRegistered.compareAndSet(false, true)) {
			entityOwnershipListenerRegistration = entityOwnershipService.registerListener(appName, ownershipListener);
		    }

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
                } catch (Exception e){
                    LOG.warn("Exception while registering listener with entityOwnershipService ", appName , e );
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

        public void closeListeners()
        {
            LOG.debug("closeListeners : calling entityRegistration close method");
            if(entityRegistration != null) {
                entityRegistration.close();
            }
            if(entityOwnershipListenerRegistration != null) {
                entityOwnershipListenerRegistration.close();
            }
        }
}
