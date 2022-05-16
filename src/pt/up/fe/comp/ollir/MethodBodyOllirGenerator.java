package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class MethodBodyOllirGenerator extends AJmmVisitor<Object, String> {

    private final SymbolTable symbolTable;
    private final StringBuilder code;
    private String methodName;


    public MethodBodyOllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
        this.methodName = "";
        // ASSIGN
        // IF
        //WHILE

        // IGNORE
        /*
        addVisit(AstNode.Var_Decl, this::visitVarDecl);
        addVisit(AstNode.Type, this::visitType);*/


        addVisit(AstNode.Method_Body, this::visitMethodBody);
        addVisit(AstNode.Assign, this::visitAssign);
        addVisit(AstNode.Array_Creation, this::visitArrayCreation);
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);

        // addVisit(AstNode.Array_Access, this::visitArrayAccess);

        /*
        addVisit(AstNode.If, this::visitIf);
        addVisit(AstNode.While, this::visitWhile);
        addVisit(AstNode.Method_Call, this::visitMethodCall); // Does this include chained dot expressions
        */

        setDefaultVisit(this::visitDefault);
    }

    private String visitDotLinked(JmmNode jmmNode, Object o) {

        System.out.println("Dot Linked");

        DotLinkedOllirGenerator dotLinkedOllirGenerator = new DotLinkedOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
        String instruction = dotLinkedOllirGenerator.visit(jmmNode, null);

        code.append(dotLinkedOllirGenerator.getCode());

        code.append(instruction);
        return "";
    }

    private String visitClassCreation(JmmNode classDecl, Object o) {
        /*
        aux1.Fac :=.Fac new(Fac).Fac;
        invokespecial(aux1.Fac,"<init>", 2).V;
        */
        String className = classDecl.get("name");
        return "new(" + className + ")." + className;
    }

    private String visitArrayCreation(JmmNode arrayCreationNode, Object o) {

        /*
            int[] c = new int[A.length];

            simplest case is int[] c = new int[2];
            goes to:
            c.array.i32 :=.array.i32 new(array, 2.i32).array.i32
         */

        JmmNode arrayAccessNode = arrayCreationNode.getChildren().get(0); // This is the BinOp that has the length of the array
        JmmNode arrayAccessOperationNode = arrayAccessNode.getChildren().get(0); // This is the array

        arrayAccessNode =  arrayAccessNode.getChildren().get(0);
        //TODO: BandAid Should a class separation : right side of Assigment Exprs -> BinOp or other and visit in AssingmentVisitor

        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable, this.methodName);
        String arrayLength = binOpOllirGenerator.visit(arrayAccessNode, arrayAccessNode);
        code.append(binOpOllirGenerator.getCode());

        // if it returned an expression and not

        return "new(array, " + arrayLength + ").array.i32";
    }


    private String visitMethodBody(JmmNode jmmNode, Object integer) {

        this.methodName = jmmNode.getJmmParent().get("name");
        // Should we start the parse here?

        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, null); // TODO: should we use code.append here ?
        }

        return "";
    }

    private String visitDefault(JmmNode jmmNode, Object integer) {
        System.out.println("Not implemented: " + jmmNode.getKind());
        return "";
    }

    private String visitArrayAccess(JmmNode jmmNode, Object integer) {
        return "";
    }



    private String visitMethodCall(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitWhile(JmmNode jmmNode, Object integer) {
        return "";
    }

    private String visitIf(JmmNode jmmNode, Integer integer) {
        return "";
    }

    private String visitAssign(JmmNode assignNode, Object integer) {
        System.out.println("Visiting assign");

        /*
        In the compiler you are expected to develop, method invocation from imported classes can only be used in statements with direct assignment (e.g.,a = M.f();, a=m1.g();)
         or as simple call statements, i.e.,without assignment (e.g., M.f2(); , m1.g();).
        Calls to methods declared inside the class can only appear in compounded operation (e.g, a = b * this.m(10,20), where "m” is declared inside the class).
        */

        // methodCall/DotExpression (length, this, or call)
        // arrayAccess in left side
        // a field (getFields method) both sides
        // ! operator (not)


        String variableName = assignNode.get("name");
        Symbol variableSymbol = ((ProgramSymbolTable) symbolTable).getLocalVariable(this.methodName, variableName);

        String variable = OllirUtils.getIdCode(variableName, (ProgramSymbolTable) symbolTable, this.methodName);

        StatementOllirGenerator binOpOllirGenerator = new StatementOllirGenerator((ProgramSymbolTable) symbolTable,this.methodName);

        JmmNode assignedNode = assignNode.getJmmChild(0);

        if(assignedNode.getKind().equals("ArrayCreation")) {
            String instruction = visit(assignedNode, null);
            assignmentGenerator(variable, assignedNode, instruction);
        }
        else if(assignedNode.getKind().equals("ClassCreation")) {
            String instruction = visit(assignedNode, null);
            assignmentGenerator(variable, assignedNode, instruction);
            String className =  assignedNode.get("name");
            code.append("invokespecial(" + variable + ", \"<init>\").V;\n");
        }


        // BinOp takes care of a BinOp, a literal, an ID (unless it's a this.)
        // Should it? It has the necessary visitors for its own operations, but should it take care of Direct access nodes?


        // Should it handle a method call YES PLEASE int[] C = new int[A.length <- This is a BINOP? NO, BUT SHOULD ];
        // Should it handle an array access
        // Should it handle an array creation NO I GUESS
        // Should it handle a class creation NO I GUESS
        // and WTF happens in a ! operator?

        // TODO: separate this in a AssignmentVisitor, and a BinOpVisitor or at least another visit,
        // Should we go back and have an Expression Node and an Expression Visitor?
        if(assignedNode.getKind().equals("BinOp") || assignedNode.getKind().equals("IntegerLiteral") || assignedNode.getKind().equals("ID")) {
            String instruction = binOpOllirGenerator.visit(assignedNode, null);
            System.out.println("Instruction: " + instruction);
            code.append(binOpOllirGenerator.getCode());
            assignmentGenerator(variable, assignedNode, instruction);
        }

        return "";
    }

    private void assignmentGenerator(String variableCode, JmmNode assignedNode, String instruction) {
        code.append(variableCode).append(" ").
                append(OllirUtils.getBinOpAssignCode(assignedNode, (ProgramSymbolTable) this.symbolTable, this.methodName)).
                append(" ").append(instruction).append(";").append("\n");
    }

    private String visitVarDecl(JmmNode jmmNode, Integer integer) {
        return ""; //TODO: Ignore and place in default visitor
    }

    private String visitType(JmmNode jmmNode, Integer integer) {
        return "";//TODO: Ignore and place in default visitor
    }

    public String getCode() {
        return code.toString();
    }
}
