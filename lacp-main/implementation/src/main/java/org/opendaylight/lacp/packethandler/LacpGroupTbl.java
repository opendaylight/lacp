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
import org.opendaylight.yangtools.yang.binding.DataObject;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.Groups;
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
import java.util.ArrayList;
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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import java.lang.*;
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

    public void lacpAddGroup(Boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref,
			     GroupId groupId)
    {
        if (nodeConnectorref == null)
            return;
        log.info("LACP: lacpAddGroup ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		System.out.println("ncId is NULL");
		log.info("LACP: lacpAddGroup Node Connector ID is NULL");
		return;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	System.out.println("onNodeConnectorUpdated:after nodeId "+ nodeId);
	if (nodeId == null)
	{
		System.out.println("nodeId is NULL");
		log.info("LACP: lacpAddGroup: nodeId is NULL");
		return;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	addGroup( true, nodeRef, nodeId , ncId, groupId);
	System.out.println("Exiting onNodeConnectorRemoved");
    }


    private boolean addGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId groupId) {

        boolean isGroupAdded = true;
	System.out.println("lacpAddGroup: nodeId-   " + nodeId );
	System.out.println("lacpAddGroup:ncId-    " + ncId );
	System.out.println("lacpAddGroup:nodeRef-    " + nodeRef ); 

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
        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);

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
                   	System.out.println("LACP: Group Additon Succeeded.");
                   	isGroupAdded = true;
               	}
               	else {
                   	log.info ("LACP: Group Additon Failed.");
                    	System.out.println ("LACP: Group Additon Failed.");
                    	isGroupAdded = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.debug ("received interrupt " + e.getMessage());
         }

        return(isGroupAdded);

    }



    public int lacpAddPort(boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref, 
			   GroupId  groupId) {

        if (nodeConnectorref == null)
            return 0;
        log.info("LACP: lacpAddPort ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		System.out.println("ncId is NULL");
		log.info("LACP: lacpAddPort Node Connector ID is NULL");
		return 0;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	System.out.println("lacpAddPort:after nodeId "+ nodeId);
	if (nodeId == null)
	{
		System.out.println("nodeId is NULL");
		log.info("LACP: lacpAddPort: nodeId is NULL");
		return 0;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);
	Group origGroup = getGroup(groupkey, nodeKey);
	if (origGroup == null) {
		System.out.println("lacpAddPort: origGroup is NULL");
	}

	Group updGroup = populateGroup(IsUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup);
	updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	return 0;

   }


    public Group populateGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId,
			       NodeConnectorId ncId, GroupId groupId,
			       Group origGroup) {

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
        groupBuilder.setBarrier(false);

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
				if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncId))) {
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

       	InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);
	ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
	try {
		Optional<Group> readGroup = readTx.read(LogicalDatastoreType.CONFIGURATION, lacpGId).get();
		if (readGroup.isPresent()) {
			return readGroup.get();
		}
	}  catch (InterruptedException|ExecutionException e) {
		log.error(e.getMessage(), e);
	}
	return null;
		
   }

   public int lacpRemPort(GroupId groupId, NodeConnectorRef nodeConnectorref, boolean IsUnicastGrp) {
        if (nodeConnectorref == null)
            return 0;
        log.info("LACP: lacpAddPort ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();

	if (ncId == null)
	{
		System.out.println("ncId is NULL");
		log.info("LACP: lacpAddPort Node Connector ID is NULL");
		return 0;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	System.out.println("lacpAddPort:after nodeId "+ nodeId);
	if (nodeId == null)
	{
		System.out.println("nodeId is NULL");
		log.info("LACP: lacpAddPort: nodeId is NULL");
		return 0;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);
	NodeKey nodeKey = new NodeKey(nodeId);
        GroupKey groupkey = new GroupKey(groupId);

        InstanceIdentifier <Group> lacpGId = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey).augmentation(FlowCapableNode.class).child(Group.class, groupkey);
	Group origGroup = getGroup(groupkey, nodeKey);
	if (origGroup == null) {
		System.out.println("lacpAddPort: origGroup is NULL");
		return 0;
	}
	Group updGroup = populatedelGroup(IsUnicastGrp, nodeRef, nodeId, ncId, groupId, origGroup);
	updateGroup(lacpGId, origGroup, updGroup , nodeInstId);
	return 0;

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
					System.out.println("Port is removed");
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
        groupBuilder.setOriginalGroup((new OriginalGroupBuilder(origGroup)).build());
	
	
        try {
             	Future<RpcResult<UpdateGroupOutput>>  result = salGroupService.updateGroup(groupBuilder.build());
               	if (result.get (5, TimeUnit.SECONDS).isSuccessful () == true)
               	{
                  	log.info ("LACP: Group Updation Succeeded.");
                   	System.out.println("LACP: Group Updation Succeeded.");
                   	isGroupUpdated = true;
               	}
               	else {
                   	log.info ("LACP: Group Updation Failed.");
                    	System.out.println ("LACP: Group Updation Failed.");
                    	isGroupUpdated = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.debug ("received interrupt " + e.getMessage());
         }

        return(isGroupUpdated);

    }

    public void lacpRemGroup(Boolean IsUnicastGrp, NodeConnectorRef nodeConnectorref, GroupId  groupId)
    {
        if (nodeConnectorref == null)
            return;
        log.info("LACP: lacpRemGroup ", nodeConnectorref);
	InstanceIdentifier<NodeConnector> ncInstId = (InstanceIdentifier<NodeConnector>)nodeConnectorref.getValue();

        NodeConnectorId ncId = InstanceIdentifier.keyOf(ncInstId).getId();
	System.out.println("lacpRemGroup:after ncId "+ncId);

	if (ncId == null)
	{
		System.out.println("ncId is NULL");
		log.info("LACP: lacpRemGroup Node Connector ID is NULL");
		return;
	}
	
	InstanceIdentifier<Node> nodeInstId = ncInstId.firstIdentifierOf(Node.class);
	NodeId nodeId = InstanceIdentifier.keyOf(nodeInstId).getId();
	System.out.println("lacpRemGroup:after nodeId "+ nodeId);
	if (nodeId == null)
	{
		System.out.println("nodeId is NULL");
		log.info("LACP: lacpRemGroup: nodeId is NULL");
		return;
	}
	NodeRef nodeRef = new NodeRef(nodeInstId);

	remGroup( true, nodeRef,nodeId , ncId, groupId);
	System.out.println("Exiting lacpRemGroup");
    }


    private boolean remGroup(boolean IsUnicastGrp, NodeRef nodeRef, NodeId nodeId, 
			     NodeConnectorId ncId, GroupId  groupId ) {


        boolean isGroupRemoved = true;
	System.out.println("lacpRemGroup: nodeId-   " + nodeId );
	System.out.println("lacpRemGroup:ncId-    " + ncId );
	System.out.println("lacpRemGroup:nodeRef-    " + nodeRef ); 


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
                   	System.out.println("LACP: Group Deletion Succeeded.");
                   	isGroupRemoved = true;
               	}
               	else {
                   	log.info ("LACP: Group Deletion Failed.");
                    	System.out.println ("LACP: Group Deletion Failed.");
                    	isGroupRemoved = false;
               	}
         }
         catch (InterruptedException | ExecutionException | TimeoutException e)
         {
             	log.debug ("received interrupt " + e.getMessage());
         }

         return(isGroupRemoved);

    }


}
