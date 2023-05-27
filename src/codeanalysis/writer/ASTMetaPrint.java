package org.polymtl.codeanalysis.writer;

import org.json.JSONArray;
import org.polymtl.codeanalysis.model.AST;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ASTMetaPrint implements Printer {
    private PrintWriter writer = null;
    private FileReader reader = null;
    private AST ast = null;
    static final String[] types = {
            "AbstractMethodStatement",
            "AbstractPublicMethodStatement",
            "AbstractProtectedMethodStatement",
            "AbstractPrivateMethodStatement",
            "ArgumentList",
            "Argument",
            "Array",
            "Arobas",
            "ArrayInitialisation",
            "ArrayExpression",
            "BinOP",
            "Block",
            "Bool",
            "Boolean",
            "Break",
            "Case",
            "CaseList",
            "CaseCondition",
            "CastExpression",
            "Catch",
            "ClassAccess",
            "ClassInstanciation",
            "ClassName",
            "ClassStatement",
            "Clone",
            "LambdaFunctionStatement",
            "Condition",
            "ConditionalExpression",
            "ConditionalFalse",
            "ConditionalTrue",
            "ConstMemberDeclaration",
            "Continue",
            "DeclareStatement",
            "Default",
            "Double",
            "DoubleLiteral",
            "DoWhile",
            "EchoStatement",
            "EId",
            "ElseIf",
            "ElseIfList",
            "ExecString",
            "ExpressionStatement",
            "False",
            "Float",
            "For",
            "ForEach",
            "FunctionCall",
            "FunctionStatement",
            "Global",
            "HeredocFlow",
            "HexLiteral",
            "Html",
            "Id",
            "IfThenElifElseStatement",
            "IfThenElifStatement",
            "IfThenElseStatement",
            "IfThenStatement",
            "Implements",
            "IncludeStatement",
            "IncludeOnceStatement",
            "Increment",
            "Init",
            "Int",
            "Integer",
            "IntegerLiteral",
            "InterfaceStatement",
            "LegalChar",
            "LogicOP",
            "MemberDeclaration",
            "MethodCall",
            "MethodStatement",
            "NamespaceName",
            "NamespaceStatement",
            "New",
            "Null",
            "Object",
            "OptReferenceParameter",
            "OptTypedReferenceParameter",
            "OptTypedValueParameter",
            "OptValueParameter",
            "ParameterList",
            "ParentClassName",
            "PostfixExpression",
            "PostIncrement",
            "PreIncrement",
            "PrintStatement",
            "Private",
            "PrivateMemberDeclaration",
            "PrivateMethodStatement",
            "Protected",
            "ProtectedMemberDeclaration",
            "ProtectedMethodStatement",
            "Public",
            "PublicMemberDeclaration",
            "PublicMethodStatement",
            "ReferenceParameter",
            "RelOP",
            "RequireStatement",
            "RequireOnceStatement",
            "Return",
            "ReturnReferenceFunction",
            "ReturnReferenceMethod",
            "ReturnValueFunction",
            "ReturnValueMethod",
            "Start",
            "StatementBody",
            "Static",
            "String",
            "StringExpression",
            "StringLiteral",
            "Switch",
            "Throw",
            "TraitStatement",
            "True",
            "TryCatch",
            "TypedReferenceParameter",
            "TypedValueParameter",
            "UnaryOP",
            "UnsetStatement",
            "UseInsteadOf",
            "UseStatement",
            "UseTraitDeclaration",
            "ValueParameter",
            "Variable",
            "VariableExpression",
            "VariableStatement",
            "While"
    };


    public ASTMetaPrint(AST ast, File fOut) throws FileNotFoundException {
        writer = new PrintWriter(fOut);
        this.ast = ast;
    }

    private void print(Object... args) {
        JSONArray array = new JSONArray();
        for (Object arg : args) {
            array.put(arg);
        }
        writer.write("  "+(new JSONArray(array)).toString()+",\n");
    }

    private void print(JSONArray args) {
        writer.write("  "+args.toString()+",\n");
    }

    @Override
    public void print() throws Exception {
        writer.write("[\n");
        visit();
        writer.write("]\n");
        writer.flush();
        writer.close();
    }

    public void visit() throws Exception {
        /*JSONTokener jsonparser;
        JSONArray array;

        BufferedReader reader = new BufferedReader(this.reader);
        String line = null;
        line = reader.readLine();
        while (line != null ) {
            if (line.equals("[")) {
                line = reader.readLine();
                continue;
            }
            if (line.equals("]"))
                break;

            jsonparser = new JSONTokener(line);
            array = new JSONArray(jsonparser);
            print(array);
            line = reader.readLine();
        }*/


        List<Integer> list = new ArrayList<>(Collections.nCopies(types.length, 0));
        List<String> types_list = Arrays.asList(types);
        for(Integer key : ast.getNodeIds()) {
            int i = types_list.indexOf(ast.getType(key));
            if(i == -1)
                throw new Exception("META Writer: Type " + ast.getType(key) + " not found.");
            list.set(i, list.get(i) + 1);
        }
        StringBuilder list_str = new StringBuilder();
        for(int i = 0 ; i < list.size() ; i++) {
            list_str.append(list.get(i));
            if(i<list.size()-1)
                list_str.append(",");
        }
        print("vector_count_token_type_ast", list_str);

    }

}
