
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.timer.*;
public class BasePortTimerRegisterTest {
	BasePortTimerRegister baseTimerReg, b1;	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		baseTimerReg = new BasePortTimerRegister((short)10, 20L);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBasePortTimerRegister() {
		BasePortTimerRegister b1 = new BasePortTimerRegister((short)30,40L);
		b1.setPortID((short)10);
		b1.setSystemID(10L);
		//fail("Not yet implemented");
	}

	@Test
	public void testSetPortID() {
		baseTimerReg.setPortID((short)100);
		//fail("Not yet implemented");
	}

	@Test
	public void testSetSystemID() {
		baseTimerReg.setSystemID(200L);
		//fail("Not yet implemented");
	}

	@Test
	public void testGetPortID() {
		baseTimerReg.getPortID();
		//fail("Not yet implemented");
	}

	@Test
	public void testGetSystemID() {
		baseTimerReg.getSystemID();
		//fail("Not yet implemented");
	}

}
