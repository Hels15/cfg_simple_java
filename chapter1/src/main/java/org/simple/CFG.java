package org.simple;

import org.simple.bbs.BB;

import java.util.ArrayList;

// Might not need this as the graph can be visited from the entry block simply
public class CFG {
    public final ArrayList<BB> _bbs;

    CFG() {
        _bbs = new ArrayList<>();
    }

    void addBB(BB bb) {
        _bbs.add(bb);
    }
}
