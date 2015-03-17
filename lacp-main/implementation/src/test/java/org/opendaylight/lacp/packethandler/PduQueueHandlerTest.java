/*
  * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
  * This program and the accompanying materials are made available under the
  *   * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  *     * and is available at http://www.eclipse.org/legal/epl-v10.html
  *     */
package org.opendaylight.lacp.packethandler;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;


import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPduBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.SubTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.VersionValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.TlvTypeOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.lacp.packet.field.*;
import org.opendaylight.lacp.Utils.*;

import org.opendaylight.lacp.queue.*;


public class PduQueueHandlerTest {


	@Test
	public void test_decodeLacp() throws Exception {

		byte[] packet = {
		0x01,  (byte)0x80, (byte)0xc2, 0x00, 0x00, 0x02, 0x3a, (byte)0xd2,
		0x43, (byte)0xf4, 0x01, 0x74, (byte)0x88, 0x09, 0x01, 0x01,
		0x01, 0x14, (byte)0xff, (byte)0xff, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x11, 0x00, 0x21, 0x00, (byte)0xff, 0x00, 0x02,
		0x4d, 0x00, 0x00, 0x00, 0x02, 0x14, (byte)0xff, (byte)0xff, 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
		0x00, (byte)0xff, 0x00, 0x01, 0x01, 0x00, 0x00, 0x00,
		0x03, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};
		PduQueueHandler qh = new PduQueueHandler();
		LacpPacketPduBuilder builder = qh.decodeLacp(new PacketReceivedBuilder()
		    	.setPayload(packet)
			.setMatch(new MatchBuilder().build())
			.build());
		PartnerInfo partner;
		ActorInfo actor;
		assertEquals("01:80:c2:00:00:02", builder.getDestAddress().getValue());
		assertEquals("3a:d2:43:f4:01:74", builder.getSrcAddress().getValue());
		assertEquals(34825, (int)builder.getLenType());
		assertEquals(1, (int)builder.getSubtype().getIntValue());
		assertEquals(1, (int)builder.getVersion().getIntValue());
		actor = builder.getActorInfo();
		assertEquals(1, (int)actor.getTlvType().getIntValue());
		assertEquals(20, (int)actor.getInfoLen());
		assertEquals(65535, (int)actor.getSystemPriority());
		assertEquals("00:00:00:00:00:11", actor.getSystemId());
		assertEquals(33, (int)actor.getKey());
		assertEquals(255, (int)actor.getPortPriority());
		assertEquals(2, (int)actor.getPort());
		assertEquals(77, (int)actor.getState());
		assertEquals(0, (int)actor.getReserved());
		assertEquals(0, (int)actor.getReserved1());
		partner = builder.getPartnerInfo();
		assertEquals(2, (int)partner.getTlvType().getIntValue());
		assertEquals(20, (int)partner.getInfoLen());
		assertEquals(65535, (int)partner.getSystemPriority());
		assertEquals("00:00:00:00:00:00", partner.getSystemId());
		assertEquals(1, (int)partner.getKey());
		assertEquals(255, (int)partner.getPortPriority());
		assertEquals(1, (int)partner.getPort());
		assertEquals(1, (int)partner.getState());
		assertEquals(0, (int)partner.getReserved());
		assertEquals(0, (int)partner.getReserved1());
		assertEquals(3, (int)builder.getCollectorTlvType().getIntValue());
		assertEquals(16, (int)builder.getCollectorInfoLen());
		assertEquals(0, (int)builder.getCollectorMaxDelay());
		assertEquals(0, (int)builder.getCollectorReserved().intValue());
		assertEquals(0, (int)builder.getCollectorReserved1().intValue());
		assertEquals(0, (int)builder.getTerminatorTlvType().getIntValue());
		assertEquals(0, (int)builder.getTerminatorInfoLen());
		assertEquals(null, builder.getTerminatorReserved());

	}

					
}

