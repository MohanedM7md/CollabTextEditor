package com.example.server.CRDT.operations;

import com.example.server.CRDT.CRDTDocument;
import com.example.server.CRDT.CharItem;
import com.example.server.model.CommentPosition;

import java.util.List;
import java.util.Objects;

public class AddCommentOperation implements Operation {
    private final CommentPosition  comment;   // the comment meta info
    private final String           userId;    // author / executor

    public AddCommentOperation(
                               CommentPosition comment,
                               String userId) {

        this.comment = Objects.requireNonNull(comment);
        this.userId  = Objects.requireNonNull(userId);
    }

    /* ---------------------------------------------------------------- */
    /* Operation interface                                              */
    /* ---------------------------------------------------------------- */

    @Override                                    // apply real change
    public void apply(CRDTDocument doc) {
        doc.applyAddComment(comment);
    }

    @Override                                    // for undo
    public Operation getInverse() {
        return new RemoveCommentOperation(userId,comment);
    }

    @Override                                    // required by history
    public String getUserId() {
        return userId;
    }
}