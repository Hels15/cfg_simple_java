package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.*;
import org.simple.type.Type;
import org.simple.type.TypeTuple;

import java.util.*;

// maybe a work-list that works until a fixed point
public class PassManager {
    public int combine_pass = 0;
    public int dce_pass     = 0;
    boolean bb_dead(BB bb, Parser parser) {
        boolean changed = false;
        int count_bb_instr = bb._instrs.size();

        // if a basic block ends with a break it can only have one successor(the shared exit)
        // this opt gets rid of the other successor
        if(bb._kind == BB.BBKind.BREAK && bb._succs.size() == 2) {
            for(int i = 0; i < bb._succs.size(); i++) {
                BB succ = bb._succs.get(i);
                if(succ._kind == BB.BBKind.LOOP_EXIT) {
                    BB to_remove = bb._succs.get(1 - i);
                    bb._succs.remove(1 -i);
                    to_remove._preds.remove(bb);
                    changed = true;
                }
            }
        }

        // if a basic block ends with a continue it can only have one successor(the loop header)
        // this opt gets rid of the other successor
        if(bb._kind == BB.BBKind.CONTINUE && bb._succs.size() == 2) {
            for(int i = 0; i < bb._succs.size(); i++) {
                BB succ = bb._succs.get(i);
                if(succ._kind == BB.BBKind.LOOP_HEADER) {
                    // get rid of the other edge, keep the loop header one
                    bb._succs.remove(1-i);
                    succ._preds.remove(bb);
                    changed = true;
                }
            }
        }

        // if the predecessor of the current block is dead, mark current block as dead
        if(bb._preds.size() == 1 && bb._preds.getFirst().dead()) {
            bb._type = Type.XCONTROL;
            changed = true;
        }

        // if the current block is dead and it is not an exit block
        // then we can remove it from the graph
        if(bb.dead() && !(bb instanceof ExitBB)) {
            // remove dead BB
            bb._preds.forEach(pred -> pred._succs.remove(bb));
            bb._succs.forEach(succ -> succ._preds.remove(bb));
            bb._instrs.clear();
            changed = true;
        }

        // If one of the predecessors of the phi is dead, then just return the live input.
        for (int i = 0; i < bb._instrs.size(); i++) {
            Instr instr = bb._instrs.get(i);
            if (instr instanceof PhiInstr phi) {
                Instr value = null;
                if (phi._bb._preds.getFirst().dead()) value = phi.in(phi.nIns() - 1);
                if (phi._bb._preds.getLast().dead())  value = phi.in(0);
                if (value != null) {bb._instrs.set(i, value); changed = true;}
            }
        }

        // remove if instructions from the bb when we know that one block is surely dead.
        // when we come across IF_TRUE or IF_FALSE we will mark the other bb as dead so the instruction
        // can be safely removed
        // see if instruction create_bbs function;
        bb._instrs.removeIf(instr ->
                instr instanceof IfInstr ifInstr &&
                        (ifInstr._type == TypeTuple.IF_TRUE || ifInstr._type == TypeTuple.IF_FALSE)
        );

        if(count_bb_instr != bb._instrs.size()) changed = true;
        return changed;
    }
    void bb_dead_main(EntryBB entry, Parser parser) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);

        Set<BB> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            BB bb = queue.poll();

            if(!visited.add(bb)) continue;

            while(bb_dead(bb, parser)) {
                dce_pass++;
            }

            queue.addAll(bb._succs);
        }

    }

    boolean combine(BB bb, Parser parser) {
           boolean changed = false;
           // if the next block has only one predecessor and it is the current block
           // then we can combine the two blocks and kill the successor, we also add the successors of the killed
           // block to the current block so that the graph still remains sound
            if(bb._succs.size() == 1 && bb._succs.getFirst()._preds.size() == 1 && !(bb instanceof EntryBB || bb._succs.getFirst() instanceof ExitBB) ) {
                BB succ = bb._succs.getFirst();
                for(Instr instr: succ._instrs) {
                    bb.addInstr(instr);
                }
                succ._type = Type.XCONTROL;

                bb._succs.clear();
                bb._succs.addAll(succ._succs);
                changed = true;
            }

        // Since we do not want implicit jumps in the
        if(bb._instrs.isEmpty() && bb._succs.size() == 1) {
            bb._instrs.add(new GotoInstr(bb).keep().peephole());
            changed = true;
        }

        return changed;
    }

    void bb_combine_main(EntryBB main, Parser parser) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(main);
        Set<BB> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            BB bb = queue.poll();

            if(!visited.add(bb)) continue;

            while(combine(bb, parser)) {
                combine_pass++;
            };

            queue.addAll(bb._succs);
        }
    }
}
