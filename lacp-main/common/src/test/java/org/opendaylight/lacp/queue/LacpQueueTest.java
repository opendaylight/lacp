

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpQueue;
import org.opendaylight.lacp.queue.LacpPDU;

public class LacpQueueTest {
        LacpQueue<LacpPDU> lacpQ;
        LacpPDU obj1, obj2, obj3, obj4;
        
        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                lacpQ = new LacpQueue<LacpPDU>();
                obj1 = new LacpPDU(1L, 1);
                obj2 = new LacpPDU(1L, 2);
                obj3 = new LacpPDU(1L, 3);
                obj4 = new LacpPDU(1L, 4);              
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testIsQueuePresent() {
                System.out.println("The lacp queue present result"+lacpQ.isQueuePresent());
                //fail("Not yet implemented");
        }

        @Test
        public void testEnqueue() {
                lacpQ.enqueue(obj1);
                lacpQ.enqueue(obj2);
                lacpQ.enqueue(obj3);
                lacpQ.enqueue(obj4);
                
                //fail("Not yet implemented");
        }

        @Test
        public void testDequeue() {
                System.out.println("The dequeue element is "
+lacpQ.dequeue());
                System.out.println("The dequeue element is "
+lacpQ.dequeue());
                System.out.println("The dequeue element is "
+lacpQ.dequeue());
                System.out.println("The dequeue element is "
+lacpQ.dequeue());
                System.out.println("The dequeue element is "
+lacpQ.dequeue());
                //fail("Not yet implemented");
        }

        @Test
        public void testHasItems() {
                System.out.println("The lacp queue haselement" +
lacpQ.hasItems());
                //fail("Not yet implemented");
        }

        @Test
        public void testSize() {
                System.out.println("The lacp queue size" +lacpQ.size());
                //fail("Not yet implemented");
        }

        @Test
        public void testRemove() {
                lacpQ.remove();
                //fail("Not yet implemented");
        }
}
