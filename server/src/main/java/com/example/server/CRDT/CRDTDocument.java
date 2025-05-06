package com.example.server.CRDT;

import com.example.server.CRDT.operations.*;
import com.example.server.dto.responses.DocumentStateResponse;
import com.example.server.model.CommentPosition;
import com.example.server.model.CursorPosition;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CRDTDocument {
    private final TreeMap<CharItem, Void> items;
    private final Map<String, List<Operation>> userOperationHistory = new ConcurrentHashMap<>();
    private final Map<String, Integer> userHistoryPointers = new ConcurrentHashMap<>();
    private final String documentId;
    private final Map<String, CursorPosition> cursors;
    private final Set<String> activeUsers;
    private final Set<String> editors;
    private final Set<String> viewers;
    private final Map<String, CommentPosition> comments = new ConcurrentHashMap<>();
    private boolean enableHistory = true;

    public CRDTDocument(String documentId, String userId) {
        this.documentId = documentId;
        this.items = new TreeMap<>(new CharItemComparator());
        this.cursors = new ConcurrentHashMap<>();
        this.activeUsers = ConcurrentHashMap.newKeySet();
        this.editors = ConcurrentHashMap.newKeySet();
        this.viewers = ConcurrentHashMap.newKeySet();
        this.activeUsers.add(userId);
        this.editors.add(userId);
    }


    public synchronized void insert(char value, int clientPosition, String userId) throws IllegalStateException {
        if (!editors.contains(userId)) {
            throw new IllegalStateException("User doesn't have edit permissions");
        }

        // Convert client position to CRDT position
        int crdtPosition = clientToCrdtPosition(clientPosition);

        if (crdtPosition < 0 || crdtPosition > items.size()) {
            throw new IndexOutOfBoundsException("Invalid insert position");
        }
        System.out.println("Inserting '" + value + "' at client position " + clientPosition +
                " (CRDT position " + crdtPosition + ")");
        CharItem newItem = createItemAtPosition(value, crdtPosition, userId);
        applyOperation(new InsertOperation(newItem, userId));
    }

    public synchronized void delete(int clientPosition, String userId) throws IllegalStateException {
        if (!editors.contains(userId)) {
            throw new IllegalStateException("User doesn't have edit permissions");
        }
        // Convert client position to CRDT position
        int crdtPosition = clientToCrdtPosition(clientPosition);

        if (crdtPosition < 0 || crdtPosition >= items.size())
            return;

        CharItem item = getItemAtPosition(crdtPosition);
        if (!item.isDeleted()) {
            applyOperation(new DeleteOperation(item, userId));
        }
        int deletedCharGlobalPos = clientPosition;
        updateCommentsOnDelete(deletedCharGlobalPos);
    }

    public synchronized void addHighlight(int startPos, int endPos, String color, String userId) throws IllegalStateException {
        List<CharItem> affectedItems = getItemsInRange(startPos, endPos);
        applyOperation(new HighlightOperation(affectedItems, color,userId));
    }

    // Methods to safely access document state
    public CharItem getItemAtPosition(int position) {
        return new ArrayList<>(items.keySet()).get(position);
    }

    public List<CharItem> getItemsInRange(int startPos, int endPos) {
        List<CharItem> allItems = new ArrayList<>(items.keySet());
        return allItems.subList(startPos, Math.min(endPos, allItems.size()));
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (CharItem item : items.keySet()) {
            if (!item.isDeleted()) {
                sb.append(item.getValue());
            }
        }
        return sb.toString();
    }
    public void setHistoryEnabled(boolean enabled) {
        this.enableHistory = enabled;
    }

    // Modified version of insert for bulk operations
    public synchronized void insertBulk(char value, int crdtPosition, String userId) {
        if (!editors.contains(userId)) {
            throw new IllegalStateException("User doesn't have edit permissions");
        }

        CharItem newItem = createItemAtPositionBulk(value, crdtPosition, userId);
        items.put(newItem, null); // Direct insert without operation tracking
    }

    // Optimized path generation
    private CharItem createItemAtPositionBulk(char value, int position, String userId) {
        if (items.isEmpty()) {
            return new CharItem(value, userId,0, List.of(0));
        }

        CharItem lastItem = items.lastKey();
        List<Integer> newPath = new ArrayList<>(lastItem.getPath());
        int lastIdx = newPath.size() - 1;
        newPath.set(lastIdx, newPath.get(lastIdx) + 1);

        return new CharItem(value, userId,0, newPath);
    }
    // Operation application (package-private for operations)
    public  void applyInsert(CharItem item) {
        items.put(item, null);
    }

    public  void applyDelete(CharItem item) {
        CharItem existing = items.floorKey(item);
        if (existing != null && existing.equals(item)) {
            existing.setDeleted(true);
        }
    }

    public void applyUndelete(CharItem item) {
        CharItem existing = items.floorKey(item);
        if (existing != null && existing.equals(item)) {
            existing.setDeleted(false);
        }
    }


    public void applyHighlight(List<CharItem> items, String color) {
        for (CharItem item : items) {
            item.setColor(color);
        }
    }
    // User management methods
    public void addEditor(String userId) {
        editors.add(userId);
        viewers.remove(userId);
    }

    public void addViewer(String userId) {
        viewers.add(userId);
        editors.remove(userId);
    }

    public void removeUser(String userId) {
        editors.remove(userId);
        viewers.remove(userId);
        activeUsers.remove(userId);
        cursors.remove(userId);
    }

    // Private helper methods
    // Private helper methods
    private CharItem createItemAtPosition(char value, int position, String userId) {
        List<CharItem> itemList = new ArrayList<>(items.keySet());
        System.out.println("\n[createItemAtPosition] Starting insertion of '" + value +
                "' at position " + position + " by user " + userId);
        System.out.println("Current document items: " + itemList);

        if (itemList.isEmpty()) {
            System.out.println("Document is empty - creating first character with path [0]");
            CharItem newItem = new CharItem(value, userId);
            newItem.setPath(List.of(0));
            return newItem;
        }

        // Adjust position if needed
        int originalPosition = position;
        while (position > 0 && position < itemList.size() &&
                items.comparator().compare(getItemAtPosition(position), getItemAtPosition(position - 1)) <= 0) {
            position--;
        }
        if (originalPosition != position) {
            System.out.println("Adjusted position from " + originalPosition + " to " + position +
                    " due to ordering constraints");
        }

        // Special case: insert at the beginning
        if (position == 0) {
            System.out.println("Inserting at beginning (before " + itemList.getFirst() + ")");
            return createItemAtZero(value, itemList.getFirst(), userId);
        }

        // Special case: insert at the end
        if (position >= itemList.size()) {
            System.out.println("Inserting at end (after " + itemList.getLast() + ")");
            return createItemAfter(value, itemList.getLast(), userId);
        }

        // Insert between two items
        CharItem before = itemList.get(position - 1);
        CharItem after = itemList.get(position);
        System.out.println("Inserting between " + before + " and " + after);
        return createItemBetween(value, before, after, userId);
    }

    private CharItem createItemBefore(char value, CharItem existing, String userId) {
        System.out.println("[createItemBefore] Creating '" + value + "' before " + existing);
        List<Integer> newPath = new ArrayList<>(existing.getPath());
        int lastIdx = newPath.size() - 1;
        newPath.set(lastIdx, newPath.get(lastIdx) + 1);

        CharItem newItem = new CharItem(value, userId);
        newItem.setPath(newPath);
        System.out.println("Created item: " + newItem + " with path " + newPath);
        return newItem;
    }

    private CharItem createItemAfter(char value, CharItem existing, String userId) {
        System.out.println("[createItemAfter] Creating '" + value + "' after " + existing);
        List<Integer> newPath = new ArrayList<>(existing.getPath());
        newPath.add(0);

        CharItem newItem = new CharItem(value, userId);
        newItem.setPath(newPath);
        System.out.println("Created item: " + newItem + " with path " + newPath);
        return newItem;
    }

    private CharItem createItemBetween(char value, CharItem before, CharItem after, String userId) {
        System.out.println("[createItemBetween] Creating '" + value + "' between " +
                before + " and " + after);
        List<Integer> myTest = new ArrayList<>(after.getPath());
        List<Integer> childPath = new ArrayList<>(before.getPath());
        childPath.add(myTest.getLast() - 1); // Start with max value
        CharItem testItem = new CharItem('x', "test", 0, childPath);

        System.out.println("Testing if path " + childPath + " would sort correctly");
        int comparison = items.comparator().compare(testItem, after);
        System.out.println("Comparison result: " + comparison +
                " (negative means valid position)");

        if (comparison < 0) {
            CharItem newItem = new CharItem(value, userId);
            newItem.setPath(childPath);
            System.out.println("Valid between position - created item: " + newItem);
            return newItem;
        } else {
            System.out.println("Path would not sort correctly - falling back to createItemBefore");
            return createItemBefore(value, after, userId);
        }
    }

    private CharItem createItemAtZero(char value, CharItem existing, String userId) {
        System.out.println("[createItemAtZero] Creating '" + value + "' before " + existing);
        List<Integer> newPath = new ArrayList<>(existing.getPath());

        int lastIdx = newPath.size() - 1;
        newPath.set(lastIdx, newPath.get(lastIdx) - 1);

        CharItem newItem = new CharItem(value, userId);
        newItem.setPath(newPath);
        System.out.println("Created item: " + newItem + " with path " + newPath);
        return newItem;
    }

    public void applyOperation(Operation op) {
        String userId = op.getUserId();

        // Initialize history for new users
        userOperationHistory.putIfAbsent(userId, new ArrayList<>());
        userHistoryPointers.putIfAbsent(userId, -1);
        op.apply(this);
        List<Operation> history = userOperationHistory.get(userId);
        int pointer = userHistoryPointers.get(userId);

        if (pointer < history.size() - 1) {
            history.subList(pointer + 1, history.size()).clear();
        }

        // Add to history and update pointer
        history.add(op);
        userHistoryPointers.put(userId, history.size() - 1);
    }

    public void removeHighlight(int startPos, int endPos, String userId) {
        List<CharItem> affectedItems = getItemsInRange(startPos, endPos);
        applyOperation(new RemoveHighlightOperation(affectedItems, userId));
    }

    public void undo(String userId) {
        if (!userOperationHistory.containsKey(userId) ||
                userHistoryPointers.get(userId) < 0) {
            System.out.println("Nothing to undo for user " + userId);
            return;
        }

        List<Operation> history = userOperationHistory.get(userId);
        int pointer = userHistoryPointers.get(userId);

        Operation op = history.get(pointer);
        Operation inverse = op.getInverse();

        if (inverse != null) {
            inverse.apply(this);
            userHistoryPointers.put(userId, pointer - 1);
            System.out.println("Undo: " + op.getClass().getSimpleName() +
                    " for user " + userId);
        }
    }

    public void redo(String userId) {
        if (!userOperationHistory.containsKey(userId)) {
            System.out.println("No history for user " + userId);
            return;
        }

        List<Operation> history = userOperationHistory.get(userId);
        int pointer = userHistoryPointers.getOrDefault(userId, -1);

        // Check if redo is possible
        if (pointer + 1 >= history.size()) {
            System.out.println("Nothing to redo for user " + userId);
            return;
        }

        // Redo the next operation in history
        Operation op = history.get(pointer + 1);

        // Here we get the inverse of the inverse (i.e., the original operation)
        Operation redoOp = op.getInverse() != null ? op.getInverse().getInverse() : null;

        if (redoOp != null) {
            redoOp.apply(this);
            userHistoryPointers.put(userId, pointer + 1);
            System.out.println("Redo: " + redoOp.getClass().getSimpleName() + " for user " + userId);
        } else {
            System.out.println("Cannot redo: inverse operation not available.");
        }
    }



    /* ---------- comment management ----------------------------------- */
    public void removeComment(String commentId, String userId) {

        CommentPosition c = comments.get(commentId);
        if (c == null) return;                 // nothing to remove

        applyOperation(new RemoveCommentOperation(userId,c));
    }
    public void addComment(int startPos, int endPos,
                           String color, String userId,String text) {
        if (startPos < 0 || endPos > getText().length() || endPos <= startPos) {
            throw new IllegalArgumentException("comment range is invalid");
        }
        final String uniqueId = UUID.randomUUID().toString();
        CommentPosition comment = new CommentPosition(
                uniqueId,
                userId,
                color,
                startPos,
                endPos,
                text);
        applyOperation(new AddCommentOperation(comment, userId));
    }
    public void applyAddComment(CommentPosition comment) {
        comments.put(comment.getId(), comment);
    }
    public void applyRemoveComment(CommentPosition comment) {
        comments.remove(comment.getId());
    }

    private void updateCommentsOnDelete(int deletedPos) {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, CommentPosition> entry : comments.entrySet()) {
            CommentPosition comment = entry.getValue();
            int start = comment.getStartPos();
            int end = comment.getEndPos();

            // Check if the deleted character is within the comment range
            if (deletedPos >= start && deletedPos < end) {
                toRemove.add(comment.getId());
            }
        }

        for (String commentId : toRemove) {
            removeComment(commentId, "system");  // Use "system" or actual userId if you prefer
        }
    }


    public Collection<CommentPosition> getComments() {
        return comments.values();
    }
    public Collection<String> getActiveUsers() {
        return activeUsers;
    }
    private static class CharItemComparator implements Comparator<CharItem> {
        @Override
        public int compare(CharItem a, CharItem b) {
            int minLength = Math.min(a.getPath().size(), b.getPath().size());
            for (int i = 0; i < minLength; i++) {
                int cmp = Integer.compare(a.getPath().get(i), b.getPath().get(i));
                if (cmp != 0) return cmp;
            }
            if (a.getPath().size() != b.getPath().size()) {
                return Integer.compare(a.getPath().size(), b.getPath().size());
            }
            int userCmp = b.getUserId().compareTo(a.getUserId());
            if (userCmp != 0) return userCmp;
            return Long.compare(a.getTimestamp(), b.getTimestamp());
        }
    }

    // Cursor management
    public void updateCursor(String userId, int position, String color) {
        if (!activeUsers.contains(userId)) return;
        cursors.put(userId, new CursorPosition(userId, position, color));
    }

    public Collection<CursorPosition> getAllCursors() {
        return cursors.values();
    }

    public synchronized int clientToCrdtPosition(int clientPos) {
        if (clientPos < 0)
            return 0;

        int visibleCount = 0;
        int crdtPos = 0;

        for (CharItem item : items.keySet()) {
            if (visibleCount >= clientPos) {
                return crdtPos;
            }
            if (!item.isDeleted()) {
                visibleCount++;
            }
            crdtPos++;
        }
        return crdtPos;
    }

    public boolean canEdit(String userId) {
        return editors.contains(userId);
    }

    public boolean canView(String userId) {
        return viewers.contains(userId);
    }
    public DocumentStateResponse getCurrentState(String operationType, String triggeringUser) {
        return new DocumentStateResponse(
                getText(),
                getAllCursors(),
                getComments(),
                operationType,
                triggeringUser
        );
    }

}