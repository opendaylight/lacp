
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.lacp.queue.LacpRxQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.lacp.queue.LacpQueue;

public class LacpRxQueueTest {
        LacpQueue<PacketReceived> lacpRxQueueId;

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
        public void testGetLacpRxQueueId() throws Exception {
                LacpQueue<PacketReceived> lacpRxQueueId1 =  LacpRxQueue.getLacpRxQueueId(); 
        }
}
