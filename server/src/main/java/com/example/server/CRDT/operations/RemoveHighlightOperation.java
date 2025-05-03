package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

import java.util.List;

public class RemoveHighlightOperation implements Operation {
    private final List<CharItem> items;
    private final String userId;
    public RemoveHighlightOperation(List<CharItem> items, String userId) {
        this.items = items;
        this.userId = userId;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyHighlight(items, null); // null removes the highlight
    }

    @Override
    public Operation getInverse() {
        // To undo a remove, we need to know the original color
        // This would require storing the original color in the operation
        // For now, we'll return a dummy operation
        return new HighlightOperation(items, "unknown-color", userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
