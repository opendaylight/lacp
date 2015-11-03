/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.Utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BitBufferHelperTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	@Test
	public void testGetByte() {
		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		BitBufferHelper.getByte(data);

	}

	@Test
	public void testGetShortByteArray() {
		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		BitBufferHelper.getShort(data);

		byte[] data1 = new byte[Short.SIZE + 5];
		for(int i=0;i < data1.length;i++){
			data1[i] = (byte)i;
		}
		BitBufferHelper.getShort(data1);

	}

	@Test
	public void testGetIntByteArray() {
		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		BitBufferHelper.getInt(data);

		byte[] data1 = new byte[Integer.SIZE + 5];
		for(int i=0;i < data1.length;i++){
			data1[i] = (byte)i;
		}
		BitBufferHelper.getShort(data1);
	}

	@Test
	public void testGetLongByteArray() {
		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		BitBufferHelper.getLong(data);
		byte[] data1 = new byte[Long.SIZE + 5];
		for(int i=0;i < data1.length;i++){
			data1[i] = (byte)i;
		}
		BitBufferHelper.getShort(data1);

	}

	@Test
	public void testGetShortByteArrayInt() {

		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		int numBits = 25;
		BitBufferHelper.getShort(data, numBits);

		numBits = 9;
		BitBufferHelper.getShort(data, numBits);
	}

	@Test
	public void testGetIntByteArrayInt() {

		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		int numBits = 35;
		BitBufferHelper.getInt(data, numBits);

		numBits = 28;
		BitBufferHelper.getInt(data, numBits);
	}

	@Test
	public void testGetLongByteArrayInt() {

		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		int numBits = 67;
		BitBufferHelper.getLong(data, numBits);

		//numBits = 90;
		//BitBufferHelper.getLong(data, numBits);
		//TODO: StartOffset = 80 - 90 = -10 Which throws Buffer Exception. hence return null
		//tonumber access this value without any check thus resulting in nullpointer exception.

		numBits = 60;
		BitBufferHelper.getLong(data, numBits);
	}

	@Test
	public void testGetBits() {

		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		int startOffset = 0;
		int numBits = 2;
		try{
			BitBufferHelper.getBits(data,startOffset,numBits);
		}catch(Exception e){}


		numBits = 8;
		try{
			BitBufferHelper.getBits(data,startOffset,numBits);
		}catch(Exception e){}


		startOffset = 11;
		numBits = 4;
		try{
			BitBufferHelper.getBits(data,startOffset,numBits);
		}catch(Exception e){}

		numBits = 14;
		try{
			BitBufferHelper.getBits(data,startOffset,numBits);
		}catch(Exception e){}
	}

	@Test
	public void testSetByte() {

		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10};
		byte input = (byte)0x01;
		try{
			BitBufferHelper.setByte(data,input,0,2);
		}catch(BufferException e){}
	}

	@Test
	public void testSetBytes(){
		//TODO
		byte[] data = {0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x10}; 
		byte[] input = {0x11,0x12,0x13,0x14};
		int startOffset = 0;
		int numBits = 2;

		numBits = 8;
		try{
			BitBufferHelper.setBytes(data,input,startOffset,numBits);
		}catch(BufferException e){}


		startOffset = 11;
		numBits = 20;
		try{
			BitBufferHelper.setBytes(data,input,startOffset,numBits);
		}catch(BufferException e){}

		numBits = 13;
		try{
			BitBufferHelper.setBytes(data,input,startOffset,numBits);
		}catch(BufferException e){}

		numBits = 14;
		try{
			BitBufferHelper.setBytes(data,input,startOffset,numBits);
		}catch(BufferException e){}

		try {
			BitBufferHelper.setBytes(null,input,startOffset,numBits);
			fail("BufferException didn't occur");
		} catch (BufferException e) {
		}
		try {
			BitBufferHelper.setBytes(data,input,-1,numBits);
			fail("BufferException didn't occur");
		} catch (BufferException e) {}
	}

	@Test
	public void testToByteArrayNumber() {
		BitBufferHelper.toByteArray(5);
		BitBufferHelper.toByteArray((byte)5);
		BitBufferHelper.toByteArray((short)5);
		BitBufferHelper.toByteArray((long)5);
		exception.expect(IllegalArgumentException.class);
		BitBufferHelper.toByteArray((float)5);
	}

	@Test
	public void testToByteArrayNumberInt() {

		BitBufferHelper.toByteArray(5,6);
		BitBufferHelper.toByteArray((short)5,6);
		BitBufferHelper.toByteArray((long)5,6);
		exception.expect(IllegalArgumentException.class);
		BitBufferHelper.toByteArray((float)5,6);

	}
	
	

}
