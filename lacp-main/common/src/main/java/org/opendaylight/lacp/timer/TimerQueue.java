package org.opendaylight.lacp.timer;

import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.ConcurrentHashMap;

public  class TimerQueue {
	
	
	//private Map <int ,ConcurrentLinkedQueue> systemTimerQueues = new ConcurrentHashMap <int ,ConcurrentLinkedQueue>();
	private	ConcurrentLinkedQueue<TimerExpiryMessage> queue = new ConcurrentLinkedQueue<TimerExpiryMessage>();
	private static TimerQueue instance = null;
	
	private TimerQueue(){
		
	}
	
	public static TimerQueue getTimerQueueInstance(){
		if (instance == null) {
			//Thread Safe
			synchronized (TimerQueue.class) {
				if (instance == null) {
					instance = new TimerQueue();
				}
			}
		}
		return instance;
	}
	
	public void add(TimerExpiryMessage obj){
		//first identify the right queue from the system id and then append it
		queue.add(obj);
		//System.out.println("adding queue object, with portid as " + obj.getPortID() + " and " + "timer wheel type as " + obj.getTimerWheelType());
	}
}

