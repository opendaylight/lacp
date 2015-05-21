/*
 * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.lacp.Utils;


import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.lacp.core.LacpConst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpPortProperties {


    private static final Logger LOG = LoggerFactory.getLogger(LacpPortProperties.class);

    public static NodeConnector getNodeConnector(DataBroker dataService, NodeConnectorRef nodeConnectorRef)
    {
        return (getNodeConnector (dataService, (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue()));
    }
    public static NodeConnector getNodeConnector(DataBroker dataService, InstanceIdentifier<NodeConnector> ncId)
    {
	NodeConnector nc = null;
	LOG.debug("getNodeConnector - Entry");


	ReadOnlyTransaction readTransaction = dataService.newReadOnlyTransaction();

        try {
       		Optional<NodeConnector> dataObjectOptional = readTransaction.read(LogicalDatastoreType.OPERATIONAL, 
                                                                              ncId).get();
        	if(dataObjectOptional.isPresent()){
          		nc = (NodeConnector) dataObjectOptional.get();
		}
        }catch(Exception e) {
        	LOG.error("Error reading node connector {}", ncId);
        	readTransaction.close();
        	throw new RuntimeException("Error reading from operational store, node connector : " + ncId, e);
        }
        readTransaction.close();
	if(nc != null){
		LOG.debug("getNodeConnector - nodeconnector ref obtained sucessfully");
	}else{
		LOG.error("getNodeConnector - nodeconnector ref cannot be obtained");
	}
	LOG.debug("getNodeConnector - Exit");
        return nc;

    }
     public static int mapSpeedDuplexFromPortFeature(NodeConnector portRef){
	LOG.debug("mapSpeedDuplexFromPortFeature- Entry");

	 int result = 0;
	 if(portRef != null){
         	FlowCapableNodeConnector flowCapNodeConn = portRef.getAugmentation(FlowCapableNodeConnector.class);
		PortFeatures portFeatures = flowCapNodeConn.getCurrentFeature(); 
	 	result = resolveBW (portFeatures);
	 	LOG.debug("mapSpeedDuplexFromPortFeature- result={}",result);
	 } 
	 LOG.debug("mapSpeedDuplexFromPortFeature- Exit");
	 return result;
     }

     private static int resolveBW(PortFeatures portFeatures) { 

	 LOG.debug("resolveBW - Entry");
	 int result = 0;

         if (portFeatures.isOneTbFd()) { 
		//USING HUNDERED GB BITMASK - CORRECT LATER
		result = (LacpConst.LINK_SPEED_BITMASK_100000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	LOG.debug("resolveBW - isOneTbFd macth");
         } else if (portFeatures.isHundredGbFd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_100000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	LOG.debug("resolveBW - isHundredGbFd macth");
         } else if (portFeatures.isFortyGbFd()) {
		result = (LacpConst.LINK_SPEED_BITMASK_40000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	LOG.debug("resolveBW - isFortyGbFd macth");
         } else if (portFeatures.isTenGbFd()) {
		result = (LacpConst.LINK_SPEED_BITMASK_10000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	LOG.debug("resolveBW - isTenGbFd macth");
         } else if (portFeatures.isOneGbHd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_1000MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
	 	LOG.debug("resolveBW - isOneGbHd macth");
	 } else if(portFeatures.isOneGbFd()){
		 result = (LacpConst.LINK_SPEED_BITMASK_1000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	LOG.debug("resolveBW - isOneGbFd macth");
         } else if (portFeatures.isHundredMbHd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_100MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
	 	LOG.debug("resolveBW - isHundredMbHd macth");
	 } else if ( portFeatures.isHundredMbFd()){
		result = (LacpConst.LINK_SPEED_BITMASK_100MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	LOG.debug("resolveBW - isHundredMbFd macth");
         } else if (portFeatures.isTenMbHd()) { 
	 	LOG.debug("resolveBW - isTenMbHd macth");
		 result = (LacpConst.LINK_SPEED_BITMASK_10MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
         } else if (portFeatures.isTenMbFd()){
		result = (LacpConst.LINK_SPEED_BITMASK_10MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	LOG.debug("resolveBW - isTenMbFd macth");
         } else { 
	 	LOG.error("resolveBW - no bandwidth macth");
             return 0; 
         } 
	 LOG.debug("resolveBW - Exit");
	 return result;
     } 
}
