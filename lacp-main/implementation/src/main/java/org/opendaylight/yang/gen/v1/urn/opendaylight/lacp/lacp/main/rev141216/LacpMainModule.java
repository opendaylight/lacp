package org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
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
import org.opendaylight.lacp.inventorylistener.LacpNodeListener;
import org.opendaylight.lacp.inventorylistener.LacpDataListener;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.packethandler.LacpPacketHandler;
import org.opendaylight.lacp.packethandler.TxUtils;
import org.opendaylight.lacp.flow.LacpFlow;
import org.opendaylight.lacp.queue.LacpRxQueue;
import org.opendaylight.lacp.util.LacpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpMainModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216.AbstractLacpMainModule {

    private final static Logger log = LoggerFactory.getLogger(LacpMainModule.class);
    private LacpNodeListener lacpListener;
    private Registration nodeListener = null;
    private LacpDataListener portDataListener;
    private Registration extPortListener = null;
    private LacpPacketHandler lacpPacketHandler;
    private Registration packetListener = null;
    private LacpFlow lacpFlow;
    private LacpSystem lacpSystem;

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
    public java.lang.AutoCloseable createInstance() {
	int queueId = 0;
        log.info("createInstance invoked for the lacp  module.");
        NotificationProviderService notificationService = getNotificationServiceDependency();
        DataBroker dataService = getDataBrokerDependency();
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);
	SalGroupService salGroupService = rpcRegistryDependency.getRpcService (SalGroupService.class);

        lacpSystem = LacpSystem.getLacpSystem();
        LacpNodeExtn.setDataBrokerService(dataService);
        lacpListener = new LacpNodeListener(lacpSystem);
        nodeListener = notificationService.registerNotificationListener(lacpListener);
        lacpFlow = new LacpFlow();
        lacpFlow.setSalFlowService(salFlowService);
        lacpFlow.setLacpFlowHardTime(getLacpFlowHardTimeout());
        lacpFlow.setLacpFlowIdleTime(getLacpFlowIdleTimeout());
        lacpFlow.setLacpFlowPriority(getLacpFlowPriority());
        lacpFlow.setLacpFlowTableId(getLacpFlowTableId());
        LacpUtil.setDataBrokerService(dataService);
        LacpUtil.setSalGroupService(salGroupService);
        portDataListener = new LacpDataListener (dataService);
        extPortListener = portDataListener.registerDataChangeListener();


        log.debug("starting to read from data store");
        lacpSystem.readDataStore(dataService);

        lacpPacketHandler = new LacpPacketHandler();
        LacpPacketHandler.setDataBrokerService(dataService);
        lacpPacketHandler.updateQueueId(LacpRxQueue.getLacpRxQueueId());
        packetListener = notificationService.registerNotificationListener(lacpPacketHandler);

	LacpGroupTbl lacpGroupTbl = new LacpGroupTbl(salGroupService, dataService);

	PacketProcessingService packetProcessingService =
	    rpcRegistryDependency.getRpcService(PacketProcessingService.class);
	
	/*
	 if(packetProcessingService != null){
               System.out.println("LacpMainModule - packetProcessingService is NOT null");
         }else{
               System.out.println("LacpMainModule - packetProcessingService is null");
         }

	TxUtils.setPacketProcessingService(packetProcessingService);
	*/

	/* Spawn the Default threads - PDU Decoder and Tx Threads */

	pduDecoderExecutor.submit(new PduDecoderProcessor());

	for (int i=0; i<4; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId,packetProcessingService));
	}
	queueId = 1;
	for (int i=0; i<6; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId,packetProcessingService));
	}

        final class CloseLacpResources implements AutoCloseable {
        @Override
          public void close() throws Exception {
            if (nodeListener != null)
            {
                nodeListener.close();
            }
            if (packetListener != null)
            {
                packetListener.close();
            }
            if (extPortListener != null)
            {
                extPortListener.close();
            }
            /* clean up the nodes and nodeconnectors learnt by lacp */
            lacpSystem.clearResources();
            return;
          }
        }
        AutoCloseable ret = new CloseLacpResources();
        log.info("Lacp(instance {}) initialized.", ret);
        return ret;
    }

}
