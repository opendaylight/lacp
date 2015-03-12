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
import java.util.Collection;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev150131.LacpNodeConnectorBuilder;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.util.LacpPortType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import java.util.concurrent.Future;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class LacpNodeExtn 
{
    private static final Logger log = LoggerFactory.getLogger(LacpNodeExtn.class);
    private LacpNodeBuilder lacpBuilder;
    private Hashtable<Integer, LacpAgg> lagList;
    private Hashtable<InstanceIdentifier<NodeConnector>, LacpPort> lacpPortList;
    private List<InstanceIdentifier<NodeConnector>> nonLacpPortList;
    private Long FlowId;
    private InstanceIdentifier<Node> nodeId;
    private static final LacpFlow lacpFlow = new LacpFlow();
    private static DataBroker dataService;
    private static AtomicInteger sysIden = new AtomicInteger(0);
    private static final int MAX_IDEN = 0xff000000;
    private int swId; // int id for the node.
    private boolean  deleteStatus;
    
    public LacpNodeExtn (InstanceIdentifier nodeId)
    {
        long groupId = 0;
        nodeId = nodeId;
        lacpBuilder = new LacpNodeBuilder();
        
        swId = obtainSwitchId();
        String sysId = obtainSystemMac();
        lacpBuilder.setSystemId(new MacAddress(sysId));
        lacpBuilder.setSystemPriority(LacpUtil.DEF_LACP_PRIORITY);
        nonLacpPortList = new ArrayList<InstanceIdentifier<NodeConnector>>();
        lacpPortList = new Hashtable<InstanceIdentifier<NodeConnector>, LacpPort>();
        deleteStatus = false;
        lacpFlow.programLacpFlow(nodeId, this);
        lacpBuilder.setNonLagGroupid(groupId);
        lagList = new Hashtable<Integer,LacpAgg>();
        ArrayList<LacpAggregators> aggList = new ArrayList<LacpAggregators>();
        lacpBuilder.setLacpAggregators(aggList);
        updateLacpNodeDS(nodeId);
    }
    private int obtainSwitchId()
    {
        sysIden.compareAndSet(MAX_IDEN, 0);
        return (sysIden.incrementAndGet());
    }
    private String obtainSystemMac()
    {
        int id = this.swId;
        String sysId = String.format("%02x:%02x:%02x:%02x:%02x:%02x", 0, 0, (id & 0xff0000),
                                      (id & 0xff00), (id & 0xff), 1);

        return sysId;
    }

    public LacpNodeExtn (InstanceIdentifier nodeId, MacAddress systemId, int sysPriority, Long groupId, ArrayList<LacpAggregators> aggList)
    {
        LacpAgg lacpAgg;

        nodeId = nodeId;
        lacpBuilder = new LacpNodeBuilder();
        swId = obtainSwitchId();
        lacpBuilder.setSystemId(systemId);
        lacpBuilder.setSystemPriority(sysPriority);
        nonLacpPortList = new ArrayList<InstanceIdentifier<NodeConnector>>();
        lacpPortList = new Hashtable<InstanceIdentifier<NodeConnector>, LacpPort>();
        deleteStatus = false;
        lacpFlow.programLacpFlow(nodeId, this);
        lacpBuilder.setNonLagGroupid(groupId);

        for (LacpAggregators lacpAggregator: aggList)
        {
            lacpAgg = new LacpAgg(lacpAggregator);
            lagList.put(lacpAgg.getAggId(), lacpAgg);
        }
       // ArrayList<LacpAggregators> lacpAggList = new ArrayList<LacpAggregators>(lagList.values());
        lacpBuilder.setLacpAggregators(aggList);
        updateLacpNodeDS(nodeId);
    }
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataService = dataBroker;
    }
    public boolean addNonLacpPort (InstanceIdentifier<NodeConnector> port)
    {
        if (this.nonLacpPortList.contains(port))
        {
            return false;
        }
        updateNodeConnectorLacpInfo (port);
        this.nonLacpPortList.add(port);
        return true;
    }
    public boolean addLacpPort (InstanceIdentifier<NodeConnector> portId, LacpPort lacpPort)
    {
        LacpPort lacpPortObj = this.lacpPortList.get(portId);
        if ((lacpPortObj != null) && (lacpPortObj.equals(portId)))
        {
            return false;
        }
        this.lacpPortList.put (portId, lacpPort);
        return true;
    }
    public boolean removeNonLacpPort (InstanceIdentifier<NodeConnector> port)
    {
        if (lacpBuilder.getNonLagGroupid() != 0)
        {
            // add hook to remove the port from the non-lag group buckets
        }
        return (nonLacpPortList.remove(port));
    }
    public LacpPort removeLacpPort (InstanceIdentifier<NodeConnector> portId, boolean hardReset)
    {
        LacpPort lacpPort = lacpPortList.remove(portId);
        //lacpPort.delete(true);

        if (hardReset == true)
        {
            // When port is going down, reset the lacp info available for the nodeConnector.
            updateNodeConnectorLacpInfo(portId);
        }
        return lacpPort;
    }
    public void setFlowId (Long flowId)
    {
        this.FlowId = flowId;
    }
    public long getFlowId ()
    {
        return this.FlowId;
    }
    public LacpPortType containsPort (InstanceIdentifier<NodeConnector> port)
    {
        if (this.nonLacpPortList.contains(port) == true)
            return LacpPortType.NON_LACPPORT;
        else if (this.lacpPortList.containsKey(port) == true)
            return LacpPortType.LACP_PORT;
        else
            return LacpPortType.NONE;
    }
    public boolean deletePort (InstanceIdentifier<NodeConnector> port, boolean hardReset)
    {
        LacpPortType pType = this.containsPort(port);
        if (pType.equals(LacpPortType.NONE))
        {
            log.error("got a a nodeConnector removal for non-existing nodeConnector {} ", port);
        }
        else if (pType.equals(LacpPortType.LACP_PORT))
        {
            if (this.removeLacpPort (port, hardReset) != null)
                return true;
        }
        else
        {
            return (this.removeNonLacpPort (port));
        }
        return false;
    }
    public void deleteLacpNode (boolean delFlag)
    {
       /* If delFlag is false, do only the cleanup
        * else, delete the information from datastore also. */
        long groupId = 0;
        this.deleteStatus = true;

        nonLacpPortList.clear();
        // add hook to remove the list of aggregators.
        Collection<LacpAgg> aggList = lagList.values();
        for (LacpAgg lacpAgg : aggList)
        {
            //lacpAgg.delete();
            //if (delFlag == true)
            {
                // irrespective of delFlag status, remove the lag group entry.
            }
        }
        //add hook to remove lag bcast group entry 
        lagList.clear();
        // add hook to remove the list of lacp ports.
        Collection<LacpPort> portList = lacpPortList.values();
        for (LacpPort lacpPort : portList)
        {
            //lacpPort.delete(false);
        }
        lacpPortList.clear();
        lacpBuilder.setNonLagGroupid(groupId);
        ArrayList<LacpAggregators> empAggList = new ArrayList<LacpAggregators>();
        lacpBuilder.setLacpAggregators(empAggList);

        if (delFlag == true)
        {
            /* clean up in switch */
            lacpFlow.removeLacpFlow(this.nodeId, this);
            updateLacpNodeDS(nodeId);
        }
        lacpBuilder = null;
       /*
        rsmThread.retainThread = false;
        */
    }    
    public void createRSM ()
    {  
        /* 
        rsmThread = new RSMThread();
        Thread rsmThreadId = new Thread(rsmThread);
        rsmThreadId.start();     
        */
    }
    public void updateLacpNodeDS (InstanceIdentifier nodeId)
    {
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNode lacpNode = lacpBuilder.build();
        InstanceIdentifier<LacpNode> lacpNodeId = nodeId.augmentation(LacpNode.class);
        write.merge(LogicalDatastoreType.OPERATIONAL, lacpNodeId, lacpNode);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback() {
        @Override
        public void onSuccess(Object o) {
          log.debug("LacpNode updation write success for txt {}", write.getIdentifier());
        }

        @Override
        public void onFailure(Throwable throwable) {
          log.error("LacpNode updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
        }
      });
    }
    public void updateNodeBcastGroupId (Long groupId)
    {
        lacpBuilder.setNonLagGroupid(groupId);
        System.out.println ("got groupid "+ groupId +"set value" +lacpBuilder.getNonLagGroupid());
        updateLacpNodeDS(this.nodeId);
    }
    public boolean addLacpAggregator (LacpAgg lacpAgg)
    {
        if (lagList.containsKey(lacpAgg.getAggId()))
            return false;
        
        lagList.put(lacpAgg.getAggId(), lacpAgg);
        List<LacpAggregators> aggList = lacpBuilder.getLacpAggregators();
        aggList.add(lacpAgg.buildLacpAgg());
        lacpBuilder.setLacpAggregators(aggList);
        updateLacpNodeDS(this.nodeId);
        return true;
    }
    public boolean removeLacpAggregator (LacpAgg lacpAgg)
    {
        if (!(lagList.containsKey(lacpAgg.getAggId())))
            return false;
        
        lacpAgg = lagList.remove(lacpAgg.getAggId());
        List<LacpAggregators> aggList = lacpBuilder.getLacpAggregators();
        aggList.remove(lacpAgg.buildLacpAgg());
        lacpBuilder.setLacpAggregators(aggList);
        updateLacpNodeDS(this.nodeId);
        return true;
    }
    public void updateNodeConnectorLacpInfo (InstanceIdentifier port)
    {
        short portNo = 0;
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNodeConnectorBuilder lacpNCBuilder = new LacpNodeConnectorBuilder();
        lacpNCBuilder.setActorPortNumber(portNo);
        lacpNCBuilder.setPartnerPortNumber(portNo);
        lacpNCBuilder.setPeriodicTime(LacpUtil.DEF_PERIODIC_TIME);
        LacpNodeConnector lacpNC = lacpNCBuilder.build();
        InstanceIdentifier<LacpNodeConnector> lacpNCId = port.augmentation(LacpNodeConnector.class);

        write.merge(LogicalDatastoreType.OPERATIONAL, lacpNCId, lacpNC);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback()
        {
            @Override
            public void onSuccess(Object o)
            {
                log.debug("LacpNodeConnector updation write success for txt {}", write.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable)
            {
                log.error("LacpNodeConnector updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
    }
    public Long getNodeBcastGroupId ()
    {
        return(lacpBuilder.getNonLagGroupid());
    }
    public int getSwitchId()
    {
        return swId;
    }
    public InstanceIdentifier getNodeId ()
    {
        return nodeId;
    }
}
