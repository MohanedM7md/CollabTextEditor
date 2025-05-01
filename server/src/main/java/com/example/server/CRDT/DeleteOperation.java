package com.example.server.CRDT;

public class DeleteOperation implements Operation {
    private final CharItem item;

    public DeleteOperation(CharItem item) {
        this.item = item;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyDelete(item);
    }

    @Override
    public Operation getInverse() {
        return new UndeleteOperation(item);
    }
}
