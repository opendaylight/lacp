/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.inventory;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.util.LacpUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;

public class LacpNodeExtnTest 
{
	public LacpNodeExtn lacpNode;
	public LacpNodeExtn lacpNode1;
	
	short key = (short)1001;
	short portId = (short)80;
	
	private NodeId nId;
	InstanceIdentifier<Node> nodeId;
	private InstanceIdentifier<NodeConnector> ncId;
	private LacpBond lacpBond;
	private LacpPort lacpPort;
	
	@MockitoAnnotations.Mock
	private DataBroker dataBroker;
	
	@MockitoAnnotations.Mock
    private DataBroker dataService;
	
	 @MockitoAnnotations.Mock
	 private WriteTransaction write;
	 
	 @MockitoAnnotations.Mock
	 private LacpUtil lacpUtil;
		
	@MockitoAnnotations.Mock
	private NotificationProviderService notify;
		
	@Before
	public void initMocks() throws InterruptedException, ExecutionException
	{
		MockitoAnnotations.initMocks(this);
		nId = new NodeId("openflow:1");
		nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nId)).build();
		lacpNode = new LacpNodeExtn(nodeId);
		lacpBond = LacpBond.newInstance(key, lacpNode);
		
		LacpNodeExtn.setDataBrokerService(dataBroker);
        LacpUtil.setDataBrokerService(dataBroker);
				
		Augmentation<NodeConnector> aug = new Augmentation<NodeConnector>() {
		};
		
		NodeConnector nc = mock(NodeConnector.class);
		FlowCapableNodeConnector fnc = mock(FlowCapableNodeConnector.class);
		PortFeatures pf = new PortFeatures(false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false);
		when(fnc.getCurrentFeature()).thenReturn(pf);
		when(nc.getAugmentation(FlowCapableNodeConnector.class)).thenReturn(fnc);
        Optional<NodeConnector> optionalNodes = Optional.of(nc);
        
        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
        when(checkedFuture.get()).thenReturn(optionalNodes);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        
        ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
	
	}
	
	@Test 
	public void updateLacpNodeInfoTest()
	{
		lacpNode.updateLacpNodeInfo();
	}
	
	@Test
	public void addLacpAggregatorTest()
	{
		assertEquals(true, lacpNode.addLacpAggregator(lacpBond));
	}
	
	@Test
	public void removeLacpAggregatorTest()
	{
		assertEquals(false, lacpNode.removeLacpAggregator(lacpBond));
		lacpNode.addLacpAggregator(lacpBond);
		assertEquals(true, lacpNode.removeLacpAggregator(lacpBond));
				
	}
	
}
