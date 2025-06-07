package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;
import org.simple.type.Type;

import java.util.*;

// maybe a work-list that works until a fixed point
public class PassManager {
    // kill dead basic blocks
    // Need to return last BB in the processed graph
    void bb_dead(EntryBB entry) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);
        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            if(bb._type == Type.XCONTROL && !(bb instanceof ExitBB)) {
                // remove dead BB
                bb._preds.forEach(pred -> pred._succs.remove(bb));
                bb._succs.forEach(succ -> succ._preds.remove(bb));
                bb._instrs.clear();
                continue;
            }
            queue.addAll(bb._succs);
        }

    }
    // combine basic blocks(single successor/single pred)
    void combine(EntryBB entry) {

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
