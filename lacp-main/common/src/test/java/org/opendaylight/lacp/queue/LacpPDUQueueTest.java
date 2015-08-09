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

import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class LacpPDUQueueTest {
        private LacpPDUQueue pduQueue, pduInst;
        private LacpPDUPortStatusContainer obj1,obj2,obj3,obj4, obj5;


        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
         pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
         obj1 = Mockito.mock(LacpPDUPortStatusContainer.class);
         obj2 = Mockito.mock(LacpPDUPortStatusContainer.class);
         obj3 = Mockito.mock(LacpPDUPortStatusContainer.class);
         obj4 = Mockito.mock(LacpPDUPortStatusContainer.class);
         obj5 = Mockito.mock(LacpPDUPortStatusContainer.class);
	 pduQueue.addLacpQueue(1L);
	 pduQueue.enqueue(1L, obj1);
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testLacpPDUQueue() throws Exception{
        }

        @Test
        public void testGetLacpPDUQueueInstance() throws Exception{
                pduInst = LacpPDUQueue.getLacpPDUQueueInstance();
        }

        @Test
        public void testIsLacpQueuePresent() throws Exception{
		assertNotNull(pduQueue.isLacpQueuePresent(1L));
        }

        @Test
        public void testRead() throws Exception{
                assertTrue(pduQueue.read(1L) != null);
        }


        @Test
        public void testEnqueueLongLacpPDUPortStatusContainer() throws Exception{
                LacpPDUQueue pduQ = LacpPDUQueue.getLacpPDUQueueInstance();
                pduQ.addLacpQueue(1L);
                pduQ.addLacpQueue(2L);
                pduQ.addLacpQueue(3L);
                pduQ.addLacpQueue(4L);
                pduQ.addLacpQueue(5L);
                pduQ.enqueue(1L, obj1);
                pduQ.enqueue(1L, obj2);
                pduQ.enqueue(1L, obj3);
                pduQ.enqueue(1L, obj4);

        }

         @Test
         public void testEnqueueFirstLongLacpPDUPortStatusContainer() throws Exception{
    		 pduQueue.addLacpQueue(1L);
                 pduQueue.enqueueAtFront(1L,obj5);
         }

        @Test
        public void testDequeueLong() throws Exception{
                assertTrue(pduQueue.dequeue(1L) != null);
                assertTrue(pduQueue.dequeue(2L) == null);

        }

        @Test
        public void testAddLacpQueue() throws Exception{
                pduQueue.addLacpQueue(1L);
                pduQueue.addLacpQueue(2L);
                pduQueue.addLacpQueue(3L);
                pduQueue.addLacpQueue(4L);
                pduQueue.addLacpQueue(5L);
		assertNotNull(pduQueue.isLacpQueuePresent(1L));
		assertNotNull(pduQueue.isLacpQueuePresent(2L));
		assertNotNull(pduQueue.isLacpQueuePresent(3L));
		assertNotNull(pduQueue.isLacpQueuePresent(4L));
		assertNotNull(pduQueue.isLacpQueuePresent(5L));
        }

        @Test
        public void testDeleteLacpQueue() throws Exception{
                pduQueue.deleteLacpQueue(1L);
                pduQueue.deleteLacpQueue(2L);
                pduQueue.deleteLacpQueue(3L);
                pduQueue.deleteLacpQueue(4L);
		assertTrue(pduQueue.isLacpQueuePresent(1L) == false);
		assertTrue(pduQueue.isLacpQueuePresent(2L) == false);
		assertTrue(pduQueue.isLacpQueuePresent(3L) == false);
		assertTrue(pduQueue.isLacpQueuePresent(4L) == false);

        }

        @Test
        public void testGetLacpQueueSize() throws Exception{
                assertTrue(pduQueue.getLacpQueueSize(1L) != 0);
        }

        @Test
        public void testIsQueuePresent()  throws Exception  {
                try{
                pduQueue.addLacpQueue(1L);
                assertTrue(pduQueue.isLacpQueuePresent(1L) == true);
		assertTrue(pduQueue.isLacpQueuePresent(2L) == true);
		assertTrue(pduQueue.isLacpQueuePresent(10L) == false);
                }catch(Exception ex){
                        System.out.println("catched Exception");
                }
        }

}
