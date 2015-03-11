
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.opendaylight.lacp.queue;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.queue.LacpPDU;
import org.opendaylight.lacp.queue.LacpPDUPortStatusContainer;

public class LacpTxQueueTest {
        LacpTxQueue txInst = LacpTxQueue.getLacpTxQueueInstance();
        LacpPDUPortStatusContainer obj1, obj2, obj3, obj4;
        
        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                txInst.addLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst.addLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                obj1 = new LacpPDU(1L, 1);
                obj2 = new LacpPDU(1L, 2);
                obj3 = new LacpPDU(1L, 3);
                obj4 = new LacpPDU(1L, 4);

        }

        @After
        public void tearDown() throws Exception {
                
        }

        @Test
        public void testLacpTxQueue() {
                txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj1);
                txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj2);
                txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj3);
                txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj4);
                //fail("Not yet implemented");
        }

        @Test
        public void testGetLacpTxQueueInstance() {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                //fail("Not yet implemented");
        }

        @Test
        public void testIsLacpQueuePresent() {
                txInst.isLacpQueuePresent(LacpTxQueue.LACP_TX_NTT_QUEUE);
                
                //fail("Not yet implemented");
        }

        @Test
        public void testEnqueueIntLacpPDU() {
                // fail("Not yet implemented");
        }

        @Test
        public void testDequeueInt() {
                
                //fail("Not yet implemented");
        }

        @Test
        public void testAddLacpQueue() {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                //fail("Not yet implemented");
        }

        @Test
        public void testDeleteLacpQueue() {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                //fail("Not yet implemented");
        }

        @Test
        public void testGetLacpQueueSize() {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                txInst1.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj1);
                txInst1.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj2);
                txInst1.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj3);
                txInst1.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj4);
                txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
                //txInst1.deleteLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
                //fail("Not yet implemented");
        }
}
