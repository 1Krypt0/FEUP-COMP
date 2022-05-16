package pt.up.fe.comp.ollir;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

/*
    Some examples:
    this.testFoo2(1, 1 + c).testFoo3(a);
    b.length;
    io.println(c, d);
 */



public class DotLinkedOllirGenerator extends AJmmVisitor<Object, String> {

    final StringBuilder code;
    final ProgramSymbolTable table;
    final String methodName;

    public DotLinkedOllirGenerator(ProgramSymbolTable symbolTable, String methodName) {
        this.table = symbolTable;
        this.code = new StringBuilder();
        this.methodName = methodName;

        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.Method_Call, this::visitMethodCall);
        addVisit(AstNode.This, this::visitThis);
        addVisit(AstNode.Length, this::visitLength);

        setDefaultVisit(this::defaultVisit);

    }

    private String visitDotLinked(JmmNode jmmNode, Object hasObject) {

        // TODO: the inside of invoke changes but could be parsed separately

        JmmNode caller = jmmNode.getJmmChild(0);
        JmmNode callee = jmmNode.getJmmChild(1);

        String returnTypeCode = "";
        String args = "";
        if (callee.getKind().equals("MethodCall")) {
            String methodName = callee.get("name");
            Type returnType = table.getReturnType(methodName);

            ArgumentsOllirGenerator argumentsOllirGenerator = new ArgumentsOllirGenerator(table, this.methodName);
            args = argumentsOllirGenerator.visit(callee, 1);

            code.append(argumentsOllirGenerator.getCode());

            if(returnType != null) {
                returnTypeCode = "." + OllirUtils.getCode(returnType);
            }
            else{
                returnTypeCode = ".V";
            }
        }
        else if (callee.getKind().equals("Length")) {
            // TODO: length
        }

        if (caller.getKind().equals("This")) {
            // visit args with Visitor and build the string and auxiliary code
            return "invokevirtual(this, \"" + methodName + "\"" + args + ")" +  returnTypeCode  +  ";\n";

        } else if (caller.getKind().equals("ID")) {
            // Find in symbol table the id
            // Could be a field, a new Class or an import
            // how to parse multiple imports
            String name = caller.get("name");
            // Is this valid if import is package.a.b.c, and a int a exists?
            // How can we concatenate the dotLink of Ids up?
            boolean isImport = table.isSubStringOfAnImport(name);
            boolean isLocalVariableClassType = table.isLocalVariableObjectClassType(name, this.methodName);
            boolean isField = table.isField(name);


            // cascade id

            if (isImport) {
                // static
                return "invokestatic(" + name +  ", \"" + methodName + "\"" + args + ")" + returnTypeCode + ";" + "\n"; // always void
            } else if (isLocalVariableClassType) {
                // virtual with variable name
            } else if (isField) {
                // check type
            }
        } else if (caller.getKind().equals("DotLinked")) {
            visit(caller, null);
        }

        return "";
    }

    private String visitLength(JmmNode jmmNode, Object o) {
        return "";
    }

    private String visitThis(JmmNode jmmNode, Object o) {
        return "";
    }

    private String visitMethodCall(JmmNode jmmNode, Object o) {
        return "";
    }

    private String visitId(JmmNode jmmNode, Object o) {
        return "";
    }

    public String defaultVisit(JmmNode jmmNode, Object o) {
        System.out.println("Not implemented: " + jmmNode.getKind());

        return "";
    }

    public String getCode() {
        return code.toString();
    }
}
