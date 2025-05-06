package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;
import com.example.server.model.CommentPosition;

import java.util.List;

public class RemoveCommentOperation implements Operation {
    private final String userId;
    private final CommentPosition comment;   // the comment meta info
    public RemoveCommentOperation(String userId, CommentPosition comment) {
        this.userId = userId;
        this.comment = comment;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyRemoveComment(comment); // null removes the comment
    }

    @Override
    public Operation getInverse() {
        return new AddCommentOperation(comment,userId);
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
