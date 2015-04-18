
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.packet.rev150210.LacpPacketPdu;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;

public class LacpTxQueueTest {
        LacpTxQueue txInst = LacpTxQueue.getLacpTxQueueInstance();
        LacpPacketPdu obj1, obj2, obj3, obj4;
        
        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                txInst.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
	        obj1 = Mockito.mock(LacpPacketPdu.class);
        	obj2 = Mockito.mock(LacpPacketPdu.class);
	        obj3 = Mockito.mock(LacpPacketPdu.class);
        	obj4 = Mockito.mock(LacpPacketPdu.class);
		txInst.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj1);
		txInst.enqueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE, (LacpPacketPdu) obj1);
        }

        @After
        public void tearDown() throws Exception {
                
        }

        @Test
        public void testLacpTxQueue() throws Exception {
                txInst.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj2);
                txInst.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj3);
                txInst.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj4);
        }

        @Test
        public void testGetLacpTxQueueInstance() throws Exception {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
        }

        @Test
        public void testIsLacpQueuePresent() throws Exception {
                txInst.isLacpQueuePresent(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
		txInst.isLacpQueuePresent(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        }

        @Test
        public void testEnqueueIntLacpPDU() throws Exception {
        }

        @Test
        public void testDequeueInt() throws Exception {
		txInst.dequeue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);	
		txInst.dequeue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        }

        @Test
        public void testAddLacpQueue() throws Exception {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        }

        @Test
        public void testDeleteLacpQueue() throws Exception {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        }

        @Test
        public void testGetLacpQueueSize() throws Exception {
                LacpTxQueue txInst1 = LacpTxQueue.getLacpTxQueueInstance();
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst1.addLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
                txInst1.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj1);
                txInst1.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj2);
                txInst1.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj3);
                txInst1.enqueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE, (LacpPacketPdu) obj4);
		txInst1.enqueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE, (LacpPacketPdu) obj4);
		assertTrue(txInst1.getLacpQueueSize(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE) != 0);
		assertTrue(txInst1.getLacpQueueSize(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE) != 0);
                txInst1.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE);
                txInst1.deleteLacpQueue(LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE);
        }
}
