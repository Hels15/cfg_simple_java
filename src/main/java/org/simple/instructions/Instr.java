package org.simple.instructions;

import java.util.*;

// Todo add label
public abstract class Instr {
    public final int _nid;

    // for use-def chains
    public final ArrayList<Instr> _inputs;

    public final ArrayList<Instr> _outputs;

    private static int UNIQUE_ID = 1;

    Instr(Instr... inputs) {
        _nid = UNIQUE_ID++;
        _inputs = new ArrayList<>();
        _outputs = new ArrayList<>();
    }
}
