package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

public class DeleteOperation implements Operation {
    private final CharItem item;
    private final String userId;
    public DeleteOperation(CharItem item, String userId) {
        this.item = item;
        this.userId = userId;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyDelete(item);
    }

    @Override
    public Operation getInverse() {
        return new UndeleteOperation(item, userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
