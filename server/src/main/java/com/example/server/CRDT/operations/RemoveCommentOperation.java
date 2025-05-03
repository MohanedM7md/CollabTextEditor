package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

import java.util.List;

public class RemoveCommentOperation implements Operation {
    private final List<CharItem> items;
    private final String userId;
    public RemoveCommentOperation(List<CharItem> items, String userId) {
        this.items = items;
        this.userId = userId;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyComment(items, null); // null removes the comment
    }

    @Override
    public Operation getInverse() {
        // To undo a remove, we need to know the original comment
        // This would require storing the original comment in the operation
        // For now, we'll return a dummy operation
        return new CommentOperation(items, "unknown-comment", userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
