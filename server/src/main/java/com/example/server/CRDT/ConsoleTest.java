package com.example.server.CRDT;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class ConsoleTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("CRDT Text Editor - Console Test");

        // Initialize document
        System.out.print("Enter your user ID: ");
        String userId = scanner.nextLine();
        CRDTDocument doc = new CRDTDocument(userId);

        // Main loop
        while (true) {
            System.out.println("\nCurrent document: " + doc.getText());
            System.out.println("1. Insert character");
            System.out.println("2. Delete character");
            System.out.println("3. Add comment");
            System.out.println("4. Remove comment");
            System.out.println("5. Highlight text");
            System.out.println("6. Remove highlight");
            System.out.println("7. Undo");
            System.out.println("8. Redo");
            System.out.println("9. Show document state");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            try {
                switch (choice) {
                    case 1: // Insert
                        System.out.print("Enter position to insert at: ");
                        int insertPos = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter character to insert: ");
                        char c = scanner.nextLine().charAt(0);
                        doc.insert(c, insertPos);
                        break;

                    case 2: // Delete
                        System.out.print("Enter position to delete: ");
                        int deletePos = Integer.parseInt(scanner.nextLine());
                        doc.delete(deletePos);
                        break;

                    case 3: // Add comment
                        System.out.print("Enter start position: ");
                        int commentStart = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter end position: ");
                        int commentEnd = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter comment text: ");
                        String comment = scanner.nextLine();

                        break;

                    case 4: // Remove comment
                        System.out.print("Enter start position: ");
                        int uncommentStart = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter end position: ");
                        int uncommentEnd = Integer.parseInt(scanner.nextLine());
                        doc.removeComment(uncommentStart, uncommentEnd);
                        break;

                    case 5: // Highlight
                        System.out.print("Enter start position: ");
                        int highlightStart = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter end position: ");
                        int highlightEnd = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter color (e.g., 'yellow'): ");
                        String color = scanner.nextLine();

                        break;

                    case 6: // Remove highlight
                        System.out.print("Enter start position: ");
                        int unhighlightStart = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter end position: ");
                        int unhighlightEnd = Integer.parseInt(scanner.nextLine());
                        doc.removeHighlight(unhighlightStart, unhighlightEnd);
                        break;

                    case 7: // Undo
                        doc.undo();
                        System.out.println("Undo performed");
                        break;

                    case 8: // Redo
                        doc.redo();
                        System.out.println("Redo performed");
                        break;

                    case 9: // Show document state
                        showDocumentState(doc);
                        break;

                    case 0: // Exit
                        System.out.println("Exiting...");
                        return;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void showDocumentState(CRDTDocument doc) {
        System.out.println("\n=== Document State ===");
        System.out.println("Text: " + doc.getText());

        List<CharItem> items = new ArrayList<>(doc.items.keySet());
        for (int i = 0; i < items.size(); i++) {
            CharItem item = items.get(i);
            System.out.printf("%3d: %s %s %s %s%n",
                    i,
                    item.isDeleted() ? "[DEL]" : "     ",
                    item.getValue(),
                    item.getComment() != null ? "Comment: " + item.getComment() : "",
                    item.getColor() != null ? "Color: " + item.getColor() : "");
        }
        System.out.println("=====================");
    }
}