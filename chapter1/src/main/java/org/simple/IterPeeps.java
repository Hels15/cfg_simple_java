package org.simple;

import org.simple.bbs.BB;
import org.simple.instructions.ConstantInstr;
import org.simple.instructions.Instr;

import java.util.ArrayList;

public abstract class IterPeeps {
    private static final Utils.WorkListI<Instr> WORK = new Utils.WorkListI<>();

    public static <N extends Instr>  Instr add(Instr i) {
        return (N)WORK.push(i);
    }

    public static void addAll( ArrayList<Instr> ary ) {
        WORK.addAll(ary);
    }

    /**
     * Iterate peepholes to a fixed point
     */
    public static void iterate(BB entry) {
        Instr i;
        int cnt=0;

        while((i = WORK.pop()) != null) {
            if(i.isDead() || i.debug()) continue;
            cnt++;

            Instr x = i.peepholeOpt();
            if(x != null) {
                if(x.isDead()) continue;
                if(x._type == null) x.setType(x.compute());
                // Changes require neighbors onto the worklist
                if(x != i || !(x instanceof ConstantInstr)) {
                    for(Instr z : i._outputs) WORK.push(z);

                    WORK.push(x);
                    if(x != i) {
                        for(Instr z : i._inputs) WORK.push(z);
                        i.subsume(x);
                    }
                }
                i.moveDepsToWorkList();
            }
        }
    }
}
