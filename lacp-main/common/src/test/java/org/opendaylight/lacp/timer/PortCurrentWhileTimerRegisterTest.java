
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import io.netty.util.Timeout;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.timer.*;
import org.opendaylight.lacp.timer.TimerFactory.LacpWheelTimer;

public class PortCurrentWhileTimerRegisterTest {
	PortCurrentWhileTimerRegister currentWhile1, currentWhile2;
	LacpWheelTimer instance = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.CURRENT_WHILE_TIMER);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		currentWhile1 = new PortCurrentWhileTimerRegister((short)10, 20L);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPortCurrentWhileTimerRegister() {
		currentWhile2 = new PortCurrentWhileTimerRegister((short)10, 20L);
		//fail("Not yet implemented");
	}

	@Test
	public void testRun() {
		instance.registerPortForCurrentWhileTimer(currentWhile1, 10L,TimeUnit.SECONDS);
		//Timeout timeoutHandle = new Timeout();
		//currentWhile1.run(timeoutHandle);
		//fail("Not yet implemented");
	}

}
