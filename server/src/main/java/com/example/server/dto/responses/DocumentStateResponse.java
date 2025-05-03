
package com.example.server.dto.responses;

import com.example.server.model.CursorPosition;
import lombok.Getter;
import lombok.Setter;
import java.util.Collection;

@Getter @Setter
public class DocumentStateResponse {
    private String text;               // Current full text
    private Collection<CursorPosition> cursors; // All cursor positions
    private String operationType;      // "INSERT/DELETE/UNDO/REDO"
    private String triggeringUser;     // Who caused this update
    public DocumentStateResponse(String text, Collection<CursorPosition> cursors, String operationType, String triggeringUser) {
        this.text = text;
        this.cursors = cursors;
        this.operationType = operationType;
        this.triggeringUser = triggeringUser;
    }
}