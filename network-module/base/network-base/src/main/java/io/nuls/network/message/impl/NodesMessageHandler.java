package io.nuls.network.message.impl;

import io.nuls.core.tools.network.IpUtil;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.manager.NodeManager;
import io.nuls.network.protocol.handler.BaseNetworkMeesageHandler;
import io.nuls.network.protocol.message.NodeMessageBody;
import io.nuls.network.protocol.message.NodesMessage;
import io.nuls.protocol.message.base.BaseMessage;

import java.util.Map;
import java.util.Set;

public class NodesMessageHandler implements BaseNetworkMeesageHandler {

    private static NodesMessageHandler instance = new NodesMessageHandler();

    private NodesMessageHandler() {

    }

    public static NodesMessageHandler getInstance() {
        return instance;
    }

    private NodeManager nodeManager = NodeManager.getInstance();

    private Set<String> localIps = IpUtil.getIps();

    @Override
    public NetworkEventResult process(BaseMessage message, Node node) {
        NodesMessage nodesMessage = (NodesMessage) message;
        NodeMessageBody body = nodesMessage.getMsgBody();

        boolean exist = false;
        Map<String, Node> outNodes = nodeManager.getNodes();
        for (Node newNode : body.getNodeList()) {
            if (localIps.contains(newNode.getIp())) {
                continue;
            }
            exist = false;
            for (Node outNode : outNodes.values()) {
                if (outNode.getIp().equals(newNode.getIp())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                newNode.setType(Node.OUT);
                newNode.setStatus(Node.CLOSE);
                newNode.setId(null);
                nodeManager.addNode(newNode);
            }
        }
        return null;
    }
}
