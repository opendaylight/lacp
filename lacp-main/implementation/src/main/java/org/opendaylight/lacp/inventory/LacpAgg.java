/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.lacp.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsBuilder;

public class LacpAgg
{
    private static final Logger log = LoggerFactory.getLogger(LacpAgg.class);
    private InstanceIdentifier aggInstId;
    private LacpAggregatorsBuilder lacpAggBuilder;
    private int  aggSpeed;
    private int  aggLacpMode;
    private int  aggLacpTimeout;
    private InstanceIdentifier logNodeConnRef;
    
    public InstanceIdentifier getAggInsIdentifier ()
    {
        return this.aggInstId;
    }
    public Integer getAggId ()
    {
        return lacpAggBuilder.getAggId();
    }
    public LacpAggregators buildLacpAgg ()
    {
        return lacpAggBuilder.build();
    }
    public LacpAgg (LacpAggregators lacpAggregator)
    {
        lacpAggBuilder = new LacpAggregatorsBuilder(lacpAggregator);
        // fill others here

    }
    public LacpAgg ()
    {
        
    }



}
