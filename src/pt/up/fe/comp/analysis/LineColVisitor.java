package pt.up.fe.comp.analysis;

import pt.up.fe.comp.BaseNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;

public class LineColVisitor extends PostorderJmmVisitor<Integer, Integer> {

    public LineColVisitor() {
        setDefaultVisit(this::placeLineCol);
    }

    private Integer placeLineCol(JmmNode node, Integer dummy) {
        var baseNode = (BaseNode) node;

        node.put("line", Integer.toString(baseNode.getBeginLine()));
        node.put("col", Integer.toString(baseNode.getBeginColumn()));
        return 0;
    }
}
