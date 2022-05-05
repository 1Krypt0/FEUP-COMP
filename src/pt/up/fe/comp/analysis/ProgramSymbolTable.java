package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class ProgramSymbolTable implements SymbolTable {


    private final List<String> imports;
    private  String className;
    private  String superClass;
    private final List<String> methods;
    private final Map<String, Type> methodReturnTypes;
    private final Map<String, List<Symbol>> methodParameters;


    public ProgramSymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.methods = new ArrayList<>();
        this.methodReturnTypes = new HashMap<>();
        this.methodParameters = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.methodReturnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return this.methodParameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return Collections.emptyList();
    }


    public void addImport(String importName) {
        this.imports.add(importName);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }
}
