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
	public void toHexTest()
	{
		byte[] arg = { (byte) 00, (byte)00, (byte) 00, (byte) 01, (byte) 01, (byte)00 };
		assertNotNull(LacpConst.toHex(arg));
	}

}
