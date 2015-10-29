/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.core;

import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpConst {

	private static final Logger LOG = LoggerFactory.getLogger(LacpConst.class);

	/* Port State Definitions */
	public static final byte PORT_STATE_LACP_ACTIVITY=0x1;
	public static final byte PORT_STATE_LACP_TIMEOUT=0x2;
	public static final byte PORT_STATE_AGGREGATION=0x4;
	public static final byte PORT_STATE_SYNCHRONIZATION=0x8;
	public static final byte PORT_STATE_COLLECTING=0x10;
	public static final byte PORT_STATE_DISTRIBUTING=0x20;
	public static final byte PORT_STATE_DEFAULTED=0x40;
	public static final byte PORT_STATE_EXPIRED=(byte)(0x80 & 0xff);

	  /* Port Variable */
	public static final short PORT_BEGIN=0x1;
	public static final short PORT_LACP_ENABLED=0x2;
	public static final short PORT_ACTOR_CHURN=0x4;
	public static final short PORT_PARTNER_CHURN=0x8;
	public static final short PORT_READY=0x10;
	public static final short PORT_READY_N=0x20;
	public static final short PORT_MATCHED=0x40;
	public static final short PORT_STANDBY=0x80;
	public static final short PORT_SELECTED=0x100;
	public static final short PORT_MOVED=0x200;
	public static final short PORT_DOWN=0x400;
        public static final short PORT_AGG_RESELECT=0x800;

	public static final byte LINK_SPEED_BITMASK_10MBPS =0x1;
	public static final byte LINK_SPEED_BITMASK_100MBPS =0x2;
	public static final byte LINK_SPEED_BITMASK_1000MBPS =0x4;
	public static final byte LINK_SPEED_BITMASK_10000MBPS =0x8;
	public static final byte LINK_SPEED_BITMASK_40000MBPS =0x10;
	public static final byte LINK_SPEED_BITMASK_100000MBPS =0x20;

	public static final byte DUPLEX_KEY_BITS=0x1;
	public static final byte SPEED_KEY_BITS=0x3E;
	public static final int  USER_KEY_BITS=0xff00;

	public static final byte BOND_LINK_UP=0;
	public static final byte BOND_LINK_FAIL=1;
	public static final byte BOND_LINK_DOWN=2;
	public static final byte BOND_LINK_BACK=3;

	public static final byte ETH_ADDR_LEN=6;
	public static final byte[] NULL_MAC_ADDRESS = {0,0,0,0,0,0};
	public static final byte[] LACP_DEST_MAC_ADDRESS = {0x01,(byte)0x80,(byte)0xc2,0,0,0x02};
	public static final String LACP_DEST_MAC_STRING = "01:80:c2:00:00:02";
	public static final short LACP_ETH_TYPE=(short)0x8809;

	public static final short SHORT_TIMEOUT=1;
	public static final short LONG_TIMEOUT=0;
	public static final short STANDBY=0x2;
	public static final short MAX_TX_IN_SECOND=3;
	public static final short COLLECTOR_MAX_DELAY=0;

	public static final short FAST_PERIODIC_TIME=1;
	public static final short SLOW_PERIODIC_TIME=30;
	public static final short SHORT_TIMEOUT_TIME=(3*FAST_PERIODIC_TIME);
	public static final short LONG_TIMEOUT_TIME = (3*SLOW_PERIODIC_TIME);
	public static final short CHURN_DETECTION_TIME=60;
	public static final short AGGREGATE_WAIT_TIME=2;

	public static final short TIMER_INTERVAL=500;  //500ms


	public static final short TYPE_LACPDU=1;
	public static final short TYPE_MARKER=2;


	public static final short MARKER_INFORMATION_SUBTYPE=1;
	public static final short MARKER_RESPONSE_SUBTYPE=2;


	public static final short LACPDU_LENGTH = 110;

	public static final int RX_HANDLER_DROPPED= 1;
	public static final int RX_HANDLER_CONSUMED = 0;

	public static final int PORT_PRIORITY = (int)0x000000ff;
	public static final int SYSTEM_PRIORITY = (int) 0x0000ffff;
	public static final short PORT_ID_MAX = (short) 0xffff;

	public static final int RESERVED = 0;
        public static final int LEN_TYPE = 34825;
        public static final short ACTOR_INFO_LEN = 20;
        public static final short PARTNER_INFO_LEN = 20;
        public static final short COLLECTOR_INFO_LEN = 16;
        public static final short TERMINATOR_INFO_LEN = 0;

    /* LACP Bond Parameter */

	public static enum BOND_TYPE {
		BOND_STABLE,
		BOND_BANDWIDTH,
		BOND_COUNT;
	}


	public static enum TIMER_TYPE {
		CURRENT_WHILE_TIMER,
		ACTOR_CHURN_TIMER,
		PERIODIC_TIMER,
		PARTNER_CHURN_TIMER,
		WAIT_WHILE_TIMER;

	}

	public static enum MUX_STATES {
	     MUX_DUMMY,
	     MUX_DETACHED,
	     MUX_WAITING,
	     MUX_ATTACHED,
	     MUX_COLLECTING_DISTRIBUTING;

	}

	public enum RX_STATES  {
		RX_DUMMY ,
		RX_INITIALIZE ,
		RX_PORT_DISABLED ,
		RX_LACP_DISABLED ,
		RX_EXPIRED ,
		RX_DEFAULTED,
		RX_CURRENT;

	}

	public static enum TX_STATES {
		TX_DUMMY,
		TRANSMIT
	}

	public static enum PERIODIC_STATES {
		PERIODIC_DUMMY,
		NO_PERIODIC,
		FAST_PERIODIC,
		SLOW_PERIODIC,
		PERIODIC_TX
	}



	static public byte[] mapMacAddrFromSwId(long swId) {
		byte[] longArray = new byte[8];
		byte[] result = new byte[6];
		ByteBuffer longArrayBB = ByteBuffer.wrap(longArray);
		longArrayBB.putLong(swId);

		System.arraycopy(longArray, 2, result, 0, 6);
		LOG.debug("mac address={} is returned", result);
		return result;
	}

	static public long mapLongFromMacAddr(byte[] addr) {
		ByteBuffer longArrayBB = ByteBuffer.wrap(new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
		longArrayBB.position(2);
        	longArrayBB.put(addr);
        	longArrayBB.rewind();
        	long result = longArrayBB.getLong();
		return result;
	}

	static public String lacpMacString(byte[] content) {
		if (content!=null){
			return (String.format("%2x:%2x:%2x:%2x:%2x:%2x", content[0],content[1],content[2],content[3],
				content[4],content[5]));
		}
		else{
			return ("No MAC");
		}
	}

	static public String getStringUpTime(Date activeSince) {

		long uptime;
		if (activeSince == null) {
			uptime = 0;
		}
		else {
			uptime = ((new Date()).getTime() -  activeSince.getTime());
		}

		long diffSeconds = uptime/1000 %60;
		long diffMinutes = uptime / (60 * 1000) % 60;
		long diffHours = uptime / (60*60*1000) % 24;
		long diffDays = uptime / (24*60*60*1000);

		String result = String.format("%d days %02d:%02d:%02d", diffDays,
				diffHours, diffMinutes, diffSeconds);
		return result;
	}

	static public String toHex(byte[] arg) {
	    return String.format("%040x", new BigInteger(1, arg));
	}
}
