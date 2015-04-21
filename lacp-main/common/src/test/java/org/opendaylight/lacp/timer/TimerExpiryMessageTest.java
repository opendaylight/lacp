
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
	public void testTimerExpiryMessage() throws Exception {
		timerExp2 = new TimerExpiryMessage(1L, 10, timerWheeltype.CURRENT_WHILE_TIMER);
	}

	@Test
	public void testSetPortID() throws Exception {
		timerExp1.setPortID(10);
	}

	@Test
	public void testGetPortID() throws Exception {
		timerExp1.getPortID();
	}

	@Test
	public void testGetTimerWheelType() throws Exception {
		timerExp1.getTimerWheelType();
	}

	@Test
	public void testSetTimerWheelType() throws Exception {
		timerExp1.setTimerWheelType(timerWheeltype.PERIODIC_TIMER);
	}

        @Test
        public void testSetSwitchId() throws Exception {
                timerExp1.setSwitchID(1L);
        }


        @Test
        public void testGetSwitchId() throws Exception {
                timerExp1.setSwitchID(1L);
                assertTrue(timerExp1.getSwitchID() == 1L);
        }

}
