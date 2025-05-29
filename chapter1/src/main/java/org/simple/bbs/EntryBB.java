package org.simple.bbs;

public class EntryBB extends BB {
    public EntryBB() {
        super();
    }

    @Override
    public void addPredecessor(BB bb) {
        throw new UnsupportedOperationException("EntryBB cannot have predecessors");
    }
}