package org.opendaylight.lacp.timer;

import java.util.concurrent.TimeUnit;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;

import java.util.Map;
import java.util.HashMap;


import io.netty.util.TimerTask;

	public final class TimerFactory {
		
        private static Map<Utils.timerWheeltype, LacpWheelTimer> timerWheelStore =
								new HashMap<Utils.timerWheeltype, LacpWheelTimer>();
	public static final class LacpWheelTimer {
	    	
	private Timer currentWhileTimerWheel = null;
	private Timer waitWhileTimerWheel = null;
	private Timer periodicTimerWheel = null;
			
			
	private LacpWheelTimer(Utils.timerWheeltype wheelTimerType) {
	        	
	        	
		        switch(wheelTimerType){
		        	case CURRENT_WHILE_TIMER:
		        	{
		        		currentWhileTimerWheel = new HashedWheelTimer();
		        		break;
		        	}
		        	case WAIT_WHILE_TIMER:
		        	{
		        		waitWhileTimerWheel = new HashedWheelTimer();
		        		break;
		        	}
		        	case PERIODIC_TIMER:
		        	{
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
	    	
	    	synchronized (TimerFactory.class) {
	   	
	    		LacpWheelTimer instance = null;
	    		instance = timerWheelStore.get(wheelTimerType);
		        switch(wheelTimerType){
		      	
		        	case CURRENT_WHILE_TIMER:
		        	{
		        		if(instance == null){
		        			instance = new LacpWheelTimer(wheelTimerType);
		        			timerWheelStore.put(Utils.timerWheeltype.CURRENT_WHILE_TIMER, instance);
		        		}
		        		break;
		        	}
		        	case WAIT_WHILE_TIMER:
		        	{
		        		if(instance == null){
		        			instance = new LacpWheelTimer(wheelTimerType);
		        			timerWheelStore.put(Utils.timerWheeltype.WAIT_WHILE_TIMER, instance);
		        		}else {
		        		}
		        		
		        		break;
		        	}
		        	case PERIODIC_TIMER:
		        	{
		        		if(instance == null){
		        			instance = new LacpWheelTimer(wheelTimerType);
		        			timerWheelStore.put(Utils.timerWheeltype.PERIODIC_TIMER, instance);
		        		}
		        		break;
			        }
		        	default:
		        	{
		        		break;
		        	}

		        } //end of switch
	        	return instance;	            
	        } //end of synchronized block
	    	
	    } //end of getInstance
	    
	    public Timeout registerPortForCurrentWhileTimer(TimerTask task, long delay, TimeUnit unit){
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
		if(obj != null){
	    		synchronized(obj){
		    		if(!obj.isExpired()){
		    			obj.cancel();
		    		}
	    		}
		}
	    }

	    public void CancelPortForWaitWhileTimer(Timeout obj){
		if(obj != null){
	    		synchronized(obj){
		    		if(!obj.isExpired()){
		    			obj.cancel();
		    		}
	    		}
		}
	    }

	    public void CancelPortForPeriodicTimer(Timeout obj){
		if(obj != null){
	    		synchronized(obj){
		    		if(!obj.isExpired()){
		    			obj.cancel();
		    		}
	    		}
		}
	    }    
	}
}
