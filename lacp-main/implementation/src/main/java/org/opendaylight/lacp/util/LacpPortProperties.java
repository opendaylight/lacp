/*
 * * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.lacp.Utils;

import java.util.StringTokenizer;
import java.io.UnsupportedEncodingException;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.CommonPort.PortNumber;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.controller.config.api.DependencyResolver; 
import org.opendaylight.controller.config.api.ModuleIdentifier; 
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType; 
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException; 
import org.opendaylight.lacp.core.LacpConst;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LacpPortProperties {


    private static final Logger log = LoggerFactory.getLogger(LacpPortProperties.class);

    public static NodeConnector getNodeConnector(DataBroker dataService, NodeConnectorRef nodeConnectorRef)
    {
        return (getNodeConnector (dataService, (InstanceIdentifier<NodeConnector>) nodeConnectorRef.getValue()));
    }
    public static NodeConnector getNodeConnector(DataBroker dataService, InstanceIdentifier<NodeConnector> ncId)
    {
	NodeConnector nc = null;
	log.debug("getNodeConnector - Entry");


	ReadOnlyTransaction readTransaction = dataService.newReadOnlyTransaction();

        try {
       		Optional<NodeConnector> dataObjectOptional = readTransaction.read(LogicalDatastoreType.OPERATIONAL, 
                                                                              ncId).get();
        	if(dataObjectOptional.isPresent()){
          		nc = (NodeConnector) dataObjectOptional.get();
		}
        }catch(Exception e) {
        	log.error("Error reading node connector {}", ncId);
        	readTransaction.close();
        	throw new RuntimeException("Error reading from operational store, node connector : " + ncId, e);
        }
        readTransaction.close();
	if(nc != null){
		log.debug("getNodeConnector - nodeconnector ref obtained sucessfully");
	}else{
		log.error("getNodeConnector - nodeconnector ref cannot be obtained");
	}
	log.debug("getNodeConnector - Exit");
        return nc;

    }
    public static long getPortNumber(NodeConnector portRef){
	log.debug("getPortNumber - Entry");
	long lPort = 0;
	if(portRef != null){
		FlowCapableNodeConnector flowConnector = portRef.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
		/*
        	PortNumber number = flowConnector.getPortNumber();
		lPort = number.getUint32();
		*/
		lPort = flowConnector.getPortNumber().getUint32();
		log.debug("getPortNumber - port number obtained from nodeconnector ref is={}",lPort);
	}
	log.debug("getPortNumber - Exit");
	return lPort;
    }

     public static PortState getPortState(NodeConnector portRef){
	log.debug("getPortState - Entry");
	FlowCapableNodeConnector flowCapNodeConn = portRef.getAugmentation(FlowCapableNodeConnector.class);
	PortState ps = flowCapNodeConn.getState();
	if(ps != null){
		boolean isBlocked = ps.isBlocked();
		boolean isLive = ps.isLive();
		boolean isLinkDown = ps.isLinkDown();
	}
	log.debug("getPortState - Exit");
	return ps;
     }

     public static int mapSpeedDuplexFromPortFeature(NodeConnector portRef){
	log.debug("mapSpeedDuplexFromPortFeature- Entry");

	 int result = 0;
	 if(portRef != null){
         	FlowCapableNodeConnector flowCapNodeConn = portRef.getAugmentation(FlowCapableNodeConnector.class);
		//PortFeatures portFeatures = flowCapNodeConn.getAdvertisedFeatures(); 
		PortFeatures portFeatures = flowCapNodeConn.getCurrentFeature(); 
	 	result = resolveBW (portFeatures);
	 	log.debug("mapSpeedDuplexFromPortFeature- result={}",result);
	 } 
	 log.debug("mapSpeedDuplexFromPortFeature- Exit");
	 return result;
     }

     private static int resolveBW(PortFeatures portFeatures) { 

	 log.debug("resolveBW - Entry");
	 int result = 0;

         if (portFeatures.isOneTbFd()) { 
		//USING HUNDERED GB BITMASK - CORRECT LATER
		result = (LacpConst.LINK_SPEED_BITMASK_100000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	log.debug("resolveBW - isOneTbFd macth");
         } else if (portFeatures.isHundredGbFd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_100000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	log.debug("resolveBW - isHundredGbFd macth");
         } else if (portFeatures.isFortyGbFd()) {
		result = (LacpConst.LINK_SPEED_BITMASK_40000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	log.debug("resolveBW - isFortyGbFd macth");
         } else if (portFeatures.isTenGbFd()) {
		result = (LacpConst.LINK_SPEED_BITMASK_10000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS; 
	 	log.debug("resolveBW - isTenGbFd macth");
         } else if (portFeatures.isOneGbHd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_1000MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
	 	log.debug("resolveBW - isOneGbHd macth");
	 } else if(portFeatures.isOneGbFd()){
		 result = (LacpConst.LINK_SPEED_BITMASK_1000MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	log.debug("resolveBW - isOneGbFd macth");
         } else if (portFeatures.isHundredMbHd()) { 
		result = (LacpConst.LINK_SPEED_BITMASK_100MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
	 	log.debug("resolveBW - isHundredMbHd macth");
	 } else if ( portFeatures.isHundredMbFd()){
		result = (LacpConst.LINK_SPEED_BITMASK_100MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	log.debug("resolveBW - isHundredMbFd macth");
         } else if (portFeatures.isTenMbHd()) { 
	 	log.debug("resolveBW - isTenMbHd macth");
		 result = (LacpConst.LINK_SPEED_BITMASK_10MBPS << LacpConst.DUPLEX_KEY_BITS) & LacpConst.SPEED_KEY_BITS;
         } else if (portFeatures.isTenMbFd()){
		result = (LacpConst.LINK_SPEED_BITMASK_10MBPS << LacpConst.DUPLEX_KEY_BITS) | LacpConst.DUPLEX_KEY_BITS;
	 	log.debug("resolveBW - isTenMbFd macth");
         } else { 
	 	log.error("resolveBW - no bandwidth macth");
             return 0; 
         } 
	 log.debug("resolveBW - Exit");
	 return result;
     } 
}
