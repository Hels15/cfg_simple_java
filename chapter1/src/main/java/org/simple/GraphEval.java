package org.simple;


import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.*;
import org.simple.type.TypeInteger;

import java.io.ObjectStreamClass;
import java.util.*;

public class GraphEval {
    public enum ResultType { VALUE, FALLTHROUGH, TIMEOUT }
    public record Result(ResultType type, long value) {}
    Result result;
    public EntryBB _entry = null;
    private final HashMap<Instr, Long> cacheValues = new HashMap<>();
    private final HashMap<Instr, Long> inLoopCache = new HashMap<>();
    public GraphEval(EntryBB entry) {
        _entry = entry;
    }

    void visit(Instr instr) {
        if (Objects.requireNonNull(instr) instanceof ReturnInstr ret) {
            result = new Result(ResultType.VALUE, getValue(ret.in(0)));
    }
        // Todo: maybe some error handling here
    }
    private long div(DivInstr div) {
        long in2 = getValue(div.in(1));
        // dividing by zero is zero and not undefined
        return in2 == 0 ? 0 : getValue(div.in(1)) / in2;
    }
    private Instr phi_idx(PhiInstr phi) {
        return switch (phi._bb._label) {
            case "true" -> phi.in(0);
            case "false" -> phi.in(1);
            case "header" ->
                // when in an if condition(loop header) pick the true case
                // if(phi(bb4, arg, Phi_arg + 1) < 10) < 10
                    phi.in(0);
            default -> null;
        };

    }
    private long phi(PhiInstr phi) {
        inLoopCache.put(phi, 0L);
        return switch (phi._bb._label) {
            case "true" -> getValue(phi.in(0));
            case "false" -> getValue(phi.in(1));
            case "header" ->
                // when in an if condition(loop header) pick the true case
                // if(phi(bb4, arg, Phi_arg + 1) < 10) < 10
                    getValue(phi.in(0));
            default -> throw Utils.TODO("unexpected phi " + phi + " in bb " + phi._bb._label);
        };
    }
    private long getValue(Instr instr) {
        var cache = cacheValues.get(instr);
        if (cache != null) return cache;

        // to have precise computation, if it's an arithmetic op and it's a predicate then
        // we won't compute the value based on what the compiler generated
        return switch(instr) {
            case ConstantInstr cons  -> ((TypeInteger)cons.compute()).value();
            case AddInstr      add   -> getValue(add.in(0)) + getValue(add.in(1));
            case BoolInstr.EQ  eq    -> getValue(eq.in(0)) == getValue(eq.in(1)) ? 1 : 0;
            case BoolInstr.LE  le    -> getValue(le.in(0)) <= getValue(le.in(1)) ? 1 : 0;
            case BoolInstr.LT  lt    -> getValue(lt.in(0)) < getValue(lt.in(1)) ? 1 : 0;
            case DivInstr      div   -> div(div);
            case PhiInstr      phi   -> phi(phi);
            case MinusInstr    minus -> -getValue(minus.in(0));
            case MulInstr      mul   -> getValue(mul.in(0)) * getValue(mul.in(1));
            case NotInstr      not   -> getValue(not.in(0)) == 0 ? 1 : 0;
            case SubInstr      sub   -> getValue(sub.in(0)) - getValue(sub.in(1));
            default                  -> throw Utils.TODO("unexpected instr " + instr);
        };
    }
    Result evaluate(long arg) {
        Queue<BB> queue = new LinkedList<>();
        queue.add(_entry);

        // cache arg to be the provided value
        cacheValues.put(Parser._arg, arg);

        boolean conditional = false;
        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            for(int i = 0; i < bb._instrs.size(); i++) {
                if(i + 1 == bb._instrs.size() && bb._instrs.get(i) instanceof IfInstr if_instr) {
                    conditional = true;
                    // first bb is true
                    // second bb is false
                    if(getValue(if_instr.in(0)) != 0) queue.add(bb._succs.getFirst());
                    else queue.add(bb._succs.getLast());
                }

                if(!conditional) visit(bb._instrs.get(i));
            }

            // Here's the part where we I handle phis(s)
            // if we come across something like arg = arg + 1 then we want to increment arg
            // and store it in the cache so that we can use it recursively later
            // The general idea is that in the predicate we still use the old value but
            // this is not a problem because the computation is involved in the predicate itself:
            // (Phi(bb4,arg,Phi_arg+1))
            // loop continues - control keeps on flowing around
            boolean isLoopBackFlow =
                    (bb._kind == BB.BBKind.BACK_EDGE || bb._kind == BB.BBKind.CONTINUE) &&
                            bb._succs.size() == 1 &&
                            bb._succs.getFirst()._kind == BB.BBKind.LOOP_HEADER;

            // we exit
            boolean isLoopExit =
                    bb._kind == BB.BBKind.BREAK ||
                            (bb._succs.size() == 1 && bb._succs.getFirst()._kind == BB.BBKind.LOOP_EXIT);

            // Here make sure its actually a backedge need to extend the fales branch to also have a backedge successor
            // This also triggers for the bb that is just before the loop header
            // Todo: it should be backedge - Technically here normalBackFlow and isLoopBackFlow should be the same
            boolean NormalBackFlow = bb._succs.size() == 1 && bb._succs.getFirst()._kind == BB.BBKind.LOOP_HEADER;
            if(NormalBackFlow == true && bb._nid != 2) {
                System.out.print("Here");
            }
            if (isLoopBackFlow || isLoopExit || NormalBackFlow) {
                for (Instr _phi : inLoopCache.keySet()) {
                    if (cacheValues.containsKey(phi_idx((PhiInstr)_phi))) {
                        long value = getValue(_phi.in(1));
                        cacheValues.put(_phi.in(0), value);
                    }
                }
            }

        if(!conditional)  queue.addAll(bb._succs);
        else conditional = false;

        }
        return result;
    }
}
