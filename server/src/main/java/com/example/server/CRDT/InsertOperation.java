package com.example.server.CRDT;

public class InsertOperation implements Operation {
    private final CharItem item;

    public InsertOperation(CharItem item) {
        this.item = item;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyInsert(item);
    }

    @Override
    public Operation getInverse() {
        return new DeleteOperation(item);
    }
}
