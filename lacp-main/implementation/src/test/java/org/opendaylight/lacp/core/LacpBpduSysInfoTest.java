/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.core;

import static org.junit.Assert.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LacpBpduSysInfoTest {

	private LacpBpduSysInfo system;

	@Before
	public void setUp() throws Exception {
		
		byte[] sys = {0,0,0,0,0,0x2};
		system = new LacpBpduSysInfo();
		system = new LacpBpduSysInfo(1, sys,(short)5, 2, (short) 80, (byte)0x55);
	}

	@Test
	public void testHashCode() {
		assertNotNull(system.hashCode());
	}

	@Test
	public void testSettersAndGetters() {

		
		short nodeKey = 0x65;
		system.setNodeKey(nodeKey);
		short nodePortNumber = 0x20;
		system.setNodePortNum(nodePortNumber);
		int nodePortPriority = 1;
		system.setNodePortPri(nodePortPriority);
		byte nodePortState = 0x7;
		system.setNodePortState(nodePortState);
		byte[] nodeSystemAddr = {0x11,0x11,0x11,0x11,0x11,0x11};
		system.setNodeSysAddr(nodeSystemAddr);
		int nodeSystemPriority = 5;
		system.setNodeSysPri(nodeSystemPriority);
		
		

		assertEquals(nodeKey,system.getNodeKey());
		assertEquals(nodePortNumber,system.getNodePortNum());
		assertEquals(nodePortPriority,system.getNodePortPriority());
		assertEquals(nodePortState,system.getNodePortState());
		assertEquals(nodeSystemPriority,system.getNodeSysPri());
	}

	/*@Test
	public void Serialize_Deserialize() {
		//TODO
		//TODO
		//BufferOverflowException
		//Total capacity -15[[SYSTEMINFO_SIZE]. Position outflows the cap
		/*byte[] ret_byte = system.serialize();
		System.out.println(ret_byte);
		system.deserialize(ByteBuffer.wrap(ret_byte));
	}*/

	@Test
	public void testEqualsObject() {
		//TODO
		LacpBpduSysInfo sys2 = new LacpBpduSysInfo();
		assertEquals(true, system.equals(system));
		
		assertEquals(false,system.equals(sys2));
		
		assertFalse(system.equals(new LacpBpduInfo()));
		assertFalse(system.equals(sys2));
		
		sys2.setNodeSysPri(system.getNodeSysPri());
		assertFalse(system.equals(sys2));
		
		sys2.setNodeKey(system.getNodeKey());
		assertFalse(system.equals(sys2));
		
		sys2.setNodePortPri(system.getNodePortPriority());
		assertFalse(system.equals(sys2));
		
		sys2.setNodePortNum(system.getNodePortNum());
		assertFalse(system.equals(sys2));
		
		sys2.setNodePortState(system.getNodePortState());
		assertFalse(system.equals(sys2));
		
		sys2.setNodeSysAddr(system.getNodeSysAddr());
		//assertTrue(system.equals(sys2));
		
	}

	@Test
	public void testToString() {
		assertNotNull(system.toString());
	}

}
