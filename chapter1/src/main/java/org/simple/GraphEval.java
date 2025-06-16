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
    public GraphEval(EntryBB entry) {
        _entry = entry;
    }

    void visit(Instr instr) {
        if (Objects.requireNonNull(instr) instanceof ReturnInstr ret) {
            result = new Result(ResultType.VALUE, getValue(ret.in(0)));
    }   if (Objects.requireNonNull(instr) instanceof AssignmentInstr assignment) {
            // Todo: do not hard code it for arg
            cacheValues.put(Parser._arg, getValue(assignment.in(0)));
            result = new Result(ResultType.VALUE, getValue(assignment.in(0)));
        }

        //throw Utils.TODO("unexpected instr " + instr);

    }
    private long div(DivInstr div) {
        long in2 = getValue(div.in(1));
        // dividing by zero is zero and not undefined
        return in2 == 0 ? 0 : getValue(div.in(1)) / in2;
    }
    private long phi(PhiInstr phi) {
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
            if(bb._nid == 12) {
                System.out.println("BB 12");
            }
            for(int i = 0; i < bb._instrs.size(); i++) {
                if(bb._instrs.get(i) instanceof ReturnInstr) {
                    System.out.print("Here");
                }
                if(i + 1 == bb._instrs.size() && bb._instrs.get(i) instanceof IfInstr if_instr) {
                    conditional = true;
                    // first bb is true
                    // second bb is false
                    if(getValue(if_instr.in(0)) != 0) queue.add(bb._succs.getFirst());
                    else queue.add(bb._succs.getLast());
                }

                if(!conditional) visit(bb._instrs.get(i));
            }
        if(!conditional)  queue.addAll(bb._succs);
        else conditional = false;

        }
        return result;
    }
}
