package com.example.server.CRDT;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CRDTConcurrentTest {
    private static final String DOC_ID = "testDoc";
    private static final int NUM_USERS = 3;
    private static final int OPERATIONS_PER_USER = 5;
    private static final String[] COLORS = {"#FF0000", "#00FF00", "#0000FF"};

    public static void main(String[] args) throws InterruptedException {
        // Create initial document owned by user1
        CRDTDocument document = new CRDTDocument(DOC_ID, "user1");

        // Add other users as editors
        for (int i = 2; i <= NUM_USERS; i++) {
            document.addEditor("user" + i);
            document.userConnected("user" + i);
        }

        System.out.println("=== Starting Concurrent CRDT Test ===");
        System.out.println("Initial active users: " + document.getActiveUsers());

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NUM_USERS);

        // Submit tasks for each user
        for (int i = 1; i <= NUM_USERS; i++) {
            final String userId = "user" + i;
            executor.submit(() -> userSimulation(document, userId));
        }

        // Shutdown and wait
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Final state
        System.out.println("\n=== Final Document State ===");
        System.out.println("Content: " + document.getText());
        System.out.println("Active users: " + document.getActiveUsers());
        System.out.println("All cursors: " + document.getAllCursors());
    }

    private static void userSimulation(CRDTDocument document, String userId) {
        Random random = new Random();
        String color = COLORS[Integer.parseInt(userId.substring(4)) % COLORS.length];

        try {
            for (int i = 0; i < OPERATIONS_PER_USER; i++) {
                // Random operation type
                int opType = random.nextInt(4);
                int position = random.nextInt(Math.max(1, document.getText().length()));

                switch (opType) {
                    case 0 -> { // Insert
                        char c = (char) ('a' + random.nextInt(26));
                        document.insert(c, position, userId);
                        System.out.printf("[%s] INSERT '%c' at %d%n", userId, c, position);
                    }
                    case 1 -> { // Delete
                        if (document.getText().length() > 0) {
                            document.delete(position, userId);
                            System.out.printf("[%s] DELETE at %d%n", userId, position);
                        }
                    }
                    case 2 -> { // Undo
                        document.undo(userId);
                        System.out.printf("[%s] UNDO%n", userId);
                    }
                    case 3 -> { // Redo
                        document.redo(userId);
                        System.out.printf("[%s] REDO%n", userId);
                    }
                }

                // Update cursor
                int cursorPos = random.nextInt(Math.max(1, document.getText().length() + 1));
                document.updateCursor(userId, cursorPos, color);

                // Random delay between operations
                Thread.sleep(100 + random.nextInt(400));
            }
        } catch (Exception e) {
            System.err.printf("[%s] Error: %s%n", userId, e.getMessage());
        }
    }
}