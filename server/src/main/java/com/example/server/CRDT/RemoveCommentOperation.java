package com.example.server.CRDT;

import java.util.List;

public class RemoveCommentOperation implements Operation {
    private final List<CharItem> items;

    public RemoveCommentOperation(List<CharItem> items) {
        this.items = items;
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
        return new CommentOperation(items, "unknown-comment");
    }
}
