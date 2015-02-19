package org.opendaylight.lacp.queue;

public class LacpRawPacket {
	String rawLacpPDU;


         public LacpRawPacket(String pdu) {
            this.rawLacpPDU = pdu;
          }

	public String getRawPdu(){
		return this.rawLacpPDU;
	}
}
