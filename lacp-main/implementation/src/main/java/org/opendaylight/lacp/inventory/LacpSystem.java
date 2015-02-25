/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LacpSystem 
{
    private static final ConcurrentHashMap<InstanceIdentifier, LacpNodeExtn> lacpNodeMap = new ConcurrentHashMap<InstanceIdentifier, LacpNodeExtn>();
    private static final LacpSystem lacpSystem = new LacpSystem ();
    private LacpSystem ()
    {
    }    
    public static LacpSystem getLacpSystem ()
    {
        return lacpSystem;
    }    
    public void addLacpNode (InstanceIdentifier nodeId, LacpNodeExtn lacpNode)
    {
        lacpNodeMap.put (nodeId, lacpNode);
        return;
    }
    public LacpNodeExtn removeLacpNode (InstanceIdentifier nodeId)
    {
        return (lacpNodeMap.remove (nodeId));
    }
    public LacpNodeExtn getLacpNode (InstanceIdentifier nodeId)
    {
        return (lacpNodeMap.get (nodeId));
    }
    public void clearLacpMap ()
    {
        lacpNodeMap.clear ();
        return;
    }
    public Collection<LacpNodeExtn> getAllLacpNodes ()
    {
        Collection<LacpNodeExtn> nodeList = lacpNodeMap.values ();
        return nodeList;
    }
}
