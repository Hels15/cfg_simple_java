package org.simple.bbs;

public class ExitBB extends BB {

    @Override
    public void addSuccessor(BB bb) {
        throw new UnsupportedOperationException("ExitBB cannot have successors");
    }
}

