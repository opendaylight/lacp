/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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

public class TimerFactoryTest {
	LacpWheelTimer instance1, instance2, instance3;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		instance1 = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.WAIT_WHILE_TIMER);
		instance2 = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.PERIODIC_TIMER);
		instance3 = TimerFactory.LacpWheelTimer.getInstance(Utils.timerWheeltype.CURRENT_WHILE_TIMER);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		Timeout obj = null;

		PortWaitWhileTimerRegister objPort = new PortWaitWhileTimerRegister((short)10,1L);
		obj=instance1.registerPortForWaitWhileTimer(objPort, 5, TimeUnit.SECONDS);
//		obj.cancel();
		instance1.CancelPortForWaitWhileTimer(obj);

		PortPeriodicTimerRegister objPort1 = new PortPeriodicTimerRegister((short)10,1L);
		obj=instance1.registerPortForWaitWhileTimer(objPort1, 5, TimeUnit.SECONDS);
//		obj.cancel();
		instance1.CancelPortForPeriodicTimer(obj);

		PortCurrentWhileTimerRegister objPort2 = new PortCurrentWhileTimerRegister((short)10,1L);
		obj=instance1.registerPortForWaitWhileTimer(objPort2, 5, TimeUnit.SECONDS);
		instance1.CancelCurrentWhileTimer(obj);
	}
}
