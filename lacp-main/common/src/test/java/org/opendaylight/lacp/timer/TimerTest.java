/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import io.netty.util.Timeout;

import org.opendaylight.lacp.timer.TimerFactory;
import org.opendaylight.lacp.timer.TimerFactory.LacpWheelTimer;
import org.opendaylight.lacp.timer.PortWaitWhileTimerRegister;
import org.opendaylight.lacp.timer.Utils;

public class TimerTest {

	public static String getTime(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static void main(String[] args) {



		LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.WAIT_WHILE_TIMER);
		System.out.println("Starting adding the Ports in wheel at " + getTime() + " and time in ms is " + System.currentTimeMillis());
		for (int i=0;i<400;i++){

			PortWaitWhileTimerRegister objPort = new PortWaitWhileTimerRegister((short)i,1L);

			System.out.println("Inserting Port " + i +" at " + System.currentTimeMillis());

			Timeout obj = null;
			obj=instance.registerPortForWaitWhileTimer(objPort, 5, TimeUnit.SECONDS);


			if(i==3){
				obj.cancel();
				try{
					Thread.sleep(000);
				    obj.cancel();
				}catch(InterruptedException ex){

				}
			}

		}


	}
}
