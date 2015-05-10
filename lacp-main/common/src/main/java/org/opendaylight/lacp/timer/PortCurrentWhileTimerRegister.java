package org.opendaylight.lacp.timer;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.opendaylight.lacp.queue.LacpTimerQueue;

public class PortCurrentWhileTimerRegister extends BasePortTimerRegister implements TimerTask  {
	
	public PortCurrentWhileTimerRegister(short pid, long sysid){
		super(pid,sysid);
	}
	
	@Override
	public void run(Timeout timeoutHandle) throws Exception {
		//identify the right timer queue using systemid as key and then enque the message
		long swid = this.getSystemID();
		TimerExpiryMessage obj = new TimerExpiryMessage(swid, this.getPortID(),Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		LacpTimerQueue objT = LacpTimerQueue.getLacpTimerQueueInstance();
		if(objT.isLacpQueuePresent(swid)){
			objT.enqueue(swid, obj);
		}
	}

}
