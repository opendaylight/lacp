package org.opendaylight.lacp.timer;

/*
 * import java.text.DateFormat;
 * import java.text.SimpleDateFormat;
 * import java.util.Date;
 * */

import io.netty.util.Timeout;
import io.netty.util.TimerTask;


public class PortCurrentWhileTimerRegister extends BasePortTimerRegister implements TimerTask  {
	
	PortCurrentWhileTimerRegister(int pid, int sysid){
		super(pid,sysid);
	}
	
	@Override
	public void run(Timeout timeoutHandle) throws Exception {
		//identify the right timer queue using systemid as key and then enque the message
		//System.out.println("Waitwhile - Timeout occured for port:" + this.getPortID() + " at " + getTime() + 
		//		" and time in ms is " + System.currentTimeMillis());
		TimerExpiryMessage obj = new TimerExpiryMessage(this.getPortID(),Utils.timerWheeltype.CURRENT_WHILE_TIMER);
		TimerQueue objT = TimerQueue.getTimerQueueInstance();
		objT.add(obj);
	}

}
