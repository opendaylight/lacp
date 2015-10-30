/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.util;

import static org.junit.Assert.*;

import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

public class LacpUtilTest 
{

	@Test
	public void macToStringTest()
	{
		String srcstr = null;
		assertEquals("null", LacpUtil.macToString(srcstr));
		srcstr = "AA:11:BB:00:01:00";
		String srcstr_1 = "AA11BB000100";
		assertEquals(srcstr_1, LacpUtil.macToString(srcstr));
	}
	
	@Test
	public void byteToStringTest()
	{
		byte[] byteArray = { (byte) 00, (byte)00, (byte) 00, (byte) 01 };
		String mac_str = "0001";
		assertEquals(mac_str, LacpUtil.bytetoString(byteArray));
	}
	
	@Test
	public void convertStringToByteArrayTest()
	{
		String mac_str = "0001";
		assertNotNull(LacpUtil.convertStringtoByte(mac_str));
	}
	
	@Test
	public void hexStringToByteArrayTest()
	{
		byte[] byteArray_1;
		String hex_String = "09348179d466baa4ab2980ef998ef89efa";
		String hex_String_1 = "a";
		byteArray_1 = LacpUtil.hexStringToByteArray(hex_String);
		assertNotNull(byteArray_1);
		byteArray_1 = LacpUtil.hexStringToByteArray(hex_String_1);
		assertNotNull(byteArray_1);
	}
	
}
