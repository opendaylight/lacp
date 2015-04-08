package org.opendaylight.lacp.state;

import org.opendaylight.lacp.core.LacpBpduInfo;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventory.LacpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTxState {
	
	private static final Logger log = LoggerFactory.getLogger(PeriodicTxState.class);
	protected LacpConst.PERIODIC_STATES stateFlag;
	
	public PeriodicTxState(){
		log.info("Entering PeriodicTxState constructor");
		setStateFlag(LacpConst.PERIODIC_STATES.PERIODIC_DUMMY);
		log.info("Exiting PeriodicTxState constructor");
	}
	
	public LacpConst.PERIODIC_STATES getStateFlag(){
		return stateFlag;
	}
	
	public void setStateFlag(LacpConst.PERIODIC_STATES state){
		log.info("Entering PeriodicTxState setStateFlag");
		stateFlag = state;
		log.info("Exiting PeriodicTxState setStateFlag");
	}

	public void executeStateAction(PeriodicTxContext  obj, LacpPort portObjRef,LacpBpduInfo pdu){
				
	}
}
