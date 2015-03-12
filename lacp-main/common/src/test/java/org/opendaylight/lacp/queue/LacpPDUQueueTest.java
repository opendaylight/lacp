import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.lacp.queue.LacpPDUQueue;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;
import org.opendaylight.lacp.queue.LacpPDU;
//import org.opendaylight.lacp.queue.*;

public class LacpPDUQueueTest {
        private LacpPDUQueue pduQueue, pduInst;
        private LacpPDUPortStatusContainer obj1,obj2,obj3,obj4;

        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                pduQueue = LacpPDUQueue.getLacpPDUQueueInstance();
         obj1 = new LacpPDU(1L, 1);
         obj2 = new LacpPDU(1L, 2);
         obj3 = new LacpPDU(1L, 3);
         obj4 = new LacpPDU(1L, 4);
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testLacpPDUQueue() {
        //      fail("Not yet implemented");
        }

        @Test
        public void testGetLacpPDUQueueInstance() {
                pduInst = LacpPDUQueue.getLacpPDUQueueInstance();
        //      fail("Not yet implemented");
        }

        @Test
        public void testIsLacpQueuePresent() {
                pduQueue.isLacpQueuePresent(1L);
        //      fail("Not yet implemented");
        }

        @Test
        public void testEnqueueLongLacpPDUPortStatusContainer() {
                pduQueue.enqueue(1L,obj1);
                LacpPDUQueue pduQ = Mockito.mock(LacpPDUQueue.class);
                pduQ.enqueue(1L, obj1);
                pduQ.enqueue(1L, obj2);
                pduQ.enqueue(1L, obj3);
                pduQ.enqueue(1L, obj4);

        }

        @Test
        public void testDequeueLong() {
                pduQueue.dequeue(1L);
                pduQueue.dequeue(1L);
                pduQueue.dequeue(1L);

        }

        @Test
        public void testAddLacpQueue() {
                pduQueue.addLacpQueue(1L);
                pduQueue.addLacpQueue(2L);
                pduQueue.addLacpQueue(3L);
                pduQueue.addLacpQueue(4L);
                pduQueue.addLacpQueue(5L);

        }

        @Test
        public void testDeleteLacpQueue() {
                pduQueue.deleteLacpQueue(1L);
                pduQueue.deleteLacpQueue(2L);
                pduQueue.deleteLacpQueue(3L);
                pduQueue.deleteLacpQueue(4L);

        }

        @Test
        public void testGetLacpQueueSize() {
                System.out.println("Size of Queue"+pduQueue.size());
        }

        @Test
        public void testIsQueuePresent()  throws Exception  {
                try{
                pduQueue.addLacpQueue(1L);
                System.out.println("Is 1L queue present "+pduQueue.isLacpQueuePresent(1L));
                System.out.println("Is 2L queue present "+pduQueue.isLacpQueuePresent(2L));
                System.out.println("Is 10L queue present "+pduQueue.isLacpQueuePresent(10L));
                }catch(Exception ex){
                        System.out.println("catched Exception");
                }

        }

}

