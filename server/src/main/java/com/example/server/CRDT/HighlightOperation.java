package com.example.server.CRDT;

import java.util.List;

public class HighlightOperation implements Operation {
    private final List<CharItem> items;
    private final String color;

    public HighlightOperation(List<CharItem> items, String color) {
        this.items = items;
        this.color = color;
    }

    @Override
    public void apply(CRDTDocument document) {
        document.applyHighlight(items, color);
    }

    @Override
    public Operation getInverse() {
        return new RemoveHighlightOperation(items);
    }
}
