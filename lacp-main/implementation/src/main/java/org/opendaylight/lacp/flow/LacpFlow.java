/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import com.google.common.base.Preconditions;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
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
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.util.LacpUtil;


public class LacpFlow
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpFlow.class);
    private static SalFlowService salFlow;
    private AtomicLong lacpFlowId = new AtomicLong();
    private AtomicLong lacpCookie = new AtomicLong(0x3000000000000000L);
    private static short flowTableId;
    private static int flowPriority;
    private static int flowHardTimeout;
    private static int flowIdleTimeout;
    
    public void setSalFlowService (SalFlowService salFlowService)
    {
        Preconditions.checkNotNull(salFlowService, "salFlowService should not be null.");
        this.salFlow = salFlowService;
    }

    public void setLacpFlowTableId (short flowTableId)
    {
        this.flowTableId = flowTableId;
        return;
    }
    public void setLacpFlowPriority (int flowPrio)
    {
        this.flowPriority = flowPrio;
        return;
    }
    public void setLacpFlowIdleTime (int idleTimeout)
    {
        this.flowIdleTimeout = idleTimeout;
        return;
    }
    public void setLacpFlowHardTime (int hardTimeout)
    {
        this.flowHardTimeout = hardTimeout;
        return;
    }

    public void programLacpFlow (InstanceIdentifier nodeId, LacpNodeExtn lacpNode)
    {
        TableKey tableKey = new TableKey(flowTableId);
        InstanceIdentifier <Table> tableId = nodeId.builder().augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
        Long nodeFlowId = lacpFlowId.getAndIncrement();
        FlowId flowId = new FlowId(String.valueOf(nodeFlowId));
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier <Flow> lacpFId = tableId.child(Flow.class, flowKey);
        Flow lacpFlow = createLacpFlow();

        final AddFlowInputBuilder builder = new AddFlowInputBuilder(lacpFlow);
        builder.setNode(new NodeRef(nodeId));
        builder.setFlowRef(new FlowRef(lacpFId));
        builder.setFlowTable(new FlowTableRef(tableId));
        builder.setTransactionUri(new Uri(lacpFlow.getId().getValue()));
        try
        {
            Future<RpcResult<AddFlowOutput>> result = salFlow.addFlow(builder.build());
            if (result != null)
            {
            if (result.get(5, TimeUnit.SECONDS).isSuccessful() == true)
            {
                LOG.debug("LACP Pdu to controller flow programmed to the node {}", lacpNode);
                lacpNode.setFlowId(nodeFlowId);
            }
            else
            {
                LOG.error("LACP Pdu to controller flow progamming failed for node {}", lacpNode);
            }
            }
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            LOG.debug("received interrupt in lacp flow programming " + e.getMessage());
        }
    }

    private Flow createLacpFlow ()
    {
        FlowBuilder lFlow = new FlowBuilder().setTableId(flowTableId).setFlowName("lacpToCntrl");

        lFlow.setId(new FlowId(Long.toString(lFlow.hashCode())));
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                                .setType(new EtherType(Long.valueOf(LacpUtil.LACP_ETHTYPE))).build())
                .setEthernetDestination(new EthernetDestinationBuilder()
                                .setAddress(LacpUtil.LACP_MAC).build());

        Match match = new MatchBuilder()
                .setEthernetMatch(ethernetMatchBuilder.build()).build();

        Action action = new ActionBuilder().setOrder(0).setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                                .setOutputAction(new OutputActionBuilder()
                                        .setMaxLength(Integer.valueOf(0xffff))
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

    public void removeLacpFlow (InstanceIdentifier nodeId, LacpNodeExtn lacpNode)
    {
        TableKey tableKey = new TableKey(flowTableId);
        InstanceIdentifier <Table> tableId = nodeId.builder().augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
        Long nodeFlowId = lacpNode.getFlowId();
        FlowId flowId = new FlowId(String.valueOf(nodeFlowId));
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier <Flow> lacpFId = tableId.child(Flow.class, flowKey);
        Flow lacpFlow = createLacpFlow();

        final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(lacpFlow);
        builder.setNode(new NodeRef(nodeId));
        builder.setFlowRef(new FlowRef(lacpFId));
        builder.setFlowTable(new FlowTableRef(tableId));
        builder.setTransactionUri(new Uri(lacpFlow.getId().getValue()));
        Future<RpcResult<RemoveFlowOutput>> result = salFlow.removeFlow(builder.build());
        try
        {
            if (result.get(5, TimeUnit.SECONDS).isSuccessful() == true)
            {
                LOG.debug("LACP Pdu to controller flow removed from node {}", lacpNode);
            }
            else
            {
                LOG.error("LACP Pdu to controller flow removal failed for node {}", lacpNode);
            }
        }
        catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            LOG.debug("received interrupt in lacp flow removal " + e.getMessage());
        }
    }
}


