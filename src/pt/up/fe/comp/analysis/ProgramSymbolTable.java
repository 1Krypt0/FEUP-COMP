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
    private final List<Symbol> fields;
    private final Map<String, List<Symbol>> localVariables;

    public ProgramSymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.methods = new ArrayList<>();
        this.methodReturnTypes = new HashMap<>();
        this.methodParameters = new HashMap<>();
        this.fields = new ArrayList<>();
        this.localVariables = new HashMap<>();
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
        return fields;
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
        return this.localVariables.get(methodSignature);
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

    public void addMethod(String methodSignature, Type returnType, List<Symbol> parameters) {
        this.methods.add(methodSignature);
        this.methodReturnTypes.put(methodSignature, returnType);
        this.methodParameters.put(methodSignature, parameters);
    }

    public boolean hasMethod(String methodSignature) {
        return this.methods.contains(methodSignature);
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }

    public boolean hasField(String fieldName) {
        return this.fields.stream().anyMatch(f -> f.getName().equals(fieldName));
    }

    public void addLocalVariable(String methodSignature, Symbol localVariable) {
        if (!this.localVariables.containsKey(methodSignature)) {
            this.localVariables.put(methodSignature, new ArrayList<>());
        }
        this.localVariables.get(methodSignature).add(localVariable);
    }
    public boolean hasLocalVariable(String methodSignature, String localVariableName) {
        return this.localVariables.get(methodSignature).stream().anyMatch(f -> f.getName().equals(localVariableName));
    }

    public void addLocalVariables(String methodSignature, List<Symbol> localVariables) {
        if (!this.localVariables.containsKey(methodSignature)) {
            this.localVariables.put(methodSignature, new ArrayList<>());
        }
        this.localVariables.get(methodSignature).addAll(localVariables);
    }


    public Type getLocalVariableType(String methodName, String localVariableName) {
        for (Symbol localVariable : this.localVariables.get(methodName)) {
            if (localVariable.getName().equals(localVariableName)) {
                return localVariable.getType();
            }
        }
        return null;
    }

    public Symbol getLocalVariable(String methodName, String localVariableName) {
        for (Symbol localVariable : this.localVariables.get(methodName)) {
            if (localVariable.getName().equals(localVariableName)) {
                return localVariable;
            }
        }
        return null;
    }

    public Type getVariableType(String variableName) {
        for (Symbol field : this.fields) {
            if (field.getName().equals(variableName)) {
                return field.getType();
            }
        }
        for (String methodName : this.methods) {
            for (Symbol localVariable : this.localVariables.get(methodName)) {
                if (localVariable.getName().equals(variableName)) {
                    return localVariable.getType();
                }
            }
        }
        return null;
    }

    public String getVariable() {
        return this.className;
    }

}
