import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.lacp.queue.LacpTimerQueue;
import org.opendaylight.lacp.timer.Utils.timerWheeltype;
import org.opendaylight.lacp.timer.TimerExpiryMessage;
import org.opendaylight.lacp.timer.Utils;

public class LacpTimerQueueTest {
        private LacpTimerQueue tmrInst, tmrInst1;
        private TimerExpiryMessage  obj5,obj6,obj7,obj8,obj9,obj10,obj11,obj12,obj13,obj14,obj15,obj16;
        
        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                tmrInst = LacpTimerQueue.getLacpTimerQueueInstance();
                 obj5 = new TimerExpiryMessage(1, 10,
                                Utils.timerWheeltype.CURRENT_WHILE_TIMER);
                 obj6 = new TimerExpiryMessage(2, 20,
                                Utils.timerWheeltype.CURRENT_WHILE_TIMER);
                 obj7 = new TimerExpiryMessage(3, 30,
                                Utils.timerWheeltype.CURRENT_WHILE_TIMER);
                 obj8 = new TimerExpiryMessage(4,40,
                                Utils.timerWheeltype.CURRENT_WHILE_TIMER);

                 obj9 = new TimerExpiryMessage(1, 10,
                                Utils.timerWheeltype.WAIT_WHILE_TIMER);
                 obj10 = new TimerExpiryMessage(2,20,
                                Utils.timerWheeltype.WAIT_WHILE_TIMER);
                 obj11 = new TimerExpiryMessage(3, 30,
                                Utils.timerWheeltype.WAIT_WHILE_TIMER);
                 obj12 = new TimerExpiryMessage(4,40,
                                Utils.timerWheeltype.WAIT_WHILE_TIMER);

                 obj13 = new TimerExpiryMessage(1,10,
                                Utils.timerWheeltype.PERIODIC_TIMER);
                 obj14 = new TimerExpiryMessage(2,20,
                                Utils.timerWheeltype.PERIODIC_TIMER);
                 obj15 = new TimerExpiryMessage(3,30,
                                Utils.timerWheeltype.PERIODIC_TIMER);
                 obj16 = new TimerExpiryMessage(4,40,
                                Utils.timerWheeltype.PERIODIC_TIMER);           
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testGetLacpTimerQueueInstance() {
                tmrInst1 = LacpTimerQueue.getLacpTimerQueueInstance();
                //fail("Not yet implemented");
        }

        @Test
        public void testIsLacpQueuePresent() {
                tmrInst.isLacpQueuePresent(1L);
        //      fail("Not yet implemented");
        }

        @Test
        public void testEnqueueLongTimerExpiryMessage() {
                //LacpPDUQueue pduQ = Mockito.mock(LacpPDUQueue.class);
                tmrInst.enqueue(1L, obj5);
                tmrInst.enqueue(1L, obj6);
                tmrInst.enqueue(1L, obj7);
                tmrInst.enqueue(1L, obj8);

                tmrInst.enqueue(1L, obj9);
                tmrInst.enqueue(1L, obj10);
                tmrInst.enqueue(1L, obj11);
                tmrInst.enqueue(1L, obj12);
                
                tmrInst.enqueue(1L, obj13);
                tmrInst.enqueue(1L, obj14);
                tmrInst.enqueue(1L, obj15);
                tmrInst.enqueue(1L, obj16);             

                System.out.println("Is 1L queue present "+tmrInst.isLacpQueuePresent(1L));              
                //fail("Not yet implemented");
        }

        @Test
        public void testDequeueLong() {
                tmrInst.dequeue(1L);
                tmrInst.dequeue(1L);
                tmrInst.dequeue(1L);
                tmrInst.dequeue(1L);
                
                //fail("Not yet implemented");
        }

        @Test
        public void testAddLacpQueue() {
                tmrInst.addLacpQueue(1L);
                tmrInst.addLacpQueue(2L);
                tmrInst.addLacpQueue(3L);
                tmrInst.addLacpQueue(4L);
                tmrInst.addLacpQueue(5L);               
                //fail("Not yet implemented");
        }

        @Test
        public void testDeleteLacpQueue() {
                tmrInst.deleteLacpQueue(1L);
                tmrInst.deleteLacpQueue(2L);
                tmrInst.deleteLacpQueue(3L);
                tmrInst.deleteLacpQueue(4L);            
                //fail("Not yet implemented");
        }

        @Test
        public void testGetLacpQueueSize() {
                System.out.println("Size of Queue"+tmrInst.getLacpQueueSize(1L));
                //fail("Not yet implemented");
        }

}

