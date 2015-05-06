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
    private static final Logger log = LoggerFactory.getLogger(LacpGroupTbl.class);
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

    //public void lacpAddGroup(Boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref,
    public Group lacpAddGroup(Boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref,
			     GroupId groupId)
    {
        if (nodeConnectorref == null){
            return null;
	}
        log.info("LACP: lacpAddGroup ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		log.warn("LACP: lacpAddGroup Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		log.warn("LACP: lacpAddGroup: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	Group grp = addGroup( true, nodeRef, nodeId , ncId, groupId);
	return grp;
    }


    //private boolean addGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
    private Group addGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId groupId) {

        boolean isGroupAdded = true;

	NodeKey nodeKey = new NodeKey(nodeId);

         /* Create output action for this ncId*/
         OutputActionBuilder oab = new OutputActionBuilder();
         oab.setOutputNodeConnector(ncId);
         ActionBuilder ab = new ActionBuilder();
         ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
         log.debug("lacpAddGroup: addGroup", ab.build());

         AddGroupInputBuilder groupBuilder = new AddGroupInputBuilder();

         if (IsUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
         }

        GroupKey groupkey = new GroupKey(groupId);
        InstanceIdentifier<Group> lacpGId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey).toInstance();

        log.info("addGroup: lacpGid " , lacpGId);

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


        try {
             	Future<RpcResult<AddGroupOutput>>  result = salGroupService.addGroup(groupBuilder.build());
               	if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               	{
                  	log.info ("LACP: Group Additon Succeeded.");
                   	log.info("LACP: Group Additon Succeeded.");
                   	isGroupAdded = true;
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
                   	log.error("LACP: Group Additon Failed.");
                    	isGroupAdded = false;
			return null;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.error("received interrupt " + e.getMessage());
         }

        return null;

    }



    public Group lacpAddPort(boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref, 
			   Group  origGroup) {

        if (nodeConnectorref == null)
            return null;
	if (origGroup == null)
        {
		log.warn("lacpAddPort: origGroup is NULL"); 
                return null;
        }
        log.info("LACP: lacpAddPort ", nodeConnectorref);
	GroupId groupId = origGroup.getGroupId();
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	log.info("lacpAddPort for group id " , groupId);
	if (ncId == null)
	{
		log.warn("LACP: lacpAddPort Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		log.warn("LACP: lacpAddPort: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier<Group> lacpGId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey).toInstance();
	log.info("lacpAddPort: lacpGid ", lacpGId);
	log.info("lacpAddPort:  key " , groupkey);

	Group updGroup = populateGroup(IsUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup); 
	if (updGroup == null){
		log.warn("lacpAddPort: updGroup is NULL");
	}
	else{
   		log.info("lacpAddPort updGroup is available proceeding to program it");
		updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	}
	return updGroup;
   }


    public Group populateGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId,
			       NodeConnectorId ncId, GroupId groupId,
			       Group origGroup) {

	NodeKey nodeKey = new NodeKey(nodeId);


         GroupBuilder groupBuilder = new GroupBuilder();


         if (IsUnicastGrp == true)
         {
                 groupBuilder.setGroupType(GroupTypes.GroupSelect);
		 log.debug("populate group: type select");
         } else {
                 groupBuilder.setGroupType(GroupTypes.GroupAll);
		 log.debug("populate group: type all");
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
					log.warn("returning null here at 1");
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
		log.error(e.getMessage(), e);
	}
	log.warn("readgrp returning null");
	return null;
		
   }

   public Group lacpRemPort(Group origGroup, NodeConnectorRef nodeConnectorref, boolean IsUnicastGrp) {
        if (nodeConnectorref == null){
            return null;
	}
	if (origGroup == null)
        {
		log.warn("lacpAddPort: origGroup is NULL");
		return null;
	}
        log.info("LACP: lacpAddPort ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();
        GroupId groupId = origGroup.getGroupId();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		log.warn("ncId is NULL");
		log.info("LACP: lacpAddPort Node Connector ID is NULL");
		return null;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		log.warn("LACP: lacpAddPort: nodeId is NULL");
		return null;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);
	Group updGroup = populatedelGroup(IsUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup);
	updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	return updGroup;

    }

    public Group populatedelGroup(boolean IsUnicastGrp, NodeRef nodeRef, 
				  NodeId nodeId, NodeConnectorId ncId, 
				  GroupId groupId, Group origGroup) {

	NodeKey nodeKey = new NodeKey(nodeId);


	GroupBuilder groupBuilder = new GroupBuilder();



         if (IsUnicastGrp == true)
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
					log.info("Port is removed");
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
		log.warn("updateGroup: orig group is null not setting it");
	}
	else
	{
		log.debug("updateGroup: orig group is not null setting it");
        	groupBuilder.setOriginalGroup((new OriginalGroupBuilder(origGroup)).build());
	}
	
	
        try {
             	Future<RpcResult<UpdateGroupOutput>>  result = salGroupService.updateGroup(groupBuilder.build());
               	if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               	{
                  	log.info ("LACP: Group Updation Succeeded.");
                   	isGroupUpdated = true;
               	}
               	else {
                   	log.error("LACP: Group Updation Failed.");
                    	isGroupUpdated = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.error("received interrupt " + e.getMessage());
         }

	log.debug("updateGroup:returning "+isGroupUpdated);
        return(isGroupUpdated);

    }

    public void lacpRemGroup(Boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref, GroupId  groupId)
    {
        if (nodeConnectorref == null)
            return;
        log.info("LACP: lacpRemGroup ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		log.warn("LACP: lacpRemGroup Node Connector ID is NULL");
		return;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	if (nodeId == null)
	{
		log.warn("LACP: lacpRemGroup: nodeId is NULL");
		return;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	remGroup( true, nodeRef,nodeId , ncId, groupId);
    }


    private boolean remGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId  groupId ) {


        boolean isGroupRemoved = true;

         /* Create output action for this ncId*/
         OutputActionBuilder oab = new OutputActionBuilder();
         oab.setOutputNodeConnector(ncId);
         ActionBuilder ab = new ActionBuilder();
         ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
         log.debug("lacpRemGroup:", ab.build());

         RemoveGroupInputBuilder groupBuilder = new RemoveGroupInputBuilder();

         if (IsUnicastGrp == true)
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
                 	log.info ("LACP: Group Deletion Succeeded.");
                   	isGroupRemoved = true;
               	}
               	else {
                   	log.warn("LACP: Group Deletion Failed.");
                    	isGroupRemoved = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.error("received interrupt " + e.getMessage());
         }

         return(isGroupRemoved);

    }


}
