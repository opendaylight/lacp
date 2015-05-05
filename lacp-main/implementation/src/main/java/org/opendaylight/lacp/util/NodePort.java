package org.opendaylight.lacp.Utils;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import com.google.common.base.Optional;
import org.opendaylight.lacp.inventory.LacpSystem;
import org.opendaylight.lacp.inventory.LacpNodeExtn;
import org.opendaylight.lacp.inventory.LacpPort;
import org.opendaylight.lacp.util.LacpUtil;

public class NodePort
{
    public static short getPortId(NodeConnectorRef portRef)
    {
        short result = 0;
        if (portRef != null)
        {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = (InstanceIdentifier<NodeConnector>)portRef.getValue();
            InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>) nodeConnectorInstanceId.firstIdentifierOf(Node.class);
            long node = LacpUtil.getNodeSwitchId(nodeId);
            LacpSystem lacpSystem = LacpSystem.getLacpSystem();
            LacpNodeExtn lacpNode = lacpSystem.getLacpNode(node);
            /* If the port is already available in the lacpPortList in the lacpNode,
             *  return the port id, else obtain the portId from the flowCapableNC 
             *  augmentation information */
            if (lacpNode != null)
            {
                LacpPort port = lacpNode.getLacpPort(nodeConnectorInstanceId);
                if (port != null)
                {
                    return port.slaveGetPortId();
                }
            }
            NodeConnector nc = readNodeConnector (portRef);
            if (nc != null)
            {
                FlowCapableNodeConnector flowNC = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
                result = Short.valueOf((flowNC.getPortNumber().getUint32()).toString());
            }
        }
        return result;
    }
    public static long getSwitchId(NodeConnectorRef portRef)
    {
        long result = 0;
        if (portRef != null)
        {
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = (InstanceIdentifier<NodeConnector>)portRef.getValue();
            InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>) nodeConnectorInstanceId.firstIdentifierOf(Node.class);
            result = LacpUtil.getNodeSwitchId(nodeId);
        }
        return result;
    }
    private static NodeConnector readNodeConnector (NodeConnectorRef ncRef)
    {
        NodeConnector nc = null;
        ReadOnlyTransaction readTx = LacpUtil.getDataBrokerService().newReadOnlyTransaction();

        try
        {
            Optional<NodeConnector> dataObject = readTx.read(LogicalDatastoreType.OPERATIONAL,
                                                             (InstanceIdentifier<NodeConnector>) ncRef.getValue()).get();
            if (dataObject.isPresent())
            {
                nc = (NodeConnector) dataObject.get();
            }
        }
        catch(Exception e)
        {
            readTx.close();
        }
        readTx.close();
        return nc;
    }
}
