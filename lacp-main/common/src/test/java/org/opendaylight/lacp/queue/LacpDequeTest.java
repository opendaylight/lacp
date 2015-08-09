/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpDeque;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class LacpDequeTest {
        LacpDeque<LacpPDUPortStatusContainer> lacpQ;
        LacpPDUPortStatusContainer obj1, obj2, obj3, obj4;

        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                lacpQ = new LacpDeque<LacpPDUPortStatusContainer>();
	        obj1 = Mockito.mock(LacpPDUPortStatusContainer.class);
        	obj2 = Mockito.mock(LacpPDUPortStatusContainer.class);
	        obj3 = Mockito.mock(LacpPDUPortStatusContainer.class);
        	obj4 = Mockito.mock(LacpPDUPortStatusContainer.class);
		lacpQ.enqueue(obj1);
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testIsQueuePresent() throws Exception {
                assertEquals(true, lacpQ.isQueuePresent());
        }

        @Test
        public void testEnqueue() throws Exception {
                lacpQ.enqueue(obj2);
                lacpQ.enqueue(obj3);
                lacpQ.enqueue(obj4);
        }

	@Test
	public void testAddFirst() throws Exception {
                lacpQ.enqueue(obj2);
                lacpQ.enqueue(obj3);
		lacpQ.addFirst(obj4);
		assertTrue(lacpQ.dequeue() == obj4);
	}

        @Test
        public void testDequeue() throws Exception {
                assertNotNull(lacpQ.dequeue());
        }

        @Test
        public void testHasItems() throws Exception {
                assertTrue(lacpQ.hasItems() != false);
        }

        @Test
        public void testSize() throws Exception {
		assertTrue(lacpQ.size() != 0);
        }

        @Test
        public void testRead() throws Exception {
		assertNotNull(lacpQ.read());
        }

        @Test
        public void testRemove() throws Exception {
                lacpQ.remove();
        }
}
