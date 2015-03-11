
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.timer.*;
import org.opendaylight.lacp.timer.TimerFactory.LacpWheelTimer;

public class PortPeriodicTimerRegisterTest {
	PortPeriodicTimerRegister periodicTmr1, periodicTmr2;
	LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.CURRENT_WHILE_TIMER);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		periodicTmr1 = new PortPeriodicTimerRegister((short)10, 20L);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPortPeriodicTimerRegister() {
		periodicTmr2 = new PortPeriodicTimerRegister((short)10, 20L);
		//fail("Not yet implemented");
	}

	@Test
	public void testRun() {
		instance.registerPortForPeriodicTimer(periodicTmr1, 10L,TimeUnit.SECONDS);
		
		//fail("Not yet implemented");
	}

}
