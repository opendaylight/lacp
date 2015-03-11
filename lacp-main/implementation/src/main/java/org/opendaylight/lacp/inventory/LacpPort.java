/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import java.util.concurrent.Future;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev150131.AggRef;

public class LacpPort
{
    private static final Logger log = LoggerFactory.getLogger(LacpPort.class);
    private LacpNodeConnectorBuilder lacpNCBuilder;
    private LacpPortParam actorAdmin;
    private LacpPortParam actorOper;
    private LacpPortParam partnerAdmin;
    private LacpPortParam partnerOper;
    private LacpAgg lacpAggRef;
    private LacpNodeExtn lacpNodeRef;
    private InstanceIdentifier ncId;
    private static DataBroker dataService;

    public LacpPort (InstanceIdentifier port, LacpPortParam actorInfo, LacpNodeExtn lacpNode)
    {
        ncId = port;
        actorAdmin = new LacpPortParam(actorInfo);
        lacpNodeRef = lacpNode;
        lacpNCBuilder = new LacpNodeConnectorBuilder();
        lacpNCBuilder.setActorPortNumber(actorInfo.getPortNumber());
        lacpNCBuilder.setPeriodicTime(LacpUtil.DEF_PERIODIC_TIME);
        updateNCLacpInfo();
    }
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataService = dataBroker;
    }
    public void updateNCLacpInfo ()
    {  
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNodeConnector lacpNC = lacpNCBuilder.build();
        InstanceIdentifier<LacpNodeConnector> lacpNCId = ncId.augmentation(LacpNodeConnector.class);
        write.merge(LogicalDatastoreType.OPERATIONAL, lacpNCId, lacpNC);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess (Object o)
            {
                log.debug("LacpNodeConnector updation write success for txt {}", write.getIdentifier());
            }

            @Override
            public void onFailure (Throwable throwable)
            {
                log.error("LacpNodeConnector updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
    }
    public void updateLacpAggregator (LacpAgg lacpAgg)
    {
        lacpAggRef = lacpAgg;
        lacpNCBuilder.setLacpAggRef(new AggRef(lacpAgg.getAggInsIdentifier()));
        updateNCLacpInfo();
    }
    public void updatePartnerInfo (LacpPortParam partnerInfo)
    {
        partnerAdmin = new LacpPortParam(partnerInfo);
        lacpNCBuilder.setPartnerPortNumber(partnerInfo.getPortNumber());
    }
    public LacpPortParam getActorAdminInfo ()
    {
        return this.actorAdmin;
    }
    public LacpPortParam getActorOperInfo ()
    {
        return this.actorOper;
    }
    public LacpPortParam getPartnerAdminInfo ()
    {
        return this.partnerAdmin;
    }
    public LacpPortParam getPartnerOperInfo ()
    {
        return this.partnerOper;
    }
    public void setAdminOperInfo (LacpPortParam adminInfo)
    {
        actorOper = adminInfo;
    }
    public void setPartnerOperInfo (LacpPortParam partnerInfo)
    {
        partnerOper = partnerInfo;
    }
}
