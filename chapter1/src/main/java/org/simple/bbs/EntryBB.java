package org.simple.bbs;

public class EntryBB extends BB {
    public EntryBB() {
        super();
    }

    public @Override BB idom() {
        return null;
    }
    @Override
    public void addPredecessor(BB bb) {
        throw new UnsupportedOperationException("EntryBB cannot have predecessors");
    }
}