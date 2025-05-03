package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;

import java.util.List;

public class CommentOperation implements Operation {
    private final List<CharItem> items;
    private final String comment;
    private final String userId;
    public CommentOperation(List<CharItem> items, String comment, String userId) {
        this.items = items;
        this.comment = comment;
        this.userId = userId;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyComment(items, comment);
    }

    @Override
    public Operation getInverse() {
        return new RemoveCommentOperation(items,userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
