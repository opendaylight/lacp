/*
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventoryListener;

import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;

enum EventType
{
    Updated,
    Deleted;
}

public class LacpNodeListener implements OpendaylightInventoryListener
{
    private static final Logger log = LoggerFactory.getLogger(LacpNodeListener.class);
    private final ExecutorService lacpService = Executors.newCachedThreadPool();
    private final SalFlowService salFlow;
    private AtomicLong lacpFlowId = new AtomicLong();
    private AtomicLong lacpCookie = new AtomicLong(0x3000000000000000L);
    private short flowTableId;
    private int flowPriority;
    private int flowHardTimeout;
    private int flowIdleTimeout;
    public static final int LACP_ETHTYPE = 34825;
    public static final MacAddress LACPMAC = new MacAddress ("01:80:c2:00:00:02");
    private LacpSystem lacpSystem;

    public LacpNodeListener(SalFlowService salFlowService)
    {
        Preconditions.checkNotNull(salFlowService, "dataBrokerService should not be null.");
        this.salFlow = salFlowService;
        this.lacpSystem = LacpSystem.getLacpSystem ();
    }
    public void setLacpFlowTableId(short flowTableId)
    {
        this.flowTableId = flowTableId;
        return;
    }
    public void setLacpFlowPriority(int flowPrio)
    {
        this.flowPriority = flowPrio;
        return;
    }
    public void setLacpFlowIdleTime(int idleTimeout)
    {
        this.flowIdleTimeout = idleTimeout;
        return;
    }
    public void setLacpFlowHardTime(int hardTimeout)
    {
        this.flowHardTimeout = hardTimeout;
        return;
    }

    @Override
    public void onNodeConnectorRemoved (NodeConnectorRemoved nodeConnectorRemoved)
    {
        if (nodeConnectorRemoved == null)
            return;
        log.info ("got a node connec removed in lacp {} ", nodeConnectorRemoved);
        InstanceIdentifier<NodeConnector> ncId = (InstanceIdentifier<NodeConnector>)nodeConnectorRemoved.getNodeConnectorRef().getValue();
        lacpService.submit (new LacpNodeConnectorUpdate (ncId, EventType.Deleted));
    }

    @Override
    public void onNodeConnectorUpdated (NodeConnectorUpdated nodeConnectorUpdated)
    {
        if (nodeConnectorUpdated == null)
            return;
        log.info ("got a node connec Updated {} in lacp ", nodeConnectorUpdated);
        InstanceIdentifier<NodeConnector> ncId = (InstanceIdentifier<NodeConnector>)nodeConnectorUpdated.getNodeConnectorRef().getValue();
        lacpService.submit (new LacpNodeConnectorUpdate (ncId, EventType.Updated));
    }

    @Override
    public void onNodeRemoved (NodeRemoved nodeRemoved)
    {
        if (nodeRemoved == null)
            return;
        log.info ("got a node removed {} in lacp ", nodeRemoved);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeRemoved.getNodeRef().getValue();
        lacpService.submit (new LacpNodeUpdate (nodeId, EventType.Deleted));
    }

    /**
     * Called when a node gets updated.
     * @param nodeUpdated Notification for when a node gets updated.
     */
    @Override
    public void onNodeUpdated (NodeUpdated nodeUpdated) {
        if (nodeUpdated == null)
            return;
        log.info("got a node updated {} ", nodeUpdated);
        InstanceIdentifier <Node> nodeId = (InstanceIdentifier<Node>) nodeUpdated.getNodeRef().getValue();
        lacpService.submit (new LacpNodeUpdate (nodeId, EventType.Updated));
    }
    private class LacpNodeUpdate implements Runnable
    {
        private InstanceIdentifier<Node> lNode;
        private EventType event;

        public LacpNodeUpdate (InstanceIdentifier<Node> node, EventType evt)
        {
            lNode = node;
            event = evt;
        }
        @Override
        public void run ()
        {  
            if (event.equals (EventType.Updated) == true)
            {
                handleNodeUpdate (lNode);
            }
            if (event.equals (EventType.Deleted) == true)
            {
                handleNodeDeletion (lNode);
            }
        }

        private void handleNodeUpdate (InstanceIdentifier<Node> lNode)
        {
            String sysId = "";
            int    sysPriority = 32768;
            InstanceIdentifier <Node> nodeId = lNode;
            LacpNodeExtn lacpNode = new LacpNodeExtn (nodeId, new MacAddress (sysId), sysPriority);
            if (lacpNode == null)
            {
                log.error ("cannot add a lacp node for node {}", nodeId); 
                return;
            }
            lacpSystem.addLacpNode (nodeId, lacpNode);

            TableKey tableKey = new TableKey(flowTableId);
            InstanceIdentifier <Table> tableId = nodeId.builder().augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
            FlowId flowId = new FlowId (String.valueOf (lacpFlowId.getAndIncrement ()));
            FlowKey flowKey = new FlowKey (flowId);
            InstanceIdentifier <Flow> lacpFId = tableId.child (Flow.class, flowKey);
            Flow lacpFlow = createLacpFlow ();

            final AddFlowInputBuilder builder = new AddFlowInputBuilder (lacpFlow);
            builder.setNode (new NodeRef (nodeId));
            builder.setFlowRef (new FlowRef (lacpFId));
            builder.setFlowTable (new FlowTableRef (tableId));
            builder.setTransactionUri (new Uri (lacpFlow.getId ().getValue ()));
            try
            {
                Future<RpcResult<AddFlowOutput>> result = salFlow.addFlow (builder.build ());
                if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
                {
                    log.info ("LACP Pdu to controller flow programmed to the node {}", lacpNode);
                }
                else
                {
                    log.error ("LACP Pdu to controller flow progamming failed for node {}", lacpNode);
                }
            }
            catch (InterruptedException | ExecutionException | TimeoutException e)
            {
                log.debug ("received interrupt " + e.getMessage());
            }
        }
        private Flow createLacpFlow ()
        {
                FlowBuilder lFlow = new FlowBuilder().setTableId(flowTableId).setFlowName("lacpToCntrl");

                lFlow.setId(new FlowId(Long.toString(lFlow.hashCode())));
                EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                        .setEthernetType(new EthernetTypeBuilder()
                                        .setType(new EtherType(Long.valueOf(LACP_ETHTYPE))).build())
                        .setEthernetDestination(new EthernetDestinationBuilder()
                                        .setAddress(LACPMAC).build());

                Match match = new MatchBuilder()
                        .setEthernetMatch(ethernetMatchBuilder.build()).build();

                Action action = new ActionBuilder().setOrder(0).setKey(new ActionKey(0))
                        .setAction(new OutputActionCaseBuilder()
                                        .setOutputAction(new OutputActionBuilder()
                                                .setMaxLength(new Integer(0xffff))
                                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                                .build()).build()).build();
                ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(action)).build();

                Instruction applyActionsInstruction = new InstructionBuilder().setOrder(0)
                        .setInstruction(new ApplyActionsCaseBuilder()
                                        .setApplyActions(applyActions) 
                                        .build()).build();

                lFlow.setMatch(match).setInstructions(new InstructionsBuilder() 
                                .setInstruction(ImmutableList.of(applyActionsInstruction)).build()) 
                        .setPriority(flowPriority).setBufferId(0L) 
                        .setHardTimeout(0).setIdleTimeout(0)
                        .setCookie(new FlowCookie(BigInteger.valueOf(lacpCookie.getAndIncrement())))
                        .setFlags(new FlowModFlags(false, false, false, false, false));
                return lFlow.build();
        }
        private void handleNodeDeletion (InstanceIdentifier<Node> lNode)
        {
            InstanceIdentifier <Node> nodeId = lNode;
            LacpNodeExtn lacpNode = lacpSystem.removeLacpNode (nodeId);
            if (lacpNode == null)
            {
                log.error ("lacpNode could not be removed for node {}", nodeId); 
                return;
            }
            lacpNode.deleteLacpNode (false);
        }
    }
    private class LacpNodeConnectorUpdate implements Runnable
    {
        private InstanceIdentifier<NodeConnector> lNodeCon;
        private EventType event;

        public LacpNodeConnectorUpdate (InstanceIdentifier<NodeConnector> nc, EventType evt)
        {
            lNodeCon = nc;
            event = evt;
        }
        @Override
        public void run ()
        {  
            if (event.equals (EventType.Updated) == true)            {
                handlePortUpdate (lNodeCon);
            }
            if (event.equals (EventType.Deleted) == true)
            {
                handlePortDelete (lNodeCon);
            }
        }
        private void handlePortUpdate (InstanceIdentifier<NodeConnector> ncId)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = lacpSystem.getLacpNode (nodeId);
            if (lacpNode != null)
            {
                lacpNode.addNonLacpPort (ncId);
            }
            else
            {
                log.error ("got a a nodeConnector updation for non-existing node {} ", nodeId);
            }
        }
        private void handlePortDelete (InstanceIdentifier<NodeConnector> ncId)
        {
            InstanceIdentifier<Node> nodeId = ncId.firstIdentifierOf(Node.class);
            LacpNodeExtn lacpNode = lacpSystem.getLacpNode (nodeId);
            if (lacpNode != null)
            {
                lacpNode.deletePort (ncId);
            }
            else
            {
                log.error ("got a a nodeConnector removal for non-existing node {} ", nodeId);
            }
        }
    }
}
