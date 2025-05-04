package com.example.server.CRDT;

import com.example.server.model.Document;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CRDTStressTest {
    private static final String DOC_ID = "stressTestDoc";
    private static final int NUM_USERS = 5;  // Increased concurrency
    private static final int OPERATIONS_PER_USER = 20;  // More operations
    private static final String[] COLORS = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF"};
    private static final Lock printLock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        // Create single user document
        Document docWrapper = new Document("user1","tr6","tg65","hgg");
        CRDTDocument document = docWrapper.getCrdtDocument();

        String userId = "user1";

        System.out.println("=== Single-threaded CRDT Test ===");

        // Insert 'a' at position 0
        document.insert('a', 0, userId);
        printDocumentState(document, "After inserting 'a' at 0");

        TimeUnit.SECONDS.sleep(2);
        // Insert 'b' at position 0 (should push 'a' to the right)
        document.insert('b', 0, userId);
        printDocumentState(document, "After inserting 'b' at 0");

        document.insert('p', 0, userId);
        printDocumentState(document, "After inserting 'p' at 0");

        // Insert 'c' at position 1 (between 'b' and 'a')
        document.insert('c', 1, userId);
        printDocumentState(document, "After inserting 'c' at 1");
        document.insert('p', 0, userId);
        printDocumentState(document, "After inserting 'p' at 0");

        // Delete the middle character (should be 'c')
        document.delete(1, userId);
        printDocumentState(document, "After deleting at position 1");

        // Delete the middle character (should be 'c')
        document.delete(0, userId);
        printDocumentState(document, "After deleting at position 0");

        // Export to file
        try {
            File outFile = new File("crdt_single_thread_test.txt");
            docWrapper.exportToTextFile(outFile);
            System.out.println("Exported to: " + outFile.getAbsolutePath()+"  "+ docWrapper.getId());
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }

        try {
            Document importedDoc = new Document("importTester","tgtgt","y56trg","565");
            importedDoc.importFromTextFile(new File("crdt_output.txt"), "importTester");
            System.out.println("\n=== IMPORTED DOCUMENT ===");
            System.out.println("Imported doc ID"+ importedDoc.getId());
            printDocumentState(importedDoc.getCrdtDocument(), "IMPORTED STATE");
        } catch (IOException e) {
            System.err.println("Import failed: " + e.getMessage());
        }
    }


    private static void userStressTest(CRDTDocument document, String userId) {
        Random random = new Random();
        String color = COLORS[Integer.parseInt(userId.substring(4)) - 1];

        try {
            for (int i = 0; i < OPERATIONS_PER_USER; i++) {
                // Random operation with realistic distribution
                int opType = getWeightedRandomOperationType(random, document);
                int position = document.getText().isEmpty() ? 0 :
                        random.nextInt(document.getText().length());

                performOperation(document, userId, opType, position, color);

                // Random delay between operations (shorter for more concurrency)
                Thread.sleep(50 + random.nextInt(150));
            }
        } catch (Exception e) {
            printLock.lock();
            try {
                System.err.printf("[%s] ERROR: %s%n", userId, e.getMessage());
                e.printStackTrace();
            } finally {
                printLock.unlock();
            }
        }
    }

    private static int getWeightedRandomOperationType(Random random, CRDTDocument document) {
        // Weighted probabilities (insert 40%, delete 30%, undo 15%, redo 10%, cursor 5%)
        double r = random.nextDouble();
        if (r < 0.40) return 0; // insert
        if (r < 0.70) return 1; // delete
        if (r < 0.85) return 2; // undo
        if (r < 0.95) return 3; // redo
        return 4; // cursor
    }

    private static void performOperation(CRDTDocument document, String userId,
                                         int opType, int position, String color) {
        printLock.lock();
        try {
            String operationDesc = "";

            switch (opType) {
                case 0 -> { // Insert
                    char c = (char) ('a' + new Random().nextInt(26));
                    document.insert(c, position, userId);
                    operationDesc = String.format("INSERT '%c' at %d", c, position);
                }
                case 1 -> { // Delete
                    if (!document.getText().isEmpty()) {
                        document.delete(position, userId);
                        operationDesc = String.format("DELETE at %d", position);
                    }
                }
                case 2 -> { // Undo
                    document.undo(userId);
                    operationDesc = "UNDO";
                }
                case 3 -> { // Redo
                    document.redo(userId);
                    operationDesc = "REDO";
                }
                case 4 -> { // Cursor update
                    int cursorPos = document.getText().isEmpty() ? 0 :
                            new Random().nextInt(document.getText().length());
                    document.updateCursor(userId, cursorPos, color);
                    operationDesc = String.format("CURSOR to %d", cursorPos);
                }
            }

            if (!operationDesc.isEmpty()) {
                System.out.printf("\n[%s] %s%n", userId, operationDesc);
                printDocumentState(document, "CURRENT STATE");
            }
        } finally {
            printLock.unlock();
        }
    }

    private static void printDocumentState(CRDTDocument document, String title) {
        System.out.println("=== " + title + " ===");
        System.out.println("TEXT: \"" + document.getText() + "\"");
        System.out.println("LENGTH: " + document.getText().length());


    }
}