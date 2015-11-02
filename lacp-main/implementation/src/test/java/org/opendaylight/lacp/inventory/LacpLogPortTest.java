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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class LacpLogPortTest 
{
	short key = (short)1001;
	short portId = (short)80;
	
	private NodeId nId;
	InstanceIdentifier<Node> nodeId;
	private InstanceIdentifier<NodeConnector> ncId;
	private LacpBond lacpBond;
	private LacpPort lacpPort;
	private LacpNodeExtn lacpNode; 
	
	@MockitoAnnotations.Mock
    private DataBroker dataService;
    
    @MockitoAnnotations.Mock
    private WriteTransaction write;
    
    @MockitoAnnotations.Mock
	private NotificationProviderService notify;
    
	@Before
	public void initMocks() throws Exception
	{
		MockitoAnnotations.initMocks(this);
		nId = new NodeId("openflow:1");
		nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nId)).build();
		lacpNode = new LacpNodeExtn(nodeId);
		lacpBond = LacpBond.newInstance(key, lacpNode);
		LacpLogPort lacpLogPort = new LacpLogPort();
		
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
	        when(dataService.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        

		LacpNodeExtn.setDataBrokerService(dataService);
	        LacpUtil.setDataBrokerService(dataService);
        
        
        	LacpBpduInfo bpduInfo = mock(LacpBpduInfo.class);
 		InstanceIdentifier<NodeConnector> iNc = InstanceIdentifier.builder(Nodes.class)
        		.child(Node.class,new NodeKey(new NodeId("Openflow:2")))
        		.child(NodeConnector.class,new NodeConnectorKey(new NodeConnectorId("NodeCon:2"))).build();
        	when(bpduInfo.getNCRef()).thenReturn(new NodeConnectorRef(iNc));
        
       		lacpPort = LacpPort.newInstance(0, portId, lacpBond, 1,  bpduInfo);
		
		ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
		
		doNothing().when(notify).publish(any(NodeConnectorUpdated.class));
		LacpLogPort.setNotificationService(notify);
		when(dataService.newWriteOnlyTransaction()).thenReturn(write);	 		
		when(write.submit()).thenReturn(Mockito.mock(CheckedFuture.class));
	
	}

	@Test
	public void create_LogicalPortTest()
	{
		lacpPort.setDataBrokerService(dataService);
		List<LacpPort> portList = lacpBond.getActivePortList();
		portList.add(lacpPort);
		LacpLogPort.createLogicalPort(lacpBond);
		assertNotNull(lacpBond.getBondInstanceId());
		assertNotNull(lacpBond.getLogicalNCRef());
		verify(notify, times(1)).publish(any(NodeConnectorUpdated.class));
	}
	
	@Test
	public void delete_LogicalPortTest()
	{
		lacpPort.setDataBrokerService(dataService);
		List<LacpPort> portList = lacpBond.getActivePortList();
		portList.add(lacpPort);
                //Notify called by createLogicalPort function --> times(1)
		LacpLogPort.createLogicalPort(lacpBond);
		//Notify called by deleteLogicalPort function --> times(2)
		LacpLogPort.deleteLogicalPort(lacpBond);
		verify(notify, times(2)).publish(any(NodeConnectorUpdated.class));
				
		//LacpNode deletion status is enabled.
		lacpNode.setLacpNodeDeleteStatus(true);
		portList = lacpBond.getActivePortList();
		portList.add(lacpPort);
		//Notify called by createLogicalPort function --> times(3)
		LacpLogPort.createLogicalPort(lacpBond);
		LacpLogPort.deleteLogicalPort(lacpBond);
		assertNotNull(lacpBond.getLogicalNCRef());
		verify(notify, times(3)).publish(any(NodeConnectorUpdated.class));
	}

}
