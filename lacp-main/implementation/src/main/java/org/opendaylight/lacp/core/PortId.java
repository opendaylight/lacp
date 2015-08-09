/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortId {
	private short portId;
	PortId(short port){
		portId = port;
	}
	public short getPort(){
		return portId;
	}
}
