/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class LagIdElemTest {

	private LagIdElem lagIdElem;

	@Before
	public void setUp() throws Exception {
		
		int sysPri=1;
		byte[] sysMac = {0x10,0x00,0x00,0x00,0x20,0x20};
		int portPri=1;
		short portNumber = 0x20;
		short key = 0x1010;
		lagIdElem = new LagIdElem(new LagIdSys(sysPri, sysMac),key,new LagIdPort(portPri, portNumber));
	}


	@Test
	public void systemsAndPorts() {
		LagIdSys sys = new LagIdSys(lagIdElem.system);
		LagIdPort pt = new LagIdPort(lagIdElem.port);
		assertEquals(0,sys.compareTo(lagIdElem.system));
		assertEquals(0,pt.compareTo(lagIdElem.port));
	}

	@Test
	public void testIsNeighborFound() {
		assertTrue(lagIdElem.isNeighborFound());
	}

	@Test
	public void testIsMacAddrEqual() {
		byte[] macAddr = {0x10,0x00,0x00,0x00,0x20,0x20};
		assertTrue(lagIdElem.isMacAddrEqual(macAddr));

		byte[] macAddr1 = {0x10,0x10,0x00,0x00,0x20,0x20};
		assertFalse(lagIdElem.isMacAddrEqual(macAddr1));
	}


	@Test
	public void testCompare_PartialTo() {
		LagIdElem lagIdElm ;
		assertEquals(0,lagIdElem.compareTo(lagIdElem));
		assertEquals(0,lagIdElem.compareToPartial(lagIdElem));
		assertEquals(-1,lagIdElem.compareTo(Mockito.mock(LagIdElem.class)));
		assertEquals(-1,lagIdElem.compareToPartial(Mockito.mock(LagIdElem.class)));

		lagIdElm = new LagIdElem(new LagIdSys(lagIdElem.system.sysPriority - 1, lagIdElem.system.sysMacAddress),(short)0x0,lagIdElem.port);
		assertEquals(1,lagIdElem.compareTo(lagIdElm));
		assertEquals(1,lagIdElem.compareToPartial(lagIdElm));
		
		lagIdElm = new LagIdElem(lagIdElem.system,(short)0x0,lagIdElem.port);
		assertEquals(1,lagIdElem.compareTo(lagIdElm));
		assertEquals(1,lagIdElem.compareToPartial(lagIdElm));

		lagIdElm = new LagIdElem(lagIdElem.system,(short)0x1110,lagIdElem.port);
		assertEquals(-1,lagIdElem.compareTo(lagIdElm));
		assertEquals(-1,lagIdElem.compareToPartial(lagIdElm));

		//Port Priority
		lagIdElm = new LagIdElem(lagIdElem.system,lagIdElem.key,new LagIdPort(lagIdElem.port.portPriority+1,lagIdElem.port.portNumber));
		assertEquals(-1,lagIdElem.compareTo(lagIdElm));
		
		lagIdElm = new LagIdElem(lagIdElem.system,lagIdElem.key,new LagIdPort(lagIdElem.port.getPort_priority() - 1,lagIdElem.port.getPort_number()));
		assertEquals(1,lagIdElem.compareTo(lagIdElm));
		
		//Port Number
		lagIdElm = new LagIdElem(lagIdElem.system,lagIdElem.key,new LagIdPort(lagIdElem.port.portPriority,(short) (lagIdElem.port.portNumber+1)));
		assertEquals(-1,lagIdElem.compareTo(lagIdElm));
		
		lagIdElm = new LagIdElem(lagIdElem.system,lagIdElem.key,new LagIdPort(lagIdElem.port.portPriority,(short) (lagIdElem.port.portNumber-1)));
		assertEquals(1,lagIdElem.compareTo(lagIdElm));
	}

	@Test
	public void testToString() {
		assertNotNull(lagIdElem.toString());
	}
	
	@Test
	public void testLagId(){
		LagIdElem lagIdElement =  new LagIdElem(lagIdElem);
		LagId lagid1 = new LagId(lagIdElem,lagIdElement);
		LagId lagid2 = new LagId(lagIdElem,lagIdElement);
		//CompareTo and CompareToPartial
		assertEquals(0,lagid1.compareTo(lagid2));
		assertEquals(0,lagid1.compareToPartial(lagid2));
		
		
		//

		int sysPri=2;
		byte[] sysMac = {0x10,0x00,0x00,0x00,0x21,0x21};
		int portPri=1;
		short portNumber = 0x20;
		short key = 0x1010;
		lagIdElement = new LagIdElem(new LagIdSys(sysPri, sysMac),key,new LagIdPort(portPri, portNumber));
		lagid1 = new LagId(lagIdElem,lagIdElement);
		
		byte[] mac = {0x10,0x00,0x00,0x00,0x20,0x20};
		assertEquals(2,lagid1.isMacAddressInLagId(mac));
		mac[4]=0x21;
		mac[5]=0x21;
		assertEquals(1,lagid1.isMacAddressInLagId(mac));
		mac[1]=0x23;
		assertEquals(0,lagid1.isMacAddressInLagId(mac));
		
	}
	
	@Test
	public void testLacpSysKeyInfo(){
		LacpSysKeyInfo lacpSysKeyInfo1 = new LacpSysKeyInfo();
		LacpSysKeyInfo lacpSysKeyInfo2 = new LacpSysKeyInfo();
		
		short lacpKey = 65;
		lacpSysKeyInfo1.setLacpKey(lacpKey);
		assertEquals(lacpKey,lacpSysKeyInfo1.getLacpKey());
		
		byte[] systemId = {0x10,0x00,0x00,0x00,0x21,0x21};
		lacpSysKeyInfo1.setSystemId(systemId);
		assertTrue(Arrays.equals(systemId, lacpSysKeyInfo1.getSystemId()));
		
		assertTrue(lacpSysKeyInfo1.equals(lacpSysKeyInfo1));
		assertFalse(lacpSysKeyInfo1.equals(null));
		assertFalse(lacpSysKeyInfo1.equals(lagIdElem));
		assertFalse(lacpSysKeyInfo1.equals(lacpSysKeyInfo2));
		lacpSysKeyInfo2.setLacpKey(lacpKey);
		assertFalse(lacpSysKeyInfo1.equals(lacpSysKeyInfo2));
		lacpSysKeyInfo2.setSystemId(systemId);
		assertTrue(lacpSysKeyInfo1.equals(lacpSysKeyInfo2));
		
		assertNotNull(lacpSysKeyInfo1.toString());
		
	}

}
