package com.editor.collabtexteditor.dto.response;

import com.editor.collabtexteditor.model.CommentPosition;
import com.editor.collabtexteditor.model.CursorPosition;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter @Setter
public class DocumentStateResponse {
    private String text;
    private Collection<CursorPosition> cursors;
    private String operationType;
    private String triggeringUser;
    private Collection<CommentPosition> comments;

    public DocumentStateResponse() {

    }
}