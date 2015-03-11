
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.timer.*;
import org.opendaylight.lacp.timer.Utils.timerWheeltype;

public class TimerExpiryMessageTest {
	TimerExpiryMessage timerExp1, timerExp2;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		timerExp1 = new TimerExpiryMessage(1L, 10, timerWheeltype.CURRENT_WHILE_TIMER);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTimerExpiryMessage() {
		timerExp2 = new TimerExpiryMessage(1L, 10, timerWheeltype.CURRENT_WHILE_TIMER);
		//fail("Not yet implemented");
	}

	@Test
	public void testSetPortID() {
		timerExp1.setPortID(10);
		//fail("Not yet implemented");
	}

	@Test
	public void testGetPortID() {
		timerExp1.getPortID();
		//fail("Not yet implemented");
	}

	@Test
	public void testGetTimerWheelType() {
		timerExp1.getTimerWheelType();
		//fail("Not yet implemented");
	}

	@Test
	public void testSetTimerWheelType() {
		timerExp1.setTimerWheelType(timerWheeltype.PERIODIC_TIMER);
		//fail("Not yet implemented");
	}

}
