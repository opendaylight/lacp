/*
 *  * Copyright (c) 2014 Dell Inc. and others.  All rights reserved.
 *   *
 *    * This program and the accompanying materials are made available under the
 *     * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *      * and is available at http://www.eclipse.org/legal/epl-v10.html
 *       */

package org.opendaylight.lacp.grouptbl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;


import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;


import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import com.google.common.base.Optional;



public class LacpGroupTbl
{
    private static final Logger LOG = LoggerFactory.getLogger(LacpGroupTbl.class);
    private final ExecutorService lacpService = Executors.newCachedThreadPool();
    private SalGroupService salGroupService;
    private DataBroker dataService;
    private final AtomicLong txNum = new AtomicLong();


    public LacpGroupTbl (SalGroupService salGroupService, DataBroker dataService)
    {
	this.salGroupService = salGroupService;
	this.dataService = dataService;
    }

    public String getNewTransactionId() {
                return "RSM-" + txNum.getAndIncrement();
    }

    //public void lacpAddGroup(Boolean isUnicastGrp, NodeConnectorRef nodeConnectorRef,
    public Group lacpAddGroup(Boolean isUnicastGrp, NodeConnectorRef nodeConnectorRef,
			     GroupId groupId)
    {
        if (nodeConnectorRef == null){
            return null;
	}
        LOG.info("LACP: lacpAddGroup ", nodeConnectorRef);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		LOG.warn("LACP: lacpAddGroup Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		LOG.warn("LACP: lacpAddGroup: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	Group grp = addGroup( true, nodeRef, nodeId , ncId, groupId);
	return grp;
    }


    //private boolean addGroup(boolean isUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
    private Group addGroup(boolean isUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId groupId) {

        boolean isGroupAdded = true, retry=false;
	int trials=0 ;

	NodeKey nodeKey = new NodeKey(nodeId);

         /* Create output action for this ncId*/
         OutputActionBuilder oab = new OutputActionBuilder();
         oab.setOutputNodeConnector(ncId);
         ActionBuilder ab = new ActionBuilder();
         ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
         LOG.debug("lacpAddGroup: addGroup", ab.build());

         AddGroupInputBuilder groupBuilder = new AddGroupInputBuilder();

         if (isUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
         }

        GroupKey groupkey = new GroupKey(groupId);
        InstanceIdentifier<Group> lacpGId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey).toInstance();

        LOG.info("addGroup: lacpGid " , lacpGId);

        groupBuilder.setGroupId(groupId);
	groupBuilder.setGroupRef(new GroupRef(lacpGId));
        groupBuilder.setGroupName("LACP"+groupId);
   	groupBuilder.setNode(nodeRef);
        groupBuilder.setBarrier(false);
        groupBuilder.setTransactionUri(new Uri (getNewTransactionId()) );

        BucketsBuilder bucketBuilder = new BucketsBuilder();
        List<Bucket> bucketList = Lists.newArrayList();
        BucketBuilder bucket = new BucketBuilder();
        bucket.setBucketId(new BucketId((long) 1));
        bucket.setKey(new BucketKey(new BucketId((long) 1)));

                /* put output action to the bucket */
        List<Action> bucketActionList = Lists.newArrayList();
        /* set order for new action and add to action list */
        ab.setOrder(bucketActionList.size());
        ab.setKey(new ActionKey(bucketActionList.size()));
        bucketActionList.add(ab.build());

        bucket.setAction(bucketActionList);
        bucketList.add(bucket.build());
        bucketBuilder.setBucket(bucketList);
        groupBuilder.setBuckets(bucketBuilder.build());



	do {
		if ( retry == true )
		{
			try {
                		Thread.sleep(1000);
            		}catch( InterruptedException e ) {
                		LOG.info("addGroup: Interrupted Exception ", e.toString());
            		}

		}
        	try {
             		Future<RpcResult<AddGroupOutput>>  result = salGroupService.addGroup(groupBuilder.build());
               		if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               		{
                  		LOG.info ("LACP: Group Additon Succeeded.");
                   		isGroupAdded = true;
	 			trials++;
				GroupBuilder retgrp = new GroupBuilder();
				retgrp.setGroupType(groupBuilder.getGroupType());
				retgrp.setGroupId(groupBuilder.getGroupId());
				retgrp.setGroupName(groupBuilder.getGroupName());
				retgrp.setContainerName(groupBuilder.getContainerName());
				retgrp.setBarrier(groupBuilder.isBarrier());
				retgrp.setBuckets(groupBuilder.getBuckets());
				return retgrp.build();
               		}	
               		else {
                   		LOG.error("LACP: Group Additon Failed.");
                    		isGroupAdded = false;
	 			trials++;
               		}
        	}	
         	catch (InterruptedException | ExecutionException | TimeoutException e)
         	{
             		LOG.error("received interrupt " + e.toString());
			retry =true;
	 		trials++;
         	}
	} while( trials<5 && (isGroupAdded==false || retry )) ;

        return null;

    }



    public Group lacpAddPort(boolean isUnicastGrp, NodeConnectorRef nodeConnectorRef, 
			   Group  origGroup) {

        if (nodeConnectorRef == null)
            return null;
	if (origGroup == null)
        {
		LOG.warn("lacpAddPort: origGroup is NULL"); 
                return null;
        }
        LOG.info("LACP: lacpAddPort ", nodeConnectorRef);
	GroupId groupId = origGroup.getGroupId();
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	LOG.info("lacpAddPort for group id " , groupId);
	if (ncId == null)
	{
		LOG.warn("LACP: lacpAddPort Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		LOG.warn("LACP: lacpAddPort: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier<Group> lacpGId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey).toInstance();
	LOG.info("lacpAddPort: lacpGid ", lacpGId);
	LOG.info("lacpAddPort:  key " , groupkey);

	Group updGroup = populateGroup(isUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup); 
	if (updGroup == null){
		LOG.warn("lacpAddPort: updGroup is NULL");
	}
	else{
   		LOG.info("lacpAddPort updGroup is available proceeding to program it");
		updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	}
	return updGroup;
   }


    public Group populateGroup(boolean isUnicastGrp, NodeRef nodeRef, NodeId nodeId,
			       NodeConnectorId ncId, GroupId groupId,
			       Group origGroup) {

	NodeKey nodeKey = new NodeKey(nodeId);


         GroupBuilder groupBuilder = new GroupBuilder();


         if (isUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
		 LOG.debug("populate group: type select");
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
		 LOG.debug("populate group: type all");
         }

        GroupKey groupkey = new GroupKey(groupId);
        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);

        groupBuilder.setGroupId(groupId);
        groupBuilder.setGroupName("LACP" + groupId);
        groupBuilder.setBarrier(false);

        BucketsBuilder bucketBuilder = new BucketsBuilder();
        List<Bucket> bucketList = Lists.newArrayList();
        BucketBuilder bucket = new BucketBuilder();
        bucket.setBucketId(new BucketId((long) 1));
        bucket.setKey(new BucketKey(new BucketId((long) 1)));
        List<Action> bucketActionList = Lists.newArrayList();

	Buckets origbuckets = origGroup.getBuckets();
	NodeConnectorId origncId;

        /* put output action to the bucket */
        /* set order for new action and add to action list */
	for (Bucket origbucket : origbuckets.getBucket()) {
		List<Action> origbucketActions = origbucket.getAction();
		for (Action action : origbucketActions) {
			if (action.getAction() instanceof OutputActionCase) {
				OutputActionCase opAction = (OutputActionCase)action.getAction();
				origncId = (NodeConnectorId) opAction.getOutputAction().getOutputNodeConnector();
				if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncId))) {
					LOG.warn("returning null here at 1");
					return(null);
				}
         			OutputActionBuilder oab = new OutputActionBuilder();
        			oab.setOutputNodeConnector(origncId);
         			ActionBuilder ab = new ActionBuilder();
         			ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        			ab.setOrder(bucketActionList.size());
        			ab.setKey(new ActionKey(bucketActionList.size()));
        			bucketActionList.add(ab.build());
			}
		   }
	    }
				
         /* Create output action for this ncId*/
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(ncId);
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        ab.setOrder(bucketActionList.size());
       	ab.setKey(new ActionKey(bucketActionList.size()));
       	bucketActionList.add(ab.build());

        bucket.setAction(bucketActionList);
        bucketList.add(bucket.build());
        bucketBuilder.setBucket(bucketList);
        groupBuilder.setBuckets(bucketBuilder.build());
	return(groupBuilder.build());

   }

   public Group getGroup(GroupKey groupkey, NodeKey nodeKey ) {

        InstanceIdentifier<Group> lacpGId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey).toInstance();
	ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
	try {
		Optional<Group> readGroup = readTx.read(LogicalDatastoreType.CONFIGURATION, lacpGId).get();
		if (readGroup.isPresent()) {
			return readGroup.get();
		}
	}  catch (InterruptedException|ExecutionException e) {
		LOG.error(e.toString(), e);
	}
	LOG.warn("readgrp returning null");
	return null;
		
   }

   public Group lacpRemPort(Group origGroup, NodeConnectorRef nodeConnectorRef, boolean isUnicastGrp) {
        if (nodeConnectorRef == null){
            return null;
	}
	if (origGroup == null)
        {
		LOG.warn("lacpAddPort: origGroup is NULL");
		return null;
	}
        LOG.info("LACP: lacpAddPort ", nodeConnectorRef);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue();
        GroupId groupId = origGroup.getGroupId();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		LOG.warn("ncId is NULL");
		LOG.info("LACP: lacpAddPort Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		LOG.warn("LACP: lacpAddPort: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);
	Group updGroup = populatedelGroup(isUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup);
	updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	return updGroup;

    }

    public Group populatedelGroup(boolean isUnicastGrp, NodeRef nodeRef, 
				  NodeId nodeId, NodeConnectorId ncId, 
				  GroupId groupId, Group origGroup) {

	NodeKey nodeKey = new NodeKey(nodeId);


	GroupBuilder groupBuilder = new GroupBuilder();



         if (isUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
         }

	GroupKey groupkey = new GroupKey(groupId);
	InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);

	groupBuilder.setGroupId(groupId);
	groupBuilder.setGroupName("LACP" + groupId);
	Buckets origbuckets = origGroup.getBuckets();
	NodeConnectorId origncId;
	BucketsBuilder bucketBuilder = new BucketsBuilder();
	List<Bucket> bucketList = Lists.newArrayList();
	BucketBuilder bucket = new BucketBuilder();
	bucket.setBucketId(new BucketId((long) 1));
	bucket.setKey(new BucketKey(new BucketId((long) 1)));

        /* put output action to the bucket */
	List<Action> bucketActionList = Lists.newArrayList();
	/* set order for new action and add to action list */
	for (Bucket origbucket : origbuckets.getBucket()) {
		List<Action> origbucketActions = origbucket.getAction();
		for (Action action : origbucketActions) {
			if (action.getAction() instanceof OutputActionCase) {
				OutputActionCase opAction = (OutputActionCase)action.getAction();
				origncId = (NodeConnectorId) opAction.getOutputAction().getOutputNodeConnector();
				if (opAction.getOutputAction().getOutputNodeConnector().equals(ncId)) {
					LOG.info("Port is removed");
				} else {
         				OutputActionBuilder oab = new OutputActionBuilder();
        				oab.setOutputNodeConnector(origncId);
         				ActionBuilder ab = new ActionBuilder();
         				ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        				ab.setOrder(bucketActionList.size());
        				ab.setKey(new ActionKey(bucketActionList.size()));
        				bucketActionList.add(ab.build());
			  }
			}
		   }
	    }
        bucket.setAction(bucketActionList);
        bucketList.add(bucket.build());
        bucketBuilder.setBucket(bucketList);
        groupBuilder.setBuckets(bucketBuilder.build());
	return(groupBuilder.build());
    }


    public boolean updateGroup(InstanceIdentifier<Group> identifier,
                       Group origGroup, Group updGroup,
                       InstanceIdentifier<Node> nodeInstId) {

	boolean isGroupUpdated = false;
        UpdateGroupInputBuilder groupBuilder = new UpdateGroupInputBuilder();

        groupBuilder.setNode(new NodeRef(nodeInstId.firstIdentifierOf(Node.class)));
        groupBuilder.setGroupRef(new GroupRef(identifier));
        groupBuilder.setTransactionUri(new Uri(getNewTransactionId()));
        groupBuilder.setUpdatedGroup((new UpdatedGroupBuilder(updGroup)).build());
	if (origGroup == null){
		LOG.warn("updateGroup: orig group is null not setting it");
	}
	else
	{
		LOG.debug("updateGroup: orig group is not null setting it");
        	groupBuilder.setOriginalGroup((new OriginalGroupBuilder(origGroup)).build());
	}
	
	
        try {
             	Future<RpcResult<UpdateGroupOutput>>  result = salGroupService.updateGroup(groupBuilder.build());
               	if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               	{
                  	LOG.info ("LACP: Group Updation Succeeded.");
                   	isGroupUpdated = true;
               	}
               	else {
                   	LOG.error("LACP: Group Updation Failed.");
                    	isGroupUpdated = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	LOG.error("received interrupt " + e.toString());
         }

	LOG.debug("updateGroup:returning "+isGroupUpdated);
        return(isGroupUpdated);

    }

    public void lacpRemGroup(Boolean isUnicastGrp, NodeConnectorRef nodeConnectorRef, GroupId  groupId)
    {
        if (nodeConnectorRef == null)
            return;
        LOG.info("LACP: lacpRemGroup ", nodeConnectorRef);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		LOG.warn("LACP: lacpRemGroup Node Connector ID is NULL");
		return;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		LOG.warn("LACP: lacpRemGroup: nodeId is NULL");
		return;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	remGroup( true, nodeRef,nodeId , ncId, groupId);
    }


    private boolean remGroup(boolean isUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId  groupId ) {


        boolean isGroupRemoved = true;

         /* Create output action for this ncId*/
         OutputActionBuilder oab = new OutputActionBuilder();
         oab.setOutputNodeConnector(ncId);
         ActionBuilder ab = new ActionBuilder();
         ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
         LOG.debug("lacpRemGroup:", ab.build());

         RemoveGroupInputBuilder groupBuilder = new RemoveGroupInputBuilder();

         if (isUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
         }

        GroupKey groupkey = new GroupKey(groupId);
        groupBuilder.setGroupId(groupId);
        groupBuilder.setGroupName("LACP"+groupId);
   	groupBuilder.setNode(nodeRef);
        groupBuilder.setBarrier(false);
        groupBuilder.setTransactionUri(new Uri (getNewTransactionId()) );

        BucketsBuilder bucketBuilder = new BucketsBuilder();
        List<Bucket> bucketList = Lists.newArrayList();
        BucketBuilder bucket = new BucketBuilder();
        bucket.setBucketId(new BucketId((long) 1));
        bucket.setKey(new BucketKey(new BucketId((long) 1)));

        /* put output action to the bucket */
        List<Action> bucketActionList = Lists.newArrayList();
        /* set order for new action and add to action list */
        ab.setOrder(bucketActionList.size());
        ab.setKey(new ActionKey(bucketActionList.size()));
        bucketActionList.add(ab.build());

        bucket.setAction(bucketActionList);
        bucketList.add(bucket.build());
        bucketBuilder.setBucket(bucketList);
        groupBuilder.setBuckets(bucketBuilder.build());


        try {
              	Future<RpcResult<RemoveGroupOutput>>  result = salGroupService.removeGroup(groupBuilder.build());
               	if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               	{
                 	LOG.info ("LACP: Group Deletion Succeeded.");
                   	isGroupRemoved = true;
               	}
               	else {
                   	LOG.warn("LACP: Group Deletion Failed.");
                    	isGroupRemoved = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	LOG.error("received interrupt " + e.toString());
         }

         return(isGroupRemoved);

    }


}
