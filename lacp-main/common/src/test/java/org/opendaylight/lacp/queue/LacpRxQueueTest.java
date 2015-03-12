
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpRxQueue;
import org.opendaylight.lacp.queue.LacpPDU;
import org.opendaylight.lacp.queue.LacpRawPacket;
import org.opendaylight.lacp.queue.LacpQueue;

public class LacpRxQueueTest {
        LacpQueue<LacpRawPacket> lacpRxQueueId;

        @BeforeClass
        public static void setUpBeforeClass() throws Exception {
        }

        @AfterClass
        public static void tearDownAfterClass() throws Exception {
        }

        @Before
        public void setUp() throws Exception {
                lacpRxQueueId = LacpRxQueue.getLacpRxQueueId();
        }

        @After
        public void tearDown() throws Exception {
        }

        @Test
        public void testGetLacpRxQueueId() {
                LacpQueue<LacpRawPacket> lacpRxQueueId1 =  LacpRxQueue.getLacpRxQueueId(); 
                //System.out.println("The rx queue is "+ lacpRxQueueId , lacpRxQueueId1);
                //fail("Not yet implemented");
        }
}
