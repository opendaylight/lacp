/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class LacpConstTest 
{
	Date date;

	@Before
	public void initMocks()
	{
		LacpConst lacpConst = new LacpConst();
		date = new Date(2015,1,1);
	}
	
	@Test
	public void mapMacAddrFromSwIdTest()
	{
		long swId = (long)1;
		assertNotNull(LacpConst.mapMacAddrFromSwId(swId));
	}
	
	@Test
	public void mapLongFromMacAddrTest()
	{
		byte[] addr = { (byte) 00, (byte)00, (byte) 00, (byte) 01 };
		LacpConst.mapLongFromMacAddr(addr);
	}
	
	@Test
	public void lacpMacStringTest()
	{
		byte[] addr = { (byte) 00, (byte)00, (byte) 00, (byte) 01, (byte) 01, (byte)00 };
		String a = LacpConst.lacpMacString(addr);
		String b = String.format("%2x:%2x:%2x:%2x:%2x:%2x", addr[0],addr[1],addr[2],addr[3],
				addr[4],addr[5]);
		assertEquals(a,b);
		a = LacpConst.lacpMacString(null);
		String c = "No MAC";
		assertEquals(a,c);
	}
	
	@Test
	public void getStringUpTimeTest()
	{
		String a = LacpConst.getStringUpTime(date);
		assertNotNull(a);
	}
	
	@Test
	public void toHexTest()
	{
		byte[] arg = { (byte) 00, (byte)00, (byte) 00, (byte) 01, (byte) 01, (byte)00 };
		assertNotNull(LacpConst.toHex(arg));
	}

}
