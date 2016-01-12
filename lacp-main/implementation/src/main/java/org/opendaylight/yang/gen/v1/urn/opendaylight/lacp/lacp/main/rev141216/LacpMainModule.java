package org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.opendaylight.lacp.packethandler.PduDecoderProcessor;
import org.opendaylight.lacp.packethandler.TxProcessor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.lacp.grouptbl.*;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.inventorylistener.LacpNodeListener;
import org.opendaylight.lacp.inventorylistener.LacpDataListener;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.inventory.LacpLogPort;
import org.opendaylight.lacp.packethandler.LacpPacketHandler;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.lacp.queue.LacpRxQueue;
import org.opendaylight.lacp.queue.LacpTxQueue;
import org.opendaylight.lacp.role.LacpEntityManager;
import org.opendaylight.lacp.util.LacpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpMainModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216.AbstractLacpMainModule {

    private final static Logger LOG = LoggerFactory.getLogger(LacpMainModule.class);
    private LacpDataListener portDataListener;
    private LacpPacketHandler lacpPacketHandler;
    private Registration packetListener = null;
    private Registration portStatusListener = null;
    private LacpFlow lacpFlow;
    private LacpSystem lacpSystem;
    private LacpEntityManager entManager;

    private final ExecutorService pduDecoderExecutor = Executors.newCachedThreadPool();
    private final ExecutorService TxThrExecutor = Executors.newFixedThreadPool(10);
 

    public LacpMainModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LacpMainModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216.LacpMainModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance()
    {
	LacpTxQueue.QueueType queueId = LacpTxQueue.QueueType.LACP_TX_NTT_QUEUE;
        LOG.info("createInstance invoked for the lacp  module.");
        NotificationProviderService notificationService = getNotificationServiceDependency();
        DataBroker dataService = getDataBrokerDependency();
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);
	SalGroupService salGroupService = rpcRegistryDependency.getRpcService (SalGroupService.class);

        lacpSystem = LacpSystem.getLacpSystem();
        LacpNodeExtn.setDataBrokerService(dataService);
        lacpFlow = new LacpFlow();
        lacpFlow.setSalFlowService(salFlowService);
        lacpFlow.setLacpFlowHardTime(getLacpFlowHardTimeout());
        lacpFlow.setLacpFlowIdleTime(getLacpFlowIdleTimeout());
        lacpFlow.setLacpFlowPriority(getLacpFlowPriority());
        lacpFlow.setLacpFlowTableId(getLacpFlowTableId());
        LacpUtil.setDataBrokerService(dataService);
        LacpPort.setDataBrokerService(dataService);
        LacpUtil.setSalGroupService(salGroupService);
        LacpLogPort.setNotificationService(notificationService);
        portDataListener = new LacpDataListener (dataService);
        portDataListener.registerDataChangeListener();
        LacpNodeListener.setLacpSystem(lacpSystem);
        LacpNodeListener nodeListener = LacpNodeListener.getNodeListenerInstance();
        portStatusListener = notificationService.registerNotificationListener(nodeListener);

        lacpPacketHandler = new LacpPacketHandler();
        LacpPacketHandler.setDataBrokerService(dataService);
        lacpPacketHandler.updateQueueId(LacpRxQueue.getLacpRxQueueId());
        packetListener = notificationService.registerNotificationListener(lacpPacketHandler);
        LOG.debug ("started the packethandler to receive lacp pdus");
        entManager = new LacpEntityManager(getOwnershipServiceDependency());


	PacketProcessingService packetProcessingService =
	    rpcRegistryDependency.getRpcService(PacketProcessingService.class);
	
	/* Spawn the Default threads - PDU Decoder and Tx Threads */

	pduDecoderExecutor.submit(new PduDecoderProcessor());

	for (int i=0; i<4; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId,packetProcessingService));
	}
	queueId = LacpTxQueue.QueueType.LACP_TX_PERIODIC_QUEUE;
	for (int i=0; i<6; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId,packetProcessingService));
	}

        final class CloseLacpResources implements AutoCloseable {
        @Override
          public void close() throws Exception {
            
            if (packetListener != null)
            {
                packetListener.close();
            }
            if (portStatusListener != null)
            {
                portStatusListener.close();
            }
            portDataListener.closeListeners();
            entManager.closeListeners();

            LOG.info("closed the listeners for lacp. Clearing the cached info.");
            /* clean up the nodes and nodeconnectors learnt by lacp */
            lacpSystem.clearResources();
            TxThrExecutor.shutdown();
            pduDecoderExecutor.shutdown();
            return;
          }
        }
        AutoCloseable ret = new CloseLacpResources();
        LOG.info("Lacp(instance {}) initialized.", ret);
        return ret;
    }

}
