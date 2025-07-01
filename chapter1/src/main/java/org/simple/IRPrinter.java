package org.simple;

import org.simple.bbs.BB;
import org.simple.instructions.Instr;

import java.util.HashSet;
import java.util.Set;

public class IRPrinter {
    public static String prettyPrint(BB entry) {
        Utils.WorkList<BB> wl = new Utils.WorkList<BB>();
        wl.push(entry);
        StringBuilder sb = new StringBuilder();

        Set<BB> visited = new HashSet<BB>();

        while (!wl.isEmpty()) {
            BB bb = wl.pop();
            if(!visited.add(bb)) continue;

            sb.append("BB ").append(bb._nid).append(":\n");
            for(Instr instr: bb._instrs) {
                // nid same as gvn number
                sb.append(printColoured(instr._nid, "\t  %" + instr._nid));
                sb.append(printColoured(instr._nid, "\t  =" + instr.toString()));
                sb.append("\t \n");
            }

             wl.addAll(bb._succs);
        }

        return sb.toString();

    }


    public static String printColoured(int id, String text) {
        int colourCode = coolAnsiColour(id);
        return "\u001B[" + colourCode + "m" + text + "\u001B[0m";
    }

    public static int coolAnsiColour(int x) {
        int y = x % 100;
        if (y > 7) {
            return 90 + (y - 7);
        } else {
            return 32 + y;
        }
    }

}
