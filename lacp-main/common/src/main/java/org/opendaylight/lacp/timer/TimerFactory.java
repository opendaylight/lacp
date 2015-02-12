package org.opendaylight.lacp.timer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import io.netty.util.TimerTask;

	public class TimerFactory {
		
        private static Map<Utils.timerWheeltype, LacpWheelTimer> timerWheelStore =
								new HashMap<Utils.timerWheeltype, LacpWheelTimer>();
	
	//private final static Logger _logger = LoggerFactory.getLogger(TimerFactory.class);

	public static final class LacpWheelTimer {
	    	
	private Timer currentWhileTimerWheel = null;
	private Timer waitWhileTimerWheel = null;
	private Timer periodicTimerWheel = null;
	private final static Logger _logger = LoggerFactory.getLogger(LacpWheelTimer.class);
			
	//as the registerXXX methods are being called on by multiple RSM threads
	//private Lock lock;
			
	private LacpWheelTimer(Utils.timerWheeltype wheelTimerType) {
	        	
	//lock = new ReentrantLock();
	        	
		        switch(wheelTimerType){
		        	case CURRENT_WHILE_TIMER:
		        	{
					 //_logger.info("Cannot send packet out or flood as controller node connector is not available for node {}.", nodeId);
					 _logger.info("Constructor current-while");
		        		currentWhileTimerWheel = new HashedWheelTimer();
		        		break;
		        	}
		        	case WAIT_WHILE_TIMER:
		        	{
					_logger.info("Constructor wait-while");
		        		waitWhileTimerWheel = new HashedWheelTimer();
		        		break;
		        	}
		        	case PERIODIC_TIMER:
		        	{
					_logger.info("Constructor periodic");
		        		periodicTimerWheel = new HashedWheelTimer();
		        		break;
			        }
		        	default:
		        	{
		        		break;
		        	}
		        }
	        }
	        
	    public static LacpWheelTimer getInstance(Utils.timerWheeltype wheelTimerType) {
	    	
	    	synchronized (timerWheelStore) {
	    		LacpWheelTimer instance = null;
	    		instance = timerWheelStore.get(wheelTimerType);
			if(instance == null){
	    			instance = new LacpWheelTimer(wheelTimerType);
        			timerWheelStore.put(wheelTimerType, instance);
	    		}
	        	return instance;	            
	        } //end of synchronized block
	    	
	    } //end of getInstance
	    
	    public Timeout registerPortForCurrentWhileTimer(TimerTask task, long delay, TimeUnit unit){
			//TODO-make below lock wheel specific to avoid parallel execution of register requests
		    	if(currentWhileTimerWheel != null){
				synchronized(currentWhileTimerWheel) {
					return currentWhileTimerWheel.newTimeout(task, delay, unit);
				}
			}
	    	return null;
	    }
	    
	    public Timeout registerPortForWaitWhileTimer(TimerTask task, long delay, TimeUnit unit){
	    	
			if(waitWhileTimerWheel != null){
		    		synchronized(waitWhileTimerWheel){
					return waitWhileTimerWheel.newTimeout(task, delay, unit);
				}
	    		}	
		return null;
	    }
	    
	    public Timeout registerPortForPeriodicTimer(TimerTask task, long delay, TimeUnit unit){
			if(periodicTimerWheel != null){
	    			synchronized(periodicTimerWheel){
					return periodicTimerWheel.newTimeout(task, delay, unit);
				}
			}
		return null;
	    }

            public void CancelCurrentWhileTimer(Timeout obj){
	    	if(obj!=null){
		    	synchronized(obj){
			    	if(!obj.isExpired()){
			    		obj.cancel();
			    	}
		    	}
	    	}
	    }
	    public void CancelPortForWaitWhileTimer(Timeout obj){
	    	if(obj!=null){
		    	synchronized(this){
			    	if(!obj.isExpired()){
			    		obj.cancel();
			    	}
		    	}
	    	}
	    }
	    public void CancelPortForPeriodicTimer(Timeout obj){
	    	if(obj != null){
		    	synchronized(this){
			    	if(!obj.isExpired()){
			    		obj.cancel();
			    	}
		    	}
		}
	    }
	}
}
