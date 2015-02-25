/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNodeBuilder;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;


enum PortType {
    NONE, 
    LACP_PORT, 
    NON_LACPPORT;
}

public class LacpNodeExtn extends LacpNodeBuilder {
    private static final Logger log = LoggerFactory.getLogger(LacpNodeExtn.class);
    private Hashtable<InstanceIdentifier, LacpPort> lacpPortList;
    private List<InstanceIdentifier> nonLacpPortList;
    private boolean  deleteStatus;
    
    /* queue id or ref to be added for packet queue and timer queue */
    public LacpNodeExtn (InstanceIdentifier nodeId, MacAddress systemId, int sysPriority)
    {
        this.setSystemId (systemId);
        this.setSystemPriority (sysPriority);
        this.nonLacpPortList = new ArrayList<>();
        this.lacpPortList = new Hashtable ();
        this.deleteStatus = false;
    }
    public void addNonLacpPort (InstanceIdentifier port)
    {
        this.nonLacpPortList.add (port);
        return;
    }
    public void addLacpPort (InstanceIdentifier portId, LacpPort lacpPort)
    {
        this.lacpPortList.put (portId, lacpPort);
        return;
    }
    public boolean removeNonLacpPort (InstanceIdentifier port)
    {
        return (this.nonLacpPortList.remove (port));
    }
    public LacpPort removeLacpPort (InstanceIdentifier portId)
    {
        return (this.lacpPortList.remove (portId));
    }
    public PortType containsPort (InstanceIdentifier port)
    {
        if (this.nonLacpPortList.contains (port) == true)
            return PortType.NON_LACPPORT;
        else if (this.lacpPortList.containsKey (port) == true)
            return PortType.LACP_PORT;
        else
            return PortType.NONE;
    }
    public void deletePort (InstanceIdentifier port)
    {
        PortType pType = this.containsPort (port);
        if (pType.equals (PortType.NONE))
        {
            log.error ("got a a nodeConnector removal for non-existing nodeConnector {} ", port);
        }
        else if (pType.equals (PortType.LACP_PORT))
        {
            this.removeLacpPort (port);
        }
        else
        {
            this.removeNonLacpPort (port);
        }
    }
    public void deleteLacpNode (boolean delFlag)
    {
       /* If delFlag is false, do only the cleanup
        * else, delete the information from datastore also. */
        this.deleteStatus = true;
        if (delFlag == true)
        {
            /* clean up in switch */
        }
   }    
    public void createRSM ()
    {  
    }
}
