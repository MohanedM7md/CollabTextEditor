package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

public class UndeleteOperation implements Operation {
    private final CharItem item;
    private final String userId;
    public UndeleteOperation(CharItem item, String userId) {
        this.item = item;
        this.userId = userId;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyUndelete(item);
    }

    @Override
    public Operation getInverse() {
        return new DeleteOperation(item, userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
