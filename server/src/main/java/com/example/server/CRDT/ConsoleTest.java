package com.example.server.CRDT;

import com.example.server.model.CursorPosition;

import java.util.Collection;

public class ConsoleTest {
    public static void main(String[] args) {
        System.out.println("=== CRDT Document Test ===");

        // Create a new document
        String docId = "doc1";
        String userId = "user1";
        CRDTDocument document = new CRDTDocument(docId, userId);

        // Test 1: Basic insertions
        System.out.println("\nTest 1: Inserting characters...");
        document.insert('H', 0);
        document.insert('e', 1);
        document.insert('l', 2);
        document.insert('l', 3);
        document.insert('o', 4);
        System.out.println("Document content: " + document.getText());

        // Test 2: Deletion
        System.out.println("\nTest 2: Deleting 'e'...");
        document.delete(1);
        System.out.println("Document content: " + document.getText());

        // Test 3: Undo/Redo
        System.out.println("\nTest 3: Undo/Redo operations...");
        System.out.println("Before undo: " + document.getText());
        document.undo();
        System.out.println("After undo: " + document.getText());
        document.redo();
        System.out.println("After redo: " + document.getText());

        // Test 4: Cursor tracking
        System.out.println("\nTest 4: Cursor tracking...");
        document.updateCursor(userId, 2, "red");
        System.out.println("Current cursors:");
        Collection<CursorPosition> cursors = document.getAllCursors();
        for (CursorPosition cursor : cursors) {
            System.out.println("- User " + cursor.getUserId() +
                    " at position " + cursor.getPosition() +
                    " (color: " + cursor.getColor() + ")");
        }

        // Test 5: Adding a comment
        System.out.println("\nTest 5: Adding a comment...");
        document.addComment(0, 3, "Greeting");
        System.out.println("Document with comment: " + document.getText());

        // Test 6: Adding another user
        System.out.println("\nTest 6: Adding another user...");
        String user2 = "user2";
        document.addEditor(user2);
        document.userConnected(user2);
        document.updateCursor(user2, 4, "blue");
        System.out.println("Active users: " + document.getActiveUsers());
        System.out.println("Current cursors:");
        for (CursorPosition cursor : document.getAllCursors()) {
            System.out.println("- User " + cursor.getUserId() +
                    " at position " + cursor.getPosition());
        }

        System.out.println("\n=== Test Complete ===");
    }
}