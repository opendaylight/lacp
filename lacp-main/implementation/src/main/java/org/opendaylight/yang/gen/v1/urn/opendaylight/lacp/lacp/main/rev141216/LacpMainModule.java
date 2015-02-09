package org.opendaylight.yang.gen.v1.urn.opendaylight.lacp.lacp.main.rev141216;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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
