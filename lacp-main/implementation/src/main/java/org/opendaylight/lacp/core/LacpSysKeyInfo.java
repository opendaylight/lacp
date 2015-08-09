/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LacpSysKeyInfo {
	private static final Logger LOG = LoggerFactory.getLogger(LacpSysKeyInfo.class);
	byte[] systemId;
	short lacpKey;

	public LacpSysKeyInfo() {
		this.systemId = new byte[LacpConst.ETH_ADDR_LEN];
		this.lacpKey = 1;
	}

	public LacpSysKeyInfo(byte[] systemId, short lacpKey) {
		super();
		this.systemId = Arrays.copyOf(systemId, LacpConst.ETH_ADDR_LEN);
		this.lacpKey = lacpKey;
	}

	public byte[] getSystemId() {
		return systemId;
	}
	public void setSystemId(byte[] systemId) {
		this.systemId = Arrays.copyOf(systemId, LacpConst.ETH_ADDR_LEN);
	}
	public short getLacpKey() {
		return lacpKey;
	}
	public void setLacpKey(short lacpKey) {
		this.lacpKey = lacpKey;
	}

	@Override
	public String toString() {
		return "LacpSysKeyInfo [systemId=" + LacpConst.toHex(systemId)
				+ ", lacpKey=" + lacpKey + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lacpKey;
		result = prime * result + Arrays.hashCode(systemId);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		LOG.debug("Entering/Exiting LacpSysKeyInfo equals method");
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (!(obj instanceof LacpSysKeyInfo)){
			return false;
		}
		LacpSysKeyInfo other = (LacpSysKeyInfo) obj;
		if (lacpKey != other.lacpKey){
			return false;
		}
		if (!Arrays.equals(systemId, other.systemId)){
			return false;
		}
		return true;
	}
}
