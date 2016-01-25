/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
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
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.aggregator.rev151125.Lacpaggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.LacpNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev151125.LagPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.node.rev150131.lag.node.LacpAggregators;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev151125.LacpNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.port.rev151125.LacpNodeConnectorBuilder;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpBond;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.lacp.util.LacpPortType;
import org.opendaylight.lacp.grouptbl.LacpGroupTbl;
import org.opendaylight.lacp.core.LacpSysKeyInfo;
import org.opendaylight.lacp.core.RSMManager;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class LacpNodeExtn
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpNodeExtn.class);
    private LacpNodeBuilder lacpBuilder;
    private Hashtable<Integer, LacpBond> lagList;
    private Hashtable<InstanceIdentifier<NodeConnector>, LacpPort> lacpPortList;
    private List<InstanceIdentifier<NodeConnector>> nonLacpPortList;
    private boolean firstGrpAdd;
    private Long flowId;
    private InstanceIdentifier<Node> nodeInstId;
    private static final LacpFlow LACPFLOW = new LacpFlow();
    private static DataBroker dataService;
    private Long switchId;
    private boolean deleteStatus;
    private boolean rsmStatus;
    private LacpGroupTbl groupTbl;
    private GroupId bcastGroupId;
    private Group bcastGroup;
    private int nextAggId;
    private Long groupId;
    //PortId-Bond map
    private ConcurrentHashMap  <Short, LacpBond> lacpList;
    //SysKeyInfo-Bond map
    private ConcurrentHashMap <LacpSysKeyInfo, LacpBond> lacpSysKeyList;

    private void initNode() {
        lacpBuilder = new LacpNodeBuilder();
        bcastGroup = null;
        nonLacpPortList = new ArrayList<InstanceIdentifier<NodeConnector>>();
        lacpPortList = new Hashtable<InstanceIdentifier<NodeConnector>, LacpPort>();
        lagList = new Hashtable<Integer,LacpBond>();
        groupTbl = new LacpGroupTbl (LacpUtil.getSalGroupService(), dataService);
        lacpList = new ConcurrentHashMap <Short,LacpBond>();
        lacpSysKeyList = new ConcurrentHashMap <LacpSysKeyInfo,LacpBond>();
        deleteStatus = false;
    }
    public LacpNodeExtn (InstanceIdentifier nodeId)
    {
        initNode();
        groupId = LacpUtil.getNextGroupId();
        bcastGroupId = new GroupId (groupId);
        nodeInstId = nodeId;

        switchId = LacpUtil.getNodeSwitchId(nodeId);
        String sysId = obtainSystemMac();
        lacpBuilder.setSystemId(new MacAddress(sysId));
        lacpBuilder.setSystemPriority(LacpUtil.DEF_LACP_PRIORITY);
	firstGrpAdd = true;
        lacpBuilder.setNonLagGroupid(groupId);
        ArrayList<LacpAggregators> aggList = new ArrayList<LacpAggregators>();
        lacpBuilder.setLacpAggregators(aggList);
        nextAggId =1;
    }

    public LacpNodeExtn (InstanceIdentifier nodeId, Node node) {
        LOG.debug ("creating lacpNode for {}", nodeId);
        initNode();
        LacpNode lNode = node.<LacpNode>getAugmentation(LacpNode.class);   
        groupId = lNode.getNonLagGroupid();
        bcastGroupId = new GroupId (groupId);
        nodeInstId = nodeId;
        switchId = LacpUtil.getNodeSwitchId(nodeId);
        lacpBuilder.setSystemId(lNode.getSystemId());
        lacpBuilder.setSystemPriority(lNode.getSystemPriority());

        lacpBuilder.setNonLagGroupid(groupId);
        ArrayList<LacpAggregators> aggList = new ArrayList<LacpAggregators>();
        aggList.addAll(lNode.getLacpAggregators());
        lacpBuilder.setLacpAggregators(aggList);

        List<NodeConnector> nodeConnectors = node.getNodeConnector();
        if (nodeConnectors == null) {
	    firstGrpAdd = true;
            LOG.debug ("No nodeConnectors are available for the node. Not creating lags");
            return;
        }
        for(NodeConnector nc : nodeConnectors) {
            FlowCapableNodeConnector flowConnector = nc.getAugmentation(FlowCapableNodeConnector.class);
            PortState portState = flowConnector.getState();
            if ((portState == null) || (portState.isLinkDown())) {
                continue;
            }
            InstanceIdentifier<NodeConnector> ncId = (InstanceIdentifier<NodeConnector>)
                InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nc.getKey()).build();
            NodeConnectorId nCon = InstanceIdentifier.keyOf(ncId).getId();
            if (nCon.getValue().contains("LOCAL")) {
                continue;
            }
            LagPort lPort = nc.<LacpNodeConnector>getAugmentation(LacpNodeConnector.class);
            if ((lPort != null) && (lPort.getPartnerPortNumber() != 0)) {
                LacpPort lacpPort = LacpPort.newInstance(switchId.longValue(), ncId, nc, lPort);
                lacpPortList.put (ncId, lacpPort);
            } else {
                nonLacpPortList.add(ncId);
            }
        }
        LOG.debug ("added ports non-lacp ports {}, lacp-ports {}", nonLacpPortList, lacpPortList);

        for (Lacpaggregator lag : lNode.getLacpAggregators()) {
            LacpBond bond = LacpBond.newInstance(lag, this);
            lagList.put(bond.getBondInstanceId(), bond); 
            LOG.debug ("creating lag for bondId {}", bond.getBondInstanceId());
            if (nextAggId < lag.getAggId()) {
                nextAggId = lag.getAggId() + 1;
            }
        }
        firstGrpAdd = false;
        bcastGroup = groupTbl.lacpAddGroup (false, bcastGroupId, nodeInstId,
                                            nonLacpPortList);
        LOG.debug ("reconstructed bcast group {}", groupId);
    }


    public void updateLacpNodeInfo()
    {
        synchronized (this)
        {
            LOG.debug ("programming the flow fo sw {}", switchId);
            LACPFLOW.programLacpFlow(nodeInstId, this);
        }
        return;
    }
    
    private String obtainSystemMac()
    {
        long id = this.switchId;
        String sysId = String.format("%02x:%02x:%02x:%02x:%02x:%02x", 0,
                  ((id & 0xff000000) >> 24), ((id & 0xff0000) >> 16),
                  ((id & 0xff00) >> 8), (id & 0xff), 1);

        return sysId;
    }
    public static void setDataBrokerService (DataBroker dataBroker)
    {
        Preconditions.checkNotNull(dataBroker, "DataBroker should not be null.");
        dataService = dataBroker;
    }
    public boolean addNonLacpPort (InstanceIdentifier<NodeConnector> port)
    {
        NodeConnectorId ncId = InstanceIdentifier.keyOf(port).getId();
        if (ncId.getValue().contains("LOCAL"))
        {
            /* Ignoring port updates for LOCAL port connected to the controller */
            LOG.debug ("getting a add port indication for LOCAL port. ignoring it");
            return false;
        }
        LacpPortType pType = this.containsPort(port);
        if (pType != LacpPortType.NONE)
        {
            if (pType == LacpPortType.LACP_PORT)
            {
                LOG.warn ("getting add non-lacp port for an lacp port without removing from lacp port list");
            }
            return false;
        }
        this.nonLacpPortList.add(port);
        LOG.debug("adding non lacp port {} ", port);
    	synchronized (groupTbl)
        {
            if (firstGrpAdd)
            {
                LOG.debug("creating non-lag group id {} ", groupId);
                bcastGroup = groupTbl.lacpAddGroup (false, new NodeConnectorRef(port), bcastGroupId);
                firstGrpAdd = false;
                lacpBuilder.setNonLagGroupid(groupId);
                LOG.debug("created non-lag group id {} ", groupId);
                updateLacpNodeDS(nodeInstId);
        	}
        	else
        	{
            		bcastGroup = groupTbl.lacpAddPort(false, new NodeConnectorRef(port), bcastGroup);
        	}
	    }
        updateNodeConnectorLacpInfo (port);
        return true;
    }
    public boolean addLacpPort (InstanceIdentifier<NodeConnector> portId, LacpPort lacpPort)
    {
        LacpPort lacpPortObj = this.lacpPortList.get(portId);
        if ((lacpPortObj != null) && (lacpPortObj.equals(lacpPort)))
        {
            return false;
        }
        this.lacpPortList.put (portId, lacpPort);
        return true;
    }
    public boolean removeNonLacpPort (InstanceIdentifier<NodeConnector> port)
    {
        NodeConnectorId ncId = InstanceIdentifier.keyOf(port).getId();
        if (ncId.getValue().contains("LOCAL"))
        {
            /* Ignoring port updates for LOCAL port connected to the controller */
            LOG.debug ("getting a remove port indication for LOCAL port. ignoring it");
            return false;
        }
        boolean result = nonLacpPortList.remove(port);

        LOG.debug("removing non lacp port {} result {}", port, result);
        if (result == true)
        {
            bcastGroup = groupTbl.lacpRemPort (bcastGroup, new NodeConnectorRef(port),
                                                false);
        }
        return result;
    }
    public LacpPort removeLacpPort (InstanceIdentifier<NodeConnector> portId, boolean hardReset)
    {
        LacpPort lacpPortObj = this.lacpPortList.get(portId);
        if ((lacpPortObj == null) || (!(lacpPortObj.getNodeConnectorId().equals(portId))))
        {
            return null;
        }
        LacpPort lacpPort = this.lacpPortList.remove(portId);

        if (hardReset == true)
        {
            // When port is going down, reset the lacp info available for the nodeConnector.
            updateNodeConnectorLacpInfo(portId);
        }
        return lacpPort;
    }
    public void setFlowId (Long flowId)
    {
        this.flowId = flowId;
    }
    public long getFlowId ()
    {
        return this.flowId;
    }
    public LacpPort getLacpPort (InstanceIdentifier<NodeConnector> portId)
    {
        LacpPort lacpPort = lacpPortList.get(portId);
        return lacpPort;
    }
    public LacpPortType containsPort (InstanceIdentifier<NodeConnector> port)
    {
        if (this.nonLacpPortList.contains(port) == true)
        {
            return LacpPortType.NON_LACPPORT;
        }
        else if (this.lacpPortList.containsKey(port) == true)
        {
            return LacpPortType.LACP_PORT;
        }
        else
        {
            return LacpPortType.NONE;
        }
    }
    public boolean deletePort (InstanceIdentifier<NodeConnector> port, boolean hardReset)
    {
        LacpPortType pType = this.containsPort(port);
        if (pType.equals(LacpPortType.NONE))
        {
            LOG.warn("got a a nodeConnector removal for non-existing nodeConnector {} ", port);
        }
        else if (pType.equals(LacpPortType.LACP_PORT))
        {
            if (this.removeLacpPort (port, hardReset) != null)
            {
                return true;
            }
        }
        else
        {
            return (this.removeNonLacpPort (port));
        }
        return false;
    }
    public void deleteLacpNode ()
    {
        long groupId = 0;
        InstanceIdentifier<NodeConnector> ncId = null;

        if (nonLacpPortList.size() != 0)
        {
            ncId = nonLacpPortList.get(0);
            nonLacpPortList.clear();
        }
        // add hook to remove the list of aggregators.
        lagList.clear();
        // add hook to remove the list of lacp ports.
        lacpPortList.clear();
        lacpBuilder.setNonLagGroupid(groupId);
        ArrayList<LacpAggregators> empAggList = new ArrayList<LacpAggregators>();
        lacpBuilder.setLacpAggregators(empAggList);

        if (this.deleteStatus == false)
        {
            /* clean up in switch */
            LACPFLOW.removeLacpFlow(this.nodeInstId, this);
            updateLacpNodeDS(nodeInstId);
            if (ncId != null)
            {
                groupTbl.lacpRemGroup (false, new NodeConnectorRef(ncId), bcastGroupId);
            }
        }
        lacpBuilder = null;
    }
    public void updateLacpNodeDS (InstanceIdentifier nodeId)
    {
        if (this.deleteStatus == true)
        {
            //do not update the datastore as the node is in process of deletion
            LOG.debug ("update of LACP node DS skipped");
            return;
        }
        final WriteTransaction write = dataService.newWriteOnlyTransaction();
        LacpNode lacpNode = lacpBuilder.build();
        InstanceIdentifier<LacpNode> lacpNodeId = nodeId.augmentation(LacpNode.class);
        write.merge(LogicalDatastoreType.OPERATIONAL, lacpNodeId, lacpNode);
        final CheckedFuture result = write.submit();
        Futures.addCallback(result, new FutureCallback() {
        @Override
        public void onSuccess(Object o) {
          LOG.debug("LacpNode updation write success for txt {}", write.getIdentifier());
        }

        @Override
        public void onFailure(Throwable throwable) {
          LOG.error("LacpNode updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
        }
      });
    }
    public void updateNodeBcastGroupId (Long groupId)
    {
        lacpBuilder.setNonLagGroupid(groupId);
        updateLacpNodeDS(this.nodeInstId);
    }
    public boolean addLacpAggregator (LacpBond lacpAgg)
    {
        LOG.debug ("in addLacpAggregator for bond {}", lacpAgg.getBondInstanceId());
        if (lagList.containsKey(lacpAgg.getBondInstanceId()))
        {
            LOG.debug ("addLacpAggregator: given bond {} is already available in the node {}", lacpAgg.getBondInstanceId(), switchId);
            return false;
        }

        lagList.put(lacpAgg.getBondInstanceId(), lacpAgg);
        List<LacpAggregators> aggList = lacpBuilder.getLacpAggregators();
        aggList.add(lacpAgg.buildLacpAgg());
        LOG.debug ("adding aggregator {}", lacpAgg.buildLacpAgg());
        lacpBuilder.setLacpAggregators(aggList);
        /* Aggregator list is only updated here. Aggregator DS will be
         * updated in LacpBond */
        return true;
    }
    public boolean removeLacpAggregator (LacpBond lacpAgg)
    {
        LOG.debug ("in removeLacpAggregator for node {}", switchId);
        if (!(lagList.containsKey(lacpAgg.getBondInstanceId())))
        {
            LOG.debug ("removeLacpAggregator: given bond {} is not available in the node {}", lacpAgg.getBondInstanceId(), switchId);
            return false;
        }
        lacpAgg = lagList.remove(lacpAgg.getBondInstanceId());
        List<LacpAggregators> aggList = lacpBuilder.getLacpAggregators();
        aggList.remove(lacpAgg.buildLacpAgg());
        lacpBuilder.setLacpAggregators(aggList);
        /* Aggregator list is only updated here. Aggregator DS will be
         * updated in LacpBond */
        return true;
    }
    public void updateNodeConnectorLacpInfo (InstanceIdentifier port)
    {
        short portNo = 0;
        if (this.deleteStatus == true)
        {
            //do not update the datastore as the node is in process of deletion
            LOG.debug ("update of LACP nodeConnector DS skipped");
            return;
        }
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
                LOG.debug("LacpNodeConnector updation write success for txt {}", write.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable)
            {
                LOG.error("LacpNodeConnector updation write failed for tx {}", write.getIdentifier(), throwable.getCause());
            }
        });
    }
    public Long getNodeBcastGroupId ()
    {
        return(lacpBuilder.getNonLagGroupid());
    }
    public Long getSwitchId()
    {
        return switchId;
    }
    public InstanceIdentifier getNodeId ()
    {
        return nodeInstId;
    }
    public void setLacpNodeDeleteStatus (boolean delStatus)
    {
        deleteStatus = delStatus;
        LOG.debug ("setting the delete status to {}", delStatus);
        return;
    }
    public boolean getLacpNodeDeleteStatus ()
    {
        return deleteStatus;
    }
    public MacAddress getNodeSystemId()
    {
        return this.lacpBuilder.getSystemId();
    }

    public int getAndIncrementNextAggId()
    {
        int aggId = 0;
        synchronized(this)
        {
            aggId = this.nextAggId;
            nextAggId++;
        }
        return aggId;
    }

    public LacpBond findLacpBondByPartnerMacKey(byte[] sysId, short key) {
        if (lacpList.size() == 0) {
            return null;
        }

        for (LacpBond bond: lacpList.values()) {
            if (bond.isPartnerExist(sysId,key)) {
                return bond;
            }
        }
        return null;
    }

    public LacpBond findLacpBondBySystemKey (LacpSysKeyInfo sysKeyInfo) {
        return lacpSysKeyList.get(sysKeyInfo);
    }

    public LacpBond findLacpBondByPort (short portId) {
        return lacpList.get(portId);
    }

    public LacpBond addLacpBondToPortList (short portId, LacpBond lacpBond) {
        LOG.debug("in addLacpBondToPortList for port {},{}", switchId, portId);
        LacpBond bond = lacpList.putIfAbsent(portId, lacpBond);
        return bond;
    }

    public LacpBond addLacpBondToSysKeyList (LacpSysKeyInfo sysKeyInfo, LacpBond lacpBond) {
        LacpBond bond = null;
        if (sysKeyInfo != null) {
            bond = lacpSysKeyList.putIfAbsent(sysKeyInfo, lacpBond);
        }
        return bond;
    }

    public LacpBond removeLacpBondFromPortList (short portId) {
        LOG.debug("in removeLacpBondFromPortList for port {}, {}", switchId, portId);
        return (lacpList.remove(portId));
    }

    public LacpBond removeLacpBondFromSysKeyInfo (LacpSysKeyInfo sysKeyInfo) {
        return (lacpSysKeyList.remove(sysKeyInfo));
    }

    public void bondInfoCleanup() {
        LOG.debug("bondInfoCleanup Entry");
        for (LacpBond bond: lacpList.values()) {
            LacpSysKeyInfo sysKeyInfo = bond.getActiveAggPartnerInfo();
            if (sysKeyInfo != null) {
                lacpSysKeyList.remove(sysKeyInfo);
            }
            for (LacpPort lacpPort: bond.getSlaveList()) {
                lacpList.remove(lacpPort.slaveGetPortId());
                lacpPort.lacpPortCleanup();
            }
            bond.lacpBondCleanup();
        }
        LOG.debug("bondInfoCleanup Exit");
    }

    public LacpPort getLacpPortForPortId (short portId) {
        LacpPort port = null;
        LacpBond bond = this.findLacpBondByPort(portId);
        if (bond != null)
        {
            LOG.debug("in getLacpPortForPortId bond is available");
            port = bond.getSlavePortObject(portId);
        } else
            LOG.debug("in getLacpPortForPortId bond not available");
        return port;
    }

    public void sendLacpPDUs() {
        for (LacpBond bond : lagList.values()) {
            bond.transmitLacpPDUsForPorts();
        }
    }
    public int getLacpSystemPriority() {
        return lacpBuilder.getSystemPriority();
    }
}
