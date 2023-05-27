/*
 * Copyright (C) 2021, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 * @author Stéphane Heudron <stephane.heudron@polymtl.ca>
 */

package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.model.AST;
import org.polymtl.codeanalysis.model.GraphIndexedVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class ASTVariableIndexer {
    private static class VariableIndex {
        protected String name;
        protected Integer id;
        protected Integer scope;
        VariableIndex(String name, Integer id, Integer scope){
            this.name = name;
            this.id = id;
            this.scope = scope;
        }
        public Integer getId() {
            return id;
        }
        public Integer getScope() {
            return scope;
        }
        public String getName() {
            return name;
        }
    }

    public static final List<String> BLOCK_TYPES = Arrays.asList(
            "ClassStatement",
            "FunctionStatement",
            "MethodStatement",
            "PublicMethodStatement",
            "PrivateMethodStatement",
            "ProtectedMethodStatement",
            "AbstractMethodStatement",
            "AbstractPublicMethodStatement",
            "AbstractProtectedMethodStatement",
            "AbstractPrivateMethodStatement",
            "LambdaFunctionStatement"
            /*"Block",
            "CompoundStatement",
            "Catch"*/
    );

    public static final List<String> VARNAME_TYPES = Arrays.asList(
            "Variable"
    );

    private static Integer current_index_scope      = 0;
    private static Integer current_index_variable   = 0;
    private static Stack<VariableIndex> global_stack = new Stack<>();
    public static void resetIndex() {
        current_index_variable = 0;
        current_index_scope = 0;
        global_stack = new Stack<>();
    }

    private List<Integer> visited;
    public ASTVariableIndexer(AST astTable) {
        visited = new ArrayList<>();
        visit(astTable, astTable.getRoot(), 0, global_stack);
    }

    public List<String> getVarTypes() {
        return VARNAME_TYPES;
    }


    private void visit(GraphIndexedVariable astTable, Integer node, Integer scope, Stack<VariableIndex> varStack) {
        if(visited.contains(node))
            return;
        visited.add(node);
        String type = astTable.getType(node);
        List<Integer> children = astTable.getChildren(node);
        if (BLOCK_TYPES.contains(type)) {
            Integer new_scope = ++current_index_scope;
            astTable.setScopeId(node, new_scope);
            if (children != null) {
                for (Integer child : children)
                    visit(astTable, child, new_scope, varStack);
            }
            while (varStack.size() > 0 && varStack.peek().getScope().equals(new_scope))
                varStack.pop();
            return;
        }
        else if (VARNAME_TYPES.contains(type)) {
            String varName = astTable.getImage(node);
            boolean exist = false;
            for(VariableIndex var : varStack) {
                if (var.getName().equals(varName)) {
                    exist = true;
                    astTable.setVarId(node, var.getId());
                    astTable.setVarScope(node, var.getScope());
                    break;
                }
            }
            if(!exist) {
                Integer id = current_index_variable++;
                varStack.push(new VariableIndex(varName, id, scope));
                astTable.setVarId(node, id);
                astTable.setVarScope(node, scope);
            }
        }

        if (children != null)
            for (Integer child : children)
                visit(astTable, child, scope, varStack);

    }
}
