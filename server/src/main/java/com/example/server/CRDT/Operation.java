package com.example.server.CRDT;
import java.util.List;


public interface Operation {
    void apply(CRDTDocument document);
    Operation getInverse();
}



