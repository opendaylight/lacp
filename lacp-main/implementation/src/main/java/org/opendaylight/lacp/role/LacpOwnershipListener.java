/**
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.role;

import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;

public class LacpOwnershipListener implements EntityOwnershipListener {
    private final LacpEntityManager entManager;

    public LacpOwnershipListener(LacpEntityManager entManager) {
        this.entManager = entManager;
    }

    @Override
    public void ownershipChanged(EntityOwnershipChange ownershipChange) {
        this.entManager.onRoleChanged(ownershipChange);
    }
}
