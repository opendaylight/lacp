/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.Utils;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.junit.Before;

import org.junit.Test;

import io.netty.util.NetUtil;

public class NetUtilsTest 
{
	
	byte[] byteArray = { (byte) 00, (byte)00, (byte) 00, (byte) 01 };
	byte[] byteArray_1 = { (byte) 00, (byte)00, (byte) 00, (byte) 01, (byte) 11, (byte) 10 };
	
	@Before
	public void initMocks() throws UnknownHostException
	{
		NetUtils netUtils = new NetUtils();
	}
	
	
	@Test
	public void byteArray4ToIntTest()
	{
		int check;
		assertNotNull(NetUtils.byteArray4ToInt(byteArray));
	}
	
	@Test
	public void byteArray6ToLongTest()
	{
		
		long check1;
		assertNotNull(NetUtils.byteArray6ToLong(byteArray_1));
	}
	
	@Test
	public void intToByteArray4Test()
	{
		int int_val = 1;
		NetUtils.intToByteArray4(int_val);
	}
	
	@Test
	public void longToByteArray6Test()
	{
		long long_val = (long)1;
		byte ba[];
		NetUtils.longToByteArray6(long_val);
	}
	
	@Test
	public void getInetNetworkMaskTest()
	{
		int prefixMaskLength = 10;
		boolean isV6 = true;
		NetUtils.getInetNetworkMask(prefixMaskLength, isV6);
	}
	
	@Test
	public void inetAddressConflictTest()
	{
		InetAddress testAddress = null; 
		InetAddress filterAddress = null;
		InetAddress testMask = null;
		InetAddress filterMask = null;
		NetUtils.inetAddressConflict(testAddress, filterAddress, testMask, filterMask);
		try
		{
			testAddress = InetAddress.getByName("172.28.2.23");
			filterAddress = InetAddress.getByName("172.28.1.10");
			testMask = InetAddress.getByName("255.255.255.0");
			filterMask = InetAddress.getByName("255.255.255.0");
		}
		catch(UnknownHostException e)
		{
			
		}
		finally
		{
			NetUtils.inetAddressConflict(testAddress, filterAddress, testMask, filterMask);
		}
	}
	
	@Test
	public void getSubnetMaskLengthTest()
	{
		NetUtils.getSubnetMaskLength(byteArray_1);
	}
	
	@Test
	public void getSubnetPrefixTest()
	{
		InetAddress ip = null; 
		InetAddress ip_1 = null; 
		int maskLen = 25;
		try
		{
			ip = InetAddress.getByName("172.28.30.254");
			ip_1 = InetAddress.getByName("172.28.30.128");
		}
		catch(UnknownHostException e)
		{
			
		}
		finally
		{
			assertEquals(ip_1, NetUtils.getSubnetPrefix(ip, maskLen));
		}
	}
	
	@Test
	public void isIPv4AddressValidTest()
	{
		String cidr = null;
		NetUtils.isIPv4AddressValid(cidr);
		cidr = "10.0.0.1/16";
		NetUtils.isIPv4AddressValid(cidr);
	}
	
	@Test
	public void isIPv6AddressValidTest()
	{
		String cidr = null;
		NetUtils.isIPv6AddressValid(cidr);
		cidr = "10::1/256";
		NetUtils.isIPv6AddressValid(cidr);
	}
	
	@Test
	public void isIPAddressValidTest()
	{
		assertEquals(false, NetUtils.isIPAddressValid(null));
		String cidr = "10.0.0.1/16";
		assertEquals(true, NetUtils.isIPAddressValid(cidr));
	}
	
	@Test
	public void isBroadcastMacddrTest()
	{
		
		assertEquals(false, NetUtils.isBroadcastMACAddr(byteArray));
		
		//Assigning Broadcast Mac Address for byte Array
		String macAddress = "FF:FF:FF:FF:FF:FF";
		String[] macAddressParts = macAddress.split(":");

		// convert hex string to byte values
		byte[] byte_array_2 = new byte[6];
		for(int i=0; i<6; i++)
		{
		    Integer hex = Integer.parseInt(macAddressParts[i], 16);
		    byte_array_2[i] = hex.byteValue();
		}
		
		assertEquals(true, NetUtils.isBroadcastMACAddr(byte_array_2));
	}
	
	@Test
	public void isZeroMacTest()
	{
		byte[] byteArray_0_Mac = { (byte) 00, (byte)00, (byte) 00, (byte) 00, (byte) 00, (byte) 00 };
		assertEquals(true, NetUtils.isZeroMAC(byteArray_0_Mac));
		assertEquals(false, NetUtils.isZeroMAC(byteArray));		
	}
	
	@Test
	public void isUnicastMacAddrTest()
	{
		//Unicast mac address
		
		byte[] byte_unicast = { (byte) 00, (byte)00, (byte) 00, (byte) 00, (byte) 00, (byte) 00 };
		assertEquals(true, NetUtils.isUnicastMACAddr(byte_unicast));
	
		//Non Unicast Mac address
		String macAddress = "11:AA:AA:AA:AA:AA";
		String[] macAddressParts = macAddress.split(":");

		byte[] byte_not_unicast = new byte[6];
		for(int i=0; i<6; i++)
		{
		    Integer hex = Integer.parseInt(macAddressParts[i], 16);
		    byte_not_unicast[i] = hex.byteValue();
		}
		assertEquals(false, NetUtils.isUnicastMACAddr(byte_not_unicast));
		
	}
	
	@Test
	public void isMulticastMacAddrTest()
	{
		String macAddress = "01:00:5E:00:00:00";
		String[] macAddressParts = macAddress.split(":");

		byte[] byte_multicast = new byte[6];
		for(int i=0; i<6; i++)
		{
		    Integer hex = Integer.parseInt(macAddressParts[i], 16);
		    byte_multicast[i] = hex.byteValue();
		}
		
		assertEquals(true, NetUtils.isMulticastMACAddr(byte_multicast));
		
		byte[] byte_not_multicast = { (byte) 00, (byte)00, (byte) 00, (byte) 00, (byte) 00, (byte) 00 };
		assertEquals(false, NetUtils.isMulticastMACAddr(byte_not_multicast));
			
	}
	
	@Test
	public void getHighestIPTest()
	{
		NetUtils.gethighestIP(true);
		NetUtils.gethighestIP(false);
	}
	
	@Test
	public void getInetAddressTest()
	{
		int address = 1;
		NetUtils.getInetAddress(address);
	}
	
	@Test
	public void parseInetAddressTest()
	{
		String addressString = "127.0.0.1";
		NetUtils.parseInetAddress(addressString);
	}
	
	@Test
	public void fieldsConflictTest()
	{
		boolean a,b,c;
		assertEquals(false, NetUtils.fieldsConflict(1, 1));
		assertEquals(false, NetUtils.fieldsConflict(0, 1));
		assertEquals(false, NetUtils.fieldsConflict(1, 0));
		
		assertEquals(true, NetUtils.fieldsConflict(2, 3));
	}
	
	@Test
	public void getUnsignedByteTest()
	{
		byte b = (byte)5;
		int a,c;
		a = (b & 0xFF);
		c = NetUtils.getUnsignedByte(b);
		assertEquals(a,c);
	}
	
	@Test
	public void getUnsignedShortTest()
	{
		short b = (short)5;
		int a,c;
		a = (b & 0xFF);
		c = NetUtils.getUnsignedShort(b);
		assertEquals(a,c);
	}

	@Test
	public void getBroadcastMacAddressTest()
	{
		NetUtils.getBroadcastMACAddr();
	}

}

