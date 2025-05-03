package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

public class UndeleteOperation implements Operation {
    private final CharItem item;

    public UndeleteOperation(CharItem item) {
        this.item = item;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyUndelete(item);
    }

    @Override
    public Operation getInverse() {
        return new DeleteOperation(item);
    }
}
