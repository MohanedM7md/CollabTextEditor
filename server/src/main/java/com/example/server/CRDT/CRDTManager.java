package com.example.server.CRDT;

import com.example.server.model.Operation;

import java.util.*;

public class CRDTManager {
    private CharNode root = new CharNode("root", "");
    private Map<String, CharNode> nodeMap = new HashMap<>();

    public CRDTManager() {
        nodeMap.put("root", root);
    }

    public void apply(Operation op) {
        if (op.getOp().equals("insert")) {
            handleInsert(op);
        } else if (op.getOp().equals("delete")) {
            handleDelete(op);
        }
    }

    private void handleInsert(Operation op) {
        String parentId = op.getParentId();
        CharNode parent = nodeMap.getOrDefault(parentId, root);

        CharNode newNode = new CharNode(op.getId(), op.getValue());
        parent.addChild(newNode);
        nodeMap.put(newNode.getId(), newNode);
    }

    private void handleDelete(Operation op) {
        CharNode node = nodeMap.get(op.getId());
        if (node != null) {
            node.setDeleted(true);
        }
    }

    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        dfs(root, sb);
        return sb.toString();
    }

    private void dfs(CharNode node, StringBuilder sb) {
        for (CharNode child : node.getChildren()) {
            if (!child.isDeleted()) {
                sb.append(child.getValue());
            }
            dfs(child, sb);
        }
    }

    public List<Operation> getAllOperations() {
        List<Operation> ops = new ArrayList<>();
        collectOps(root, ops);
        return ops;
    }

    private void collectOps(CharNode node, List<Operation> ops) {
        for (CharNode child : node.getChildren()) {
            Operation op = new Operation(
                    child.isDeleted() ? "delete" : "insert",
                    child.getValue(),
                    "unknown",  // Parent info isn't stored after insert
                    child.getId(),
                    child.getId().split(":")[0],
                    child.getId().split(":")[1]
            );
            ops.add(op);
            collectOps(child, ops);
        }
    }
}
