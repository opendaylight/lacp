package org.opendaylight.lacp.queue;

import org.opendaylight.lacp.timer.*;



public class LacpQueueTest {

    public static void main(String[] args) {

            LacpPDUQueue pduInst = LacpPDUQueue.getLacpPDUQueueInstance();

            LacpQueue<LacpRawPacket> lacpRxQueueId = new LacpQueue<LacpRawPacket>();

            boolean o1, o2, o3;
            
          //PDU object
            LacpPDUPortStatusContainer obj1 = new LacpPDU(1L, 1);
            LacpPDUPortStatusContainer obj2 = new LacpPDU(1L, 2);
            LacpPDUPortStatusContainer obj3 = new LacpPDU(1L, 3);
            LacpPDUPortStatusContainer obj4 = new LacpPDU(1L, 4);

            /*
             * Port status object
             */
            LacpPDUPortStatusContainer portStatus1 = new LacpPortStatus(1L, 1, LacpPortStatus.PORT_STATUS_DOWN);
            LacpPDUPortStatusContainer portStatus2 = new LacpPortStatus(1L, 2, LacpPortStatus.PORT_STATUS_UP);
            LacpPDUPortStatusContainer portStatus3 = new LacpPortStatus(1L, 3, LacpPortStatus.PORT_STATUS_UP);
            LacpPDUPortStatusContainer portStatus4 = new LacpPortStatus(1L, 4, LacpPortStatus.PORT_STATUS_UP);

            //Raw PDU packet object
            LacpRawPacket pdu1 = new LacpRawPacket(new String("This is a new LACPPDU1"));
            LacpRawPacket pdu2 = new LacpRawPacket(new String("This is a new LACPPDU2"));
            LacpRawPacket pdu3 = new LacpRawPacket(new String("This is a new LACPPDU3"));
            LacpRawPacket pdu4 = new LacpRawPacket(new String("This is a new LACPPDU4"));

	
            LacpTimerQueue tmrInst = LacpTimerQueue.getLacpTimerQueueInstance();

            TimerExpiryMessage obj5 = new TimerExpiryMessage(1L, 1, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
            TimerExpiryMessage obj6 = new TimerExpiryMessage(1L, 2, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
            TimerExpiryMessage obj7 = new TimerExpiryMessage(1L, 3, Utils.timerWheeltype.CURRENT_WHILE_TIMER);
            TimerExpiryMessage obj8 = new TimerExpiryMessage(1L, 4, Utils.timerWheeltype.CURRENT_WHILE_TIMER);


            TimerExpiryMessage obj9 = new TimerExpiryMessage(1L, 1, Utils.timerWheeltype.WAIT_WHILE_TIMER);
            TimerExpiryMessage obj10 = new TimerExpiryMessage(1L, 2, Utils.timerWheeltype.WAIT_WHILE_TIMER);
            TimerExpiryMessage obj11 = new TimerExpiryMessage(1L, 3, Utils.timerWheeltype.WAIT_WHILE_TIMER);
            TimerExpiryMessage obj12 = new TimerExpiryMessage(1L, 4, Utils.timerWheeltype.WAIT_WHILE_TIMER);

            TimerExpiryMessage obj13 = new TimerExpiryMessage(1L, 1, Utils.timerWheeltype.PERIODIC_TIMER);
            TimerExpiryMessage obj14 = new TimerExpiryMessage(1L, 2, Utils.timerWheeltype.PERIODIC_TIMER);
            TimerExpiryMessage obj15 = new TimerExpiryMessage(1L, 3, Utils.timerWheeltype.PERIODIC_TIMER);
            TimerExpiryMessage obj16 = new TimerExpiryMessage(1L, 4, Utils.timerWheeltype.PERIODIC_TIMER);

            LacpTxQueue txInst = LacpTxQueue.getLacpTxQueueInstance();
	

            System.out.println("===========================================================");
            System.out.println("LacpPDUQueue");
            System.out.println("===========================================================");
            pduInst.addLacpQueue(1L);
            System.out.println("Instance" +pduInst);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,obj1);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,obj2);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,obj3);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,obj4);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            obj4 = pduInst.dequeue(1L);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            
            if(obj1.getMessageType() == LacpPDUPortStatusContainer.MessageType.LACP_PDU_MSG){
            	System.out.println("The object type is LACP PDU");
            }
            
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,portStatus1);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,portStatus2);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,portStatus3);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            o1 = pduInst.enqueue(1L,portStatus4);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            portStatus4 = pduInst.dequeue(1L);
            System.out.println("The size of the queue is "+ pduInst.getLacpQueueSize(1L));
            if(portStatus1.getMessageType() == LacpPDUPortStatusContainer.MessageType.LACP_PORT_STATUS_MSG){
            	System.out.println("The object type is LACP PORT status");
            }

		
            System.out.println("===========================================================");
            System.out.println("LacpTimerQueue");
            System.out.println("===========================================================");
            tmrInst.addLacpQueue(1L);
            System.out.println("Instance" +tmrInst);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj5);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj6);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj7);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj8);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            obj9 = tmrInst.dequeue(1L);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));


            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj9);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj10);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj11);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj12);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            obj12 = tmrInst.dequeue(1L);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));


            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj13);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj14);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj15);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            o1 = tmrInst.enqueue(1L,obj16);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));
            obj16 = tmrInst.dequeue(1L);
            System.out.println("The size of the queue is "+ tmrInst.getLacpQueueSize(1L));

            System.out.println("===========================================================");
            System.out.println("Lacp Tx NTT Queue");
            System.out.println("===========================================================");

            txInst.addLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);
            txInst.addLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);
            System.out.println("Instance" +txInst);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU) obj1);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU)obj2);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU)obj3);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_NTT_QUEUE, (LacpPDU)obj4);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            obj4 = (LacpPDU) txInst.dequeue(LacpTxQueue.LACP_TX_NTT_QUEUE);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_NTT_QUEUE));
            System.out.println("===========================================================");
            System.out.println("Lacp Tx Periodic Queue");
            System.out.println("===========================================================");
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_PERIODIC_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE, (LacpPDU)obj1);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_PERIODIC_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE, (LacpPDU)obj2);
            System.out.println("The size of the queue is "+ txInst.getLacpQueueSize(LacpTxQueue.LACP_TX_PERIODIC_QUEUE));
            o1 = txInst.enqueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE, (LacpPDU)obj3);



            System.out.println("===========================================================");
            System.out.println("LacpRxQueue");
            System.out.println("===========================================================");
            System.out.println("Instance" +lacpRxQueueId);
            System.out.println("The size of the queue is "+ lacpRxQueueId.size());
            o1 = lacpRxQueueId.enqueue(pdu1);
            System.out.println("The size of the queue is "+ lacpRxQueueId.size());
            o1 = lacpRxQueueId.enqueue(pdu2);
            System.out.println("The size of the queue is "+ lacpRxQueueId.size());
            o1 = lacpRxQueueId.enqueue(pdu3);
            System.out.println("The size of the queue is "+ lacpRxQueueId.size());
            o1 = lacpRxQueueId.enqueue(pdu4);
            System.out.println("The size of the queue is "+ lacpRxQueueId.size());
            pdu4 = lacpRxQueueId.dequeue();
            System.out.println("The size of the queue is "+ lacpRxQueueId.size()+" raw pdu data is = " + pdu4.getRawPdu());

            System.out.println("===========================================================");
            System.out.println("Deleting the entries in the PDU Queue");
            System.out.println("===========================================================");
            pduInst.deleteLacpQueue(1L);

            System.out.println("===========================================================");
            System.out.println("Deleting the entries in the Timer Queue");
            System.out.println("===========================================================");
            tmrInst.deleteLacpQueue(1L);

            System.out.println("===========================================================");
            System.out.println("Deleting the entries in the Tx Periodic Queue");
            System.out.println("===========================================================");
            txInst.deleteLacpQueue(LacpTxQueue.LACP_TX_PERIODIC_QUEUE);

            System.out.println("===========================================================");
            System.out.println("Deleting the entries in the Tx NTT Queue");
            System.out.println("===========================================================");
            txInst.deleteLacpQueue(LacpTxQueue.LACP_TX_NTT_QUEUE);

            System.out.println("===========================================================");
            System.out.println("Deleting the entries in the Raw packet Queue");
            System.out.println("===========================================================");
            lacpRxQueueId.remove();



            System.out.println("The size of the PDU queue is "+ pduInst.getLacpQueueSize(1L));
            System.out.println("The size of the Timer queue is "+ tmrInst.getLacpQueueSize(1L));
            System.out.println("The size of the RawPacket Queue is  "+ lacpRxQueueId.size());

    }
}

