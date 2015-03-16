package org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.opendaylight.lacp.packethandler.PduDecoderProcessor;
import org.opendaylight.lacp.packethandler.TxProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
import org.opendaylight.lacp.inventoryListener.LacpNodeListener;
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpMainModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216.AbstractLacpMainModule {

    private final static Logger log = LoggerFactory.getLogger(LacpMainModule.class);
    /*
    private LacpNodeListener lacpListener;
    */
    private Registration listenerRegistration = null;

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
        NotificationProviderService notificationService = getNotificationServiceDependency ();
        DataBroker dataService = getDataBrokerDependency ();
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency ();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService (SalFlowService.class);

        /*
        lacpListener = new LacpNodeListener(dataService, salFlowService);
        lacpListener.setLacpFlowHardTime (getLacpFlowHardTimeout());
        lacpListener.setLacpFlowIdleTime (getLacpFlowIdleTimeout());
        lacpListener.setLacpFlowPriority (getLacpFlowPriority());
        lacpListener.setLacpFlowTableId (getLacpFlowTableId());
        listenerRegistration  = notificationService.registerNotificationListener(lacpListener);
        */

	/* Spawn the Default threads - PDU Decoder and Tx Threads */

	pduDecoderExecutor.submit(new PduDecoderProcessor());

	for (int i=0; i<4; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId));
	}
	queueId = 1;
	for (int i=0; i<6; i++) {
		TxThrExecutor.submit(new TxProcessor(queueId));
	}

        final class CloseLacpResources implements AutoCloseable {
        @Override
          public void close() throws Exception {
            if (listenerRegistration != null)
            {
                listenerRegistration.close ();
            } 
            return;
          }
        }
        AutoCloseable ret = new CloseLacpResources();
        log.info("Lacp(instance {}) initialized.", ret);
        System.out.println ("lacp instance initialized."+ ret);
        return ret;
    }

}
