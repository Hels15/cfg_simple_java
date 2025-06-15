package org.simple;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;
import org.simple.instructions.ScopeInstr;

import java.util.*;

// Todo: Nicer GRAPH
public class GraphDot {

    // scope edges has to come somewhere
    public String generateDotOutput(EntryBB entry, ScopeInstr scopeA) {
        StringBuilder sb = new StringBuilder();

        sb.append("digraph G {\n\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=none, fontname=Helvetica];\n\n");

        Set<BB> visited = new HashSet<>();
        Queue<BB> queue = new LinkedList<>();
        queue.add(entry);

        Map<BB, String> bbIds = new HashMap<>();
        int clusterId = 0;

        // Traverse CFG and generate cluster for each BB
        while (!queue.isEmpty()) {
            BB bb = queue.poll();
            if (!visited.add(bb)) continue;

            // Assign a custom ID and store it
            String bbId = "bb" + clusterId;
            String customLabel = String.format("BB #%d", clusterId);
            bbIds.put(bb, bbId);

            sb.append(String.format("  subgraph cluster_%d {\n", clusterId));
            sb.append("    style=filled;\n");
            sb.append("    color=lightgrey;\n");
            sb.append(String.format("    label = \"%s: type: #%s\";\n", customLabel, bb._type.toString()));
            sb.append(String.format("    %s [label=<\n", bbId));
            sb.append("      <TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n");

            // Insert BB ID inside block
            sb.append(String.format("        <TR><TD><b>%s</b></TD></TR>\n", customLabel));
            sb.append(String.format("        <TR><TD><FONT COLOR=\"blue\">%s</FONT></TD></TR>\n", bb._label));

            for (Instr instr : bb._instrs) {
                sb.append(String.format("        <TR><TD>i%d: %s</TD></TR>\n", instr._nid, instr.toString()));
            }

            sb.append("      </TABLE>\n");
            sb.append("    >];\n");
            sb.append("  }\n\n");

            // Add successors to queue
            queue.addAll(bb._succs);
            clusterId++;
        }

        // Add edges between BBs
        sb.append("\n");
        for (Map.Entry<BB, String> entrySet : bbIds.entrySet()) {
            BB bb = entrySet.getKey();
            String fromId = entrySet.getValue();

            for (BB succ : bb._succs) {
                String toId = bbIds.get(succ);
                if (toId != null) {
                    sb.append(String.format("  %s -> %s;\n", fromId, toId));
                }
            }
        }

        sb.append("}\n");

        return sb.toString();
    }
}