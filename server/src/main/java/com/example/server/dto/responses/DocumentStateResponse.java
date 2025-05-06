
package com.example.server.dto.responses;

import com.example.server.model.CommentPosition;
import com.example.server.model.CursorPosition;
import lombok.Getter;
import lombok.Setter;
import java.util.Collection;

@Getter @Setter
public class DocumentStateResponse {
    private String text;
    private Collection<CursorPosition> cursors;
    private Collection<CommentPosition> comments;
    private String operationType;
    private String triggeringUser;

    public DocumentStateResponse(String text,
                                 Collection<CursorPosition> cursors,
                                 Collection<CommentPosition> comments,
                                 String operationType,
                                 String triggeringUser) {
        this.text = text;
        this.cursors = cursors;
        this.comments = comments;
        this.operationType = operationType;
        this.triggeringUser = triggeringUser;
    }
}
