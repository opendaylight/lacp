/* 
 * Copyright (c) 2014 Dell Inc. and others.  All rights reserved. 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.packethandler;

import org.junit.Test;
import org.mockito.Mockito;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.lacp.packethandler.LacpPacketHandler;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.base.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;
import org.opendaylight.lacp.queue.LacpQueue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class LacpPacketHandlerTest
{
    private DataBroker dataBroker;
    private LacpQueue<PacketReceived> queueId;
    private LacpPacketHandler lacpHandler;

    @Before
    public void initMocks()
    {
        lacpHandler = new LacpPacketHandler();
        dataBroker = Mockito.mock(DataBroker.class);
        lacpHandler.setDataBrokerService(dataBroker);
        queueId = Mockito.mock(LacpQueue.class);
        lacpHandler.updateQueueId(queueId);
    }
    @Test
    public void verifyNullPacket() throws Exception
    {
        lacpHandler.onPacketReceived(null);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }
    @Test
    public void verifyPacketValidMac() throws Exception
    {
        byte packArr[] = { 0x01, (byte)0x80, (byte)0xc2, 0x00, 0x00, 0x02, 0x00, 0x01, (byte)0xe8,
                           0x00, (byte)0xee, (byte)0xee, (byte)0x81, 0x00, (byte)0x0f, (byte)0xff, (byte)0x88, (byte)0x09,
                           0x01, 0x01, 0x01, 0x14, (byte)0x80, 0x00};
        PacketReceived packet = new PacketReceivedBuilder().setPayload(packArr)
                                 .setMatch(new MatchBuilder().build()).build();
        lacpHandler.onPacketReceived(packet);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }
    @Test
    public void verifyPacketInvalidMac() throws Exception
    {
        byte packArr[] = { 0x01, (byte)0x80, (byte)0xd2, 0x00, 0x00, 0x02, 0x00, 0x01, (byte)0xe8,
                           0x00, (byte)0xee, (byte)0xee, (byte)0x81, 0x00, (byte)0x0f, (byte)0xff, (byte)0x88, (byte)0x09,
                           0x01, 0x01, 0x01, 0x14, (byte)0x80, 0x00};

        PacketReceived packet = new PacketReceivedBuilder().setPayload(packArr)
                                 .setMatch(new MatchBuilder().build()).build();
        lacpHandler.onPacketReceived(packet);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }
    @Test
    public void verifyPacketInvalidPacket() throws Exception
    {
        byte packArr[] = {};
        PacketReceived packet = new PacketReceivedBuilder().setPayload(packArr)
                                 .setMatch(new MatchBuilder().build()).build();
        lacpHandler.onPacketReceived(packet);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }

    @Test
    public void verifyPacketInvalidType() throws Exception
    {
        byte packArr[] = { 0x01, (byte)0x80, (byte)0xc2, 0x00, 0x00, 0x02, 0x00, 0x01, (byte)0xe8,
                           0x00, (byte)0xee, (byte)0xee, (byte)0x81, 0x00, (byte)0x0f, (byte)0xff, (byte)0x80, (byte)0x09,
                           0x01, 0x01, 0x01, 0x14, (byte)0x80, 0x00};
        PacketReceived packet = new PacketReceivedBuilder().setPayload(packArr)
                                 .setMatch(new MatchBuilder().build()).build();
        lacpHandler.onPacketReceived(packet);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }
    @Test
    public void verifyPacketValidPort() throws Exception
    {
        byte packArr[] = { 0x01, (byte)0x80, (byte)0xc2, 0x00, 0x00, 0x02, 0x00, 0x01, (byte)0xe8,
                           0x00, (byte)0xee, (byte)0xee, (byte)0x81, 0x00, (byte)0x0f, (byte)0xff, (byte)0x88, (byte)0x09,
                           0x01, 0x01, 0x01, 0x14, (byte)0x80, 0x00};
        InstanceIdentifier<NodeConnector> ncId = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                                  .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("1"))).build();
        PacketReceived packet = new PacketReceivedBuilder().setPayload(packArr)
                                 .setMatch(new MatchBuilder().build())
                                 .setIngress(new NodeConnectorRef(ncId))
                                 .build();

        ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
        Optional optional = Mockito.mock(Optional.class);
        CheckedFuture result = Mockito.mock(CheckedFuture.class);
        when(result.get()).thenReturn(optional);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(result);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        lacpHandler.onPacketReceived(packet);
        verify(queueId, times(0)).enqueue(any(PacketReceived.class));
    }

}
