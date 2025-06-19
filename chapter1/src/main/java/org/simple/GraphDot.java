package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.IfInstr;
import org.simple.instructions.Instr;
import org.simple.instructions.ScopeInstr;

import java.util.*;

public class GraphDot {

    public String generateDotOutput(EntryBB entry, Parser parser) {
        StringBuilder sb = new StringBuilder();

        sb.append("digraph CFG {\n\n");

        sb.append("/*\n");
        sb.append(parser.src());
        sb.append("\n*/\n");

        sb.append("  node [shape=plaintext];\n\n");

        Set<BB> visited = new HashSet<>();
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);

        Map<BB, String> bbIds = new HashMap<>();

        // Traverse CFG and generate cluster for each BB
        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            if (!visited.add(bb)) continue;

            // Assign a custom ID and store it
            String bbId = "bb" + bb._nid;
            bbIds.put(bb, bbId);

            sb.append("/*\n");

            sb.append(parser.src());
            sb.append("\n*/\n");

            sb.append("  ").append(bbId).append(" [\n");
            sb.append("    label=<\n");
            sb.append("      <table border=\"1\" cellborder=\"0\" cellspacing=\"0\" cellpadding=\"4\">\n");
            sb.append("        <tr><td bgcolor=\"lightblue\" align=\"left\">").append(bbId).append("</td></tr>\n");

            for (Instr instr : bb._instrs) {
                sb.append("        <tr><td align=\"left\">")
                        .append(escapeHtml(instr.print()))
                        .append("</td></tr>\n");
            }

            sb.append("      </table>\n");
            sb.append("    >\n");
            sb.append("  ];\n");

            queue.addAll(bb._succs);
        }

        // Add edges between BBs
        sb.append("\n");
        for (Map.Entry<BB, String> entrySet : bbIds.entrySet()) {
            BB bb = entrySet.getKey();
            String fromId = entrySet.getValue();

            for (BB succ : bb._succs) {
                String toId = bbIds.get(succ);
                if (toId != null) {
                    sb.append("  ").append(fromId)
                            .append(" -> ").append(toId)
                            .append(" [label=\"").append(escapeHtml(succ._label)).append("\"];\n");
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
