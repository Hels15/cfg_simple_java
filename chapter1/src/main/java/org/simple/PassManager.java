package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.IfInstr;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;
import org.simple.type.Type;
import org.simple.type.TypeTuple;

import java.util.*;

// maybe a work-list that works until a fixed point
public class PassManager {
    // kill dead basic blocks
    // Need to return last BB in the processed graph
    void bb_dead(EntryBB entry) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);

        Set<BB> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            if(!visited.add(bb)) continue;
            // just with single predecessor - this ignores the if case
            if(bb._preds.size() == 1 && bb._preds.getFirst().dead()) {
                bb._type = Type.XCONTROL;
            }


            if(bb.dead() && !(bb instanceof ExitBB)) {
                // remove dead BB
                bb._preds.forEach(pred -> pred._succs.remove(bb));
                bb._succs.forEach(succ -> succ._preds.remove(bb));
                bb._instrs.clear();
                continue;
            }

            bb._instrs.removeIf(instr ->
                    instr instanceof IfInstr ifInstr &&
                            (ifInstr._type == TypeTuple.IF_TRUE || ifInstr._type == TypeTuple.IF_FALSE)
            );

            queue.addAll(bb._succs);
        }

    }
    // Call it first then use worklist
    // combine basic blocks(single successor/single pred)
    void combine(EntryBB entry) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);
        Set<BB> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            if(!visited.add(bb)) continue;

            if(bb._succs.size() == 1 && bb._succs.getFirst()._preds.size() == 1 && !(bb instanceof EntryBB || bb instanceof ExitBB) ) {
                // combine bb(s)
                BB succ = bb._succs.getFirst();
                for(Instr instr: succ._instrs) {
                    bb.addInstr(instr);
                }
                succ._type = Type.XCONTROL;

                bb._succs.clear();
                bb._succs.addAll(succ._succs);
                // tag as dead
            }
            queue.addAll(bb._succs);
        }
    }

//    void collect_returns(EntryBB entry, ArrayList<ReturnInstr> returns) {
//        Queue<BB> queue = new LinkedList<>();
//        queue.add(entry);
//
//        Set<ReturnInstr> visited = new HashSet<>();
//        while (!queue.isEmpty()) {
//            BB bb = queue.poll();
//            for (Instr instr : bb._instrs) {
//                if (instr instanceof ReturnInstr ret) {
//                    if (!visited.add(ret)) returns.add(ret);
//
//                }
//            }
//            queue.addAll(bb._succs);
//        }
//
//    }
}
