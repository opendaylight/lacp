/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.timer;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.opendaylight.lacp.queue.LacpTimerQueue;

public class PortWaitWhileTimerRegister extends BasePortTimerRegister implements TimerTask  {


	static int i;

	public PortWaitWhileTimerRegister(short pid, long sysid){
		super(pid,sysid);
	}

	public  String getTime(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}
	@Override
	public void run(Timeout timeoutHandle) throws Exception {
		//identify the right timer queue using systemid as key and then enque the message
		long swid = this.getSystemID();

		TimerExpiryMessage obj = new TimerExpiryMessage(swid,this.getPortID(),Utils.timerWheeltype.WAIT_WHILE_TIMER);
		LacpTimerQueue objT = LacpTimerQueue.getLacpTimerQueueInstance();
		if(objT.isLacpQueuePresent(swid)){
			objT.enqueue(swid, obj);
		}
	}
}

