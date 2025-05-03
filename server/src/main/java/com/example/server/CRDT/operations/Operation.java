package com.example.server.CRDT.operations;
import com.example.server.CRDT.CRDTDocument;


public interface Operation {
    void apply(CRDTDocument document);
    Operation getInverse();
}



