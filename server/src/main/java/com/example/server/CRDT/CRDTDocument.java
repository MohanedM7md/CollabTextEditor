package com.example.server.CRDT;

import com.example.server.CRDT.operations.*;
import com.example.server.dto.responses.DocumentStateResponse;
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


    // Document modification methods
    public synchronized void insert(char value, int position,String userId) throws IllegalStateException {
        if (position < 0 || position > items.size()) {
            throw new IndexOutOfBoundsException("Invalid insert position");
        }
        if (!editors.contains(userId)) {
            throw new IllegalStateException("User doesn't have edit permissions");
        }
        CharItem newItem = createItemAtPosition(value, position,userId);
        applyOperation(new InsertOperation(newItem,userId));
    }

    public synchronized void delete(int position,String userId) throws IllegalStateException {
        if (!editors.contains(userId)) {
            throw new IllegalStateException("User doesn't have edit permissions");
        }
        if (position < 0 || position >= items.size()) return;
        CharItem item = getItemAtPosition(position);
        applyOperation(new DeleteOperation(item,userId));
    }
    public synchronized void addComment(int startPos, int endPos, String comment, String userId) throws IllegalStateException {
        List<CharItem> affectedItems = getItemsInRange(startPos, endPos);
        applyOperation(new CommentOperation(affectedItems, comment,userId));
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

    public void applyComment(List<CharItem> items, String comment) {
        for (CharItem item : items) {
            item.setComment(comment);
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
    private CharItem createItemAtPosition(char value, int position, String userId) {
        List<CharItem> itemList = new ArrayList<>(items.keySet());

        if (itemList.isEmpty()) {
            CharItem newItem = new CharItem(value, userId);
            newItem.setPath(List.of(0));
            return newItem;
        }

        if (position <= 0) {
            return createItemBefore(value, itemList.getFirst(),userId);
        }

        if (position >= itemList.size()) {
            return createItemAfter(value, itemList.getLast(),userId);
        }

        return createItemBetween(value, itemList.get(position-1), itemList.get(position),userId);
    }

    private CharItem createItemBefore(char value, CharItem existing, String userId) {
        List<Integer> newPath = new ArrayList<>(existing.getPath());
        int lastIdx = newPath.size() - 1;
        newPath.set(lastIdx, newPath.get(lastIdx) + 1);

        CharItem newItem = new CharItem(value, userId);
        newItem.setPath(newPath);
        return newItem;
    }

    private CharItem createItemAfter(char value, CharItem existing, String userId) {
        List<Integer> newPath = new ArrayList<>(existing.getPath());
        newPath.add(0);

        CharItem newItem = new CharItem(value, userId);
        newItem.setPath(newPath);
        return newItem;
    }

    private CharItem createItemBetween(char value, CharItem before, CharItem after, String userId) {
        List<Integer> childPath = new ArrayList<>(before.getPath());
        childPath.add(0);
        CharItem testItem = new CharItem('x', "test", 0, childPath);

        if (items.comparator().compare(testItem, after) < 0) {
            CharItem newItem = new CharItem(value, userId);
            newItem.setPath(childPath);
            return newItem;
        } else {
            return createItemBefore(value, after,userId);
        }
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
    public void removeComment(int startPos, int endPos,String userId) {
        List<CharItem> affectedItems = getItemsInRange(startPos, endPos);
        applyOperation(new RemoveCommentOperation(affectedItems ,userId));
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
        if (!userOperationHistory.containsKey(userId) ||
                userHistoryPointers.get(userId) >= userOperationHistory.get(userId).size() - 1) {
            System.out.println("Nothing to redo for user " + userId);
            return;
        }

        List<Operation> history = userOperationHistory.get(userId);
        int pointer = userHistoryPointers.get(userId) + 1;

        Operation op = history.get(pointer);
        op.apply(this);

        userHistoryPointers.put(userId, pointer);
        System.out.println("Redo: " + op.getClass().getSimpleName() +
                " for user " + userId);
    }

    public String getActiveUsers() {
        return activeUsers.toString();
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

    // User presence
    public void userConnected(String userId) {
        activeUsers.add(userId);
    }

    public void userDisconnected(String userId) {
        activeUsers.remove(userId);
        cursors.remove(userId);
    }

    public int getClientPosition(CharItem item) {
        int pos = 0;
        for (CharItem current : items.keySet()) {
            if (current.equals(item)) {
                return current.isDeleted() ? -1 : pos; // Return -1 for deleted items
            }
            if (!current.isDeleted()) {
                pos++; // Only count non-deleted items for client positions
            }
        }
        return -1; // Item not found
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
                operationType,
                triggeringUser
        );
    }

}