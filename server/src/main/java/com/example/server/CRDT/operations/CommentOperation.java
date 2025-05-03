package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

import java.util.List;

public class CommentOperation implements Operation {
    private final List<CharItem> items;
    private final String comment;

    public CommentOperation(List<CharItem> items, String comment) {
        this.items = items;
        this.comment = comment;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyComment(items, comment);
    }

    @Override
    public Operation getInverse() {
        return new RemoveCommentOperation(items);
    }
}
