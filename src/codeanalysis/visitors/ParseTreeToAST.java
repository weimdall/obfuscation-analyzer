package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.exceptions.ASTJsonException;
import org.polymtl.codeanalysis.model.AST;
import org.polymtl.codeanalysis.model.ParseTree;
import org.polymtl.codeanalysis.model.defsInt;
import org.polymtl.codeanalysis.util.*;

import java.util.*;

public class ParseTreeToAST {
    private ParseTree ptTable = null;
    private AST astTable = null;
    protected static int iNextNode = 0;
    protected static int iNextLambda = 0;
    protected ASTNavigatorCl<Integer, String> ptnavigator;

    public void resetIdsGen() {
        ASTVariableIndexer.resetIndex();
        iNextNode = 0;
    }

    public ParseTreeToAST(ParseTree parAstTable) {
        astTable = new AST();
        ptTable = parAstTable;
        astTable.setRoot(parAstTable.getRoot());
        astTable.setFilename(parAstTable.getFilename());
        ptnavigator = new ASTNavigatorCl<>(parAstTable.getSuccTable(), parAstTable.getTypeTable());
    }


    private void transferNodeType(Integer parseNodeId, Integer astNodeId) {
        String curType = ptTable.getType(parseNodeId);
        if (curType == null)
            throw new ASTJsonException("Missing node type for node " + parseNodeId);

        String oldVal = astTable.setNodeType(astNodeId, curType);
        if (oldVal != null) {
            throw new ASTJsonException("ERROR: invalid multiple node type " +
                    curType + " for node " + astNodeId + " (old val: " + oldVal + "- new value: "+curType+")");
        }
    }

    private void transferNodePositions(Integer parseNodeId, Integer astNodeId) {
        Integer[] pos = ptTable.getPositions(parseNodeId);
        if (pos == null)
            return;
        Integer[] oldVal = astTable.setNodePosition(astNodeId, pos);
        if (oldVal != null) {
            throw new ASTJsonException("ERROR: invalid multiple node pos " +
                    Arrays.toString(pos) + " for node " + astNodeId + " (old val: " + Arrays.toString(oldVal) + ")");
        }
    }

    private void transferNodeToken(Integer parseNodeId, Integer astNodeId) {
        String curToken = ptTable.getImage(parseNodeId);
        if (curToken != null) {
            String oldVal = astTable.setNodeImage(astNodeId, curToken);
            if (oldVal != null) {
                throw new ASTJsonException("ERROR: invalid multiple node token " +
                        curToken + " for node " + astNodeId + " (old val: " + oldVal + ")");
            }
        }
    }



    private int getNewNode() {
        return ++iNextNode;
    }

    private List<Integer> visitChildren(Integer ptNodeId) {
        List<Integer> children = ptTable.getChildren(ptNodeId);
        List<Integer> activeChildren = new ArrayList<Integer>();
        if (children != null) {
            for (Integer mChild : children) {
                visit(mChild);
                Integer childNodePtr = astTable.getNodeASTPtr(mChild);
                if (childNodePtr != defsInt.UNDEF_VAL) {
                    activeChildren.add(childNodePtr);
                }
            }
        }
        return activeChildren;
    }

    //
    // visit routines
    //


    private Integer visit_GENERIC(Integer ptNodeId) {
        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodeType(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);
        return astNodeId;
    }
    private Integer visit_GENERIC_rename(Integer ptNodeId, String type) {
        Integer astNodeId = visit_GENERIC(ptNodeId);
        astTable.setNodeType(astNodeId, type);
        return astNodeId;
    }

    private Integer visit_compress_GENERIC(Integer ptNodeId) {

        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren <= 0)
            return null;

        List<Integer> activeChildren = visitChildren(ptNodeId);
        int nActiveChildren = activeChildren.size();

        if (nActiveChildren == 0) {
            return null;
        } else if (nActiveChildren == 1) {
            // compress path
            astTable.setNodePtr(ptNodeId, activeChildren.get(0));
            return activeChildren.get(0);
        } else {
            // keep ast node with active children
            Integer astNodeId = getNewNode();
            for (Integer aChild : activeChildren) {
                astTable.addEdge(astNodeId, aChild);
            }

            astTable.setNodePtr(ptNodeId, astNodeId);
            transferNodeType(ptNodeId, astNodeId);
            transferNodePositions(ptNodeId, astNodeId);
            transferNodeToken(ptNodeId, astNodeId);

            return astNodeId;
        }
    }

    private Integer visit_VARIABLE(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren != 2)
            throw new ASTJsonException("Variable node " + ptNodeId + " has " + nChildren + " children (!=2??)");

        Integer astNodeId;
        if (ptnavigator.matchChildrenTypes(ptNodeId, Arrays.asList( "Dollar", "VarName" ))) {
            //throw new ASTJsonException("Variable node " + ptNodeId + " should have two children of types Id, VarName");
            astNodeId = getNewNode();
            astTable.setNodePtr(ptNodeId, astNodeId);
            transferNodeType(ptNodeId, astNodeId);
            transferNodePositions(ptnavigator.getChild(ptNodeId, 1), astNodeId);
            transferNodeToken(ptnavigator.getChild(ptNodeId, 1), astNodeId);
        } else {
            astNodeId = visit_GENERIC_rename(ptNodeId, "VariableExpression");
        }

        return astNodeId;
    }

    private Integer visit_compress_FOR(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (!ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "ForPredicate"))
            throw new ASTJsonException("For Statement node " + ptNodeId + " should have a child of type ForPredicate");
        List<Integer> activeChildren = visitChildren(ptNodeId);
        if(activeChildren.size() <= 0)
            throw new ASTJsonException("All the branch of a FOR statement has been pruned : node " + ptNodeId);

        Integer astNodeId = getNewNode();
        Integer body = getNewNode();
        astTable.setNodeType(body, "StatementBody");

        List<Integer> forPredicateChildren = astTable.getChildren(activeChildren.get(0));

        astTable.addEdge(astNodeId, forPredicateChildren.get(0));
        astTable.addEdge(astNodeId, forPredicateChildren.get(1));
        astTable.addEdge(astNodeId, forPredicateChildren.get(2));
        astTable.addEdge(astNodeId, body);
        if(activeChildren.size() != 1)
            astTable.addEdge(body, activeChildren.get(1));

        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, "For");
        transferNodePositions(ptNodeId, astNodeId);
        astTable.deleteNode(activeChildren.get(0)); // Delete ForPredicate (shortcut)

        return astNodeId;
    }

    private Integer visit_compress_FORINCREMENTINIT(Integer ptNodeId) {
        String newType = null;
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        switch (ptTable.getType(ptNodeId)) {
            case "ForInit":
                newType = "Init";
                break;
            case "ForIncrement":
                newType = "Increment";
                break;
            default:
                newType = ptTable.getType(ptNodeId);
        }

        if( !ptnavigator.matchChildrenTypes(ptNodeId, Arrays.asList( "ArgumentList" )) )
            throw new ASTJsonException(ptTable.getType(ptNodeId) + " node " + ptNodeId + " should have one child of type ArgumentList");

        List<Integer> activeChildren = visitChildren(ptNodeId);
        Integer astNodeId = getNewNode();
        if(nChildren >= 1) {
            List<Integer> children = astTable.getChildren(activeChildren.get(0));
            if(children != null)
                for (Integer aChild : children)
                    astTable.addEdge(astNodeId, aChild);
            astTable.deleteNode(activeChildren.get(0));
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, newType);

        return astNodeId;
    }

    private Integer visit_compress_FOREACH(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if(ptnavigator.match( ptNodeId, "EndForeach", Arrays.asList( nChildren-1 )))
            nChildren -= 1;
        if (!ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "ForEachPredicate"))
            throw new ASTJsonException("ForEach Statement node " + ptNodeId + " should have a child of type ForEachPredicate");
        List<Integer> activeChildren = visitChildren(ptNodeId);
        if(activeChildren.size() != nChildren)
            throw new ASTJsonException("An entire branch of a FOREACH statement has been pruned : node " + ptNodeId);

        Integer astNodeId = getNewNode();
        Integer body = getNewNode();
        astTable.setNodeType(body, "StatementBody");

        List<Integer> predChildren = astTable.getChildren(activeChildren.get(0));
        astTable.addEdge(astNodeId, predChildren.get(0));
        astTable.addEdge(astNodeId, predChildren.get(1));
        if(predChildren.size() > 2)
            astTable.addEdge(astNodeId, predChildren.get(2));
        astTable.addEdge(astNodeId, body);
        astTable.addEdge(body, activeChildren.get(1));

        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, "ForEach");
        transferNodePositions(ptNodeId, astNodeId);
        astTable.deleteNode(activeChildren.get(0));

        return astNodeId;
    }

    private Integer visit_compress_SWITCHSTATEMENT(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (!ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Condition"))
            throw new ASTJsonException("SwitchStatement node " + ptNodeId + " should have a child of type Condition");

        Integer switchNode = getNewNode();
        Integer caseListNode = getNewNode();

        astTable.setNodeType(caseListNode, "CaseList");

        astTable.setNodePtr(ptNodeId, switchNode);
        astTable.setNodeType(switchNode, "Switch");
        transferNodePositions(ptNodeId, switchNode);
        transferNodeToken(ptNodeId, switchNode);

        List<Integer> activeChildren = visitChildren(ptNodeId);
        if(activeChildren.size() <= 0)
            throw new ASTJsonException("All the branch of a SwitchStatement statement has been pruned : node " + ptNodeId);

        Integer conditionNode = activeChildren.get(0);
        astTable.addEdge(switchNode,astTable.getChildren(conditionNode).get(0));
        astTable.addEdge(switchNode, caseListNode);

        for(int i = 1; i < activeChildren.size(); i++) {
            astTable.addEdge(caseListNode, activeChildren.get(i));
        }

        astTable.deleteNode(conditionNode);
        return switchNode;
    }

    private Integer visit_compress_CONDITIONAL(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if(ptnavigator.match( ptNodeId, "EndIf", Arrays.asList( nChildren-1 )))
            nChildren -= 1;
        if (nChildren <= 0)
            throw new ASTJsonException("Conditional Statement node " + ptNodeId + " needs at least one child");
        if(!ptnavigator.match( ptNodeId, "IfStatement", Arrays.asList( 1 ) ) ||
           !ptnavigator.match( ptNodeId, "Condition", Arrays.asList( 0 )))
            throw new ASTJsonException("First child of Conditional Statement node " + ptNodeId + " should be a ifStatement with a condition");

        List<Integer> activeChildren = visitChildren(ptNodeId);

        Integer ifThenNode = activeChildren.get(1);
        astTable.setNodePtr(ptNodeId, ifThenNode);
        transferNodePositions(ptNodeId, ifThenNode);

        if(activeChildren.size() != nChildren)
            throw new ASTJsonException("An entire branch of a conditional has been pruned : node " + ptNodeId);

        if(nChildren == 2) { //IFTHEN
            astTable.setNodeType(ifThenNode, "IfThenStatement");
        }
        else if(nChildren == 3 && ptnavigator.match(ptNodeId, "ElseStatement", Arrays.asList(2))) { //IFTHENELSE
            astTable.setNodeType(ifThenNode, "IfThenElseStatement");
            astTable.addEdge(ifThenNode, astTable.getChildren(activeChildren.get(2)).get(0));// Shortcut else
            astTable.deleteNode(activeChildren.get(2));// Delete Else
        }
        else { // IFTHENELIF
            boolean elsePresence = ptnavigator.match(ptNodeId, "ElseStatement", Arrays.asList(nChildren-1));
            int elseList = getNewNode();
            astTable.setNodeType(elseList, "ElseIfList");
            astTable.setNodeType(ifThenNode, elsePresence ? "IfThenElifElseStatement" : "IfThenElifStatement");
            astTable.addEdge(ifThenNode, elseList);
            for(int i = 2; i < nChildren; i++) {
                if(i == nChildren - 1 && elsePresence) {
                    astTable.addEdge(ifThenNode, astTable.getChildren(activeChildren.get(i)).get(0)); // Shortcut else
                    astTable.deleteNode(activeChildren.get(i)); // Delete Else
                }
                else
                    astTable.addEdge(elseList, activeChildren.get(i));
            }
        }

        return ifThenNode;
    }

    private Integer visit_compress_IFELSEWHILESTATEMENT(Integer ptNodeId) {
        boolean doWhile = ptnavigator.match(ptNodeId, "DoWhileStatement");
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if(ptnavigator.match( ptNodeId, "EndWhile", Arrays.asList( nChildren-1 )))
            nChildren -= 1;
        if (nChildren <= 0)
            throw new ASTJsonException(ptTable.getType(ptNodeId) + " node " + ptNodeId + " needs 1 or 2 children (got " + nChildren + ")");
        List<Integer> activeChildren = visitChildren(ptNodeId);
        if((!doWhile && activeChildren.size() != nChildren) || (doWhile && activeChildren.size() != nChildren-1 ) )
            System.err.println("WARN: An entire branch of a "+ ptTable.getType(ptNodeId) +" statement has been pruned : node " + ptNodeId);

        Integer astNodeId = getNewNode();
        Integer body = getNewNode();
        astTable.setNodeType(body, "StatementBody");
        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        switch (ptTable.getType(ptNodeId)) {
            case "IfStatement":
                astTable.setNodeType(astNodeId, "If");
                Integer PTleftBrother = ptTable.getLeftBrother(ptNodeId);
                astTable.addEdge(astNodeId, astTable.getNodeASTPtr(PTleftBrother)); // Get first parent then first child -> Condition
                break;
            case "ElseifStatement":
                astTable.setNodeType(astNodeId, "ElseIf");
                break;
            case "ElseStatement":
                astTable.setNodeType(astNodeId, "Else");
                break;
            case "WhileStatement":
                astTable.setNodeType(astNodeId, "While");
                break;
            case "DoWhileStatement":
                astTable.setNodeType(astNodeId, "DoWhile");
                break;
            default:
                transferNodeType(ptNodeId, astNodeId);
        }


        if(activeChildren.size() == 0) { // Empty IF
            astTable.addEdge(astNodeId, body);
            return astNodeId;
        }

        Integer condition = null;
        Integer childBody = activeChildren.get(0);
        if (activeChildren.size() == 2) {
            if (doWhile) {
                condition = activeChildren.get(1);
                childBody = activeChildren.get(0);
            } else {
                condition = activeChildren.get(0);
                childBody = activeChildren.get(1);
            }
            astTable.addEdge(body, childBody);
            astTable.addEdge(astNodeId, condition);
        }
        else
            astTable.addEdge(body, childBody);

        astTable.addEdge(astNodeId, body);

        return astNodeId;
    }

    private Integer visit_compress_SWITCHDEFAULT(Integer ptNodeId) {
        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        Integer idBody = getNewNode();
        Integer idDefault = getNewNode();

        astTable.addEdge(astNodeId, idDefault);
        astTable.addEdge(astNodeId, idBody);

        astTable.setNodeType(idDefault, "Default");
        astTable.setNodeType(astNodeId, "Case");
        astTable.setNodeType(idBody, "Block");
        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);

        if(activeChildren.size() > 0) {
            for (Integer aChild : activeChildren) {
                astTable.addEdge(idBody, aChild);
            }
        }

        return astNodeId;
    }

    private Integer visit_compress_CASE(Integer ptNodeId) {
        if (!ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Condition"))
            throw new ASTJsonException("Case node " + ptNodeId + " should have a child of type Condition");
        List<Integer> activeChildren = visitChildren(ptNodeId);
        // keep ast node with or without active children
        Integer caseNodeId = getNewNode();
        Integer statementBodyId = getNewNode();

        astTable.setNodeType(statementBodyId, "Block");

        astTable.setNodePtr(ptNodeId, caseNodeId);
        astTable.setNodeType(caseNodeId, "Case");
        transferNodePositions(ptNodeId, caseNodeId);
        transferNodeToken(ptNodeId, caseNodeId);

        Integer conditionNode = activeChildren.get(0);
        astTable.addEdge(caseNodeId, astTable.getChildren(conditionNode).get(0));
        astTable.addEdge(caseNodeId, statementBodyId);
        astTable.deleteNode(conditionNode);

        if(activeChildren.size() > 0) {
            for (int i = 1; i < activeChildren.size(); i++) {
                astTable.addEdge(statementBodyId, activeChildren.get(i));
            }
        }

        return caseNodeId;
    }

    private Integer visit_compress_ternary(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren <= 0)
            return null;

        List<Integer> activeChildren = visitChildren(ptNodeId);
        if (activeChildren.size() != 3)
            throw new ASTJsonException("Ternary " + ptTable.getType(ptNodeId) + "("+ptNodeId+") should have 3 children (found " + activeChildren.size() + ")");

        astTable.setNodePtr(ptNodeId, activeChildren.get(1));
        astTable.addEdge(activeChildren.get(1), activeChildren.get(0));
        astTable.addEdge(activeChildren.get(1), activeChildren.get(2));
        return activeChildren.get(1);
    }

    private Integer visit_compress_operator(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren != 0)
            throw new ASTJsonException("Operator " + ptTable.getType(ptNodeId) + " (" + ptNodeId + ") should not have any child (found " + nChildren + ")");

        Integer astNodeId = getNewNode();
        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);

        return astNodeId;
    }

    private Integer visit_compress_RelOP(Integer ptNodeId) {
        Integer astNodeId = visit_compress_operator(ptNodeId);
        astTable.setNodeType(astNodeId, "RelOP");
        return astNodeId;
    }
    private Integer visit_compress_LogicOP(Integer ptNodeId) {
        Integer astNodeId = visit_compress_operator(ptNodeId);
        astTable.setNodeType(astNodeId, "LogicOP");
        return astNodeId;
    }
    private Integer visit_compress_BinOP(Integer ptNodeId) {
        //todo: Add BinaryAnd test !
        if(Arrays.asList("Parameter", "MemberDeclaration", "VariableStatement", "ReturnReferenceFunction", "ReturnReferenceMethod").contains(ptTable.getType(ptTable.getParent(ptNodeId).get(0))))
            return null;
        Integer astNodeId = visit_compress_operator(ptNodeId);
        astTable.setNodeType(astNodeId, "BinOP");
        return astNodeId;
    }
    private Integer visit_compress_CONST(Integer ptNodeId) {
        if(ptnavigator.match(ptTable.getParent(ptNodeId).get(0), "MemberDeclaration"))
            return null;
        return visit_GENERIC(ptNodeId);
    }
    private Integer visit_compress_INCREMENT(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren != 2)
            throw new ASTJsonException("Operator " + ptTable.getType(ptNodeId) + " (" + ptNodeId + ") should have 2 children (found " + nChildren + ")");
        List<Integer> activeChildren = visitChildren(ptNodeId);
        if (activeChildren.size() != 1)
            throw new ASTJsonException("Operator " + ptTable.getType(ptNodeId) + " (" + ptNodeId + ") should have one active child (found " + activeChildren.size() + ")");

        boolean postfix = ptnavigator.match(ptNodeId, "PostfixIncrement") | ptnavigator.match(ptNodeId, "PostfixDecrement");
        Integer astNodeId = getNewNode();
        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, ptTable.getType(ptNodeId).replace("fix", ""));
        if (postfix) {
            transferNodePositions(ptnavigator.getChild(ptNodeId, 1), astNodeId);
            transferNodeToken(ptnavigator.getChild(ptNodeId, 1), astNodeId);
        }
        else {
            transferNodePositions(ptnavigator.getChild(ptNodeId, 0), astNodeId);
            transferNodeToken(ptnavigator.getChild(ptNodeId, 0), astNodeId);
        }
        astTable.addEdge(astNodeId, activeChildren.get(0));
        return astNodeId;
    }


    private Integer visit_compress_COMPOUND(Integer ptNodeId) {
        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, "Block");
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);

        return astNodeId;
    }

    private Integer visit_compress_POSTFIX(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren <= 0)
            return null;

        List<String> types = Arrays.asList("FunctionCall", "MethodCall", "BinOP", "ArrayExpression");

        List<Integer> activeChildren = visitChildren(ptNodeId);
        int nActiveChildren = activeChildren.size();

        if (nActiveChildren == 0) {
            return null;
        } else if ( nActiveChildren == 1 ) {
            astTable.setNodePtr(ptNodeId, activeChildren.get(0));
        } else if(nActiveChildren ==  2 && types.contains(ptTable.getType(ptnavigator.getChild(ptNodeId, 1)))) {
            astTable.setNodePtr(ptNodeId, activeChildren.get(1));
        } else {
            boolean collapse = true;
            for(int i = 1 ; i < activeChildren.size() ; i++) {
                if(!types.contains(astTable.getType(activeChildren.get(i)))) {
                    collapse = false;
                    break;
                }
            }
            if(collapse) { // A chain of function call for instance
                astTable.setNodePtr(ptNodeId, activeChildren.get(activeChildren.size()-1));
                return null;
            }

            Integer astNodeId = getNewNode();
            for(int i = 0 ; i < activeChildren.size() ; i++) {
                if(i+1 < activeChildren.size() && types.contains(astTable.getType(activeChildren.get(i+1)))) {
                    continue;
                }
                astTable.addEdge(astNodeId, activeChildren.get(i));
            }

            astTable.setNodePtr(ptNodeId, astNodeId);
            transferNodeType(ptNodeId, astNodeId);
            transferNodePositions(ptNodeId, astNodeId);
            transferNodeToken(ptNodeId, astNodeId);

            return astNodeId;
        }
        return null;
    }

    private Integer visit_compress_PARAMETER(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        String nodeType = null;
        List<String> children_types = new ArrayList<>();
        for(Integer childId : ptTable.getChildren(ptNodeId))
            children_types.add(ptTable.getType(childId));

        if(nChildren == 1)
            nodeType = "ValueParameter";
        else if(nChildren == 3 && "Assign".equals(children_types.get(1)))
            nodeType = "OptValueParameter";
        else if(nChildren == 2 && Arrays.asList("Id", "NamespaceName").contains(children_types.get(0)))
            nodeType = "TypedValueParameter";
        else if(nChildren == 2 && "Array".equals(children_types.get(0)))
            nodeType = "TypedValueParameter";
        else if(nChildren == 4 && Arrays.asList("Id", "NamespaceName").contains(children_types.get(0)) && "Assign".equals(children_types.get(2)))
            nodeType = "OptTypedValueParameter";
        else if(nChildren == 4 && "Array".equals(children_types.get(0)) && "Assign".equals(children_types.get(2)))
            nodeType = "OptTypedValueParameter";
        else if(nChildren == 2 && "BinaryAnd".equals(children_types.get(0)))
            nodeType = "ReferenceParameter";
        else if(nChildren == 4 && "BinaryAnd".equals(children_types.get(0)) && "Assign".equals(children_types.get(2)))
            nodeType = "OptReferenceParameter";
        else if(nChildren == 3 && Arrays.asList("Id", "NamespaceName").contains(children_types.get(0)) && "BinaryAnd".equals(children_types.get(1)))
            nodeType = "TypedReferenceParameter";
        else if(nChildren == 5 && Arrays.asList("Id", "NamespaceName").contains(children_types.get(0)) && "BinaryAnd".equals(children_types.get(1)) && "Assign".equals(children_types.get(3)))
            nodeType = "OptTypedReferenceParameter";
        else {
            ptnavigator.getChildren(ptNodeId).forEach(i -> System.out.println(ptTable.getType(i)));
            throw new ASTJsonException("Parameter " + " (" + ptNodeId + ") has " + nChildren + " children that does not follow any known scheme.");
        }

        List<Integer> activeChildren = visitChildren(ptNodeId);
        Integer astNodeId = getNewNode();
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, nodeType);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);

        return astNodeId;
    }

    private Integer visit_compress_UNARY(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren != 2)
            throw new ASTJsonException("Unary expression should have two children (found " + nChildren + ")");

        List<Integer> activeChildren = visitChildren(ptNodeId);
        if (activeChildren.size() != 2)
            throw new ASTJsonException("Unary expression should have two active children (active " + activeChildren.size() + ")");

        Integer astNodeId = getNewNode();
        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, "UnaryOP");
        transferNodePositions(ptnavigator.getChild(ptNodeId, 0), astNodeId);
        transferNodeToken(ptnavigator.getChild(ptNodeId, 0), astNodeId);
        astTable.addEdge(astNodeId, activeChildren.get(1));
        astTable.deleteNode(activeChildren.get(0));
        return astNodeId;

    }

    private Integer visit_compress_ARRAYEXPRESSION(Integer ptNodeId) {
        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        Integer leftBrother = astTable.getNodeASTPtr(ptTable.getLeftBrother(ptNodeId));
        if (leftBrother == null || leftBrother == defsInt.UNDEF_VAL)
            throw new ASTJsonException("ArrayExpression (" + ptNodeId + ") requires a left brother");
        astTable.removeEdge(astTable.getNodeASTPtr(ptTable.getParent(ptNodeId).get(0)), astNodeId);
        astTable.addEdge(astNodeId, leftBrother);
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodeType(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        return astNodeId;
    }

    private Integer visit_compress_ARRAYINITIALISATION(Integer ptNodeId) {
        Integer astNodeId = visit_GENERIC(ptNodeId);
        List<Integer> children = astTable.getChildren(astNodeId);
        if(children.size() > 0 && astTable.getType(children.get(0)).equals("Array"))
            astTable.deleteNode(astTable.getChildren(astNodeId).get(0));
        return astNodeId;
    }

    private Integer visit_compress_FUNCTION(Integer ptNodeId) {
        if(     !ptnavigator.matchChildrenTypes(ptNodeId, Arrays.asList("ArgumentList", "CallEnd")))
            throw new ASTJsonException("Function call (" + ptNodeId + ") should have an ArgumentList : " + ptNodeId);

        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        Integer leftBrother = astTable.getNodeASTPtr(ptTable.getLeftBrother(ptNodeId));
        if (leftBrother == null || leftBrother == defsInt.UNDEF_VAL)
            throw new ASTJsonException("Function call (" + ptNodeId + ") requires a left brother");
        astTable.removeEdge(astTable.getNodeASTPtr(ptTable.getParent(ptNodeId).get(0)), astNodeId);
        astTable.addEdge(astNodeId, leftBrother);
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodeType(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        if(astTable.getType(leftBrother).equals("Id") || astTable.getType(leftBrother).equals("NamespaceName")) {
            transferNodeToken(ptTable.getLeftBrother(ptNodeId), astNodeId);
        }
        return astNodeId;
    }

    private Integer visit_compress_CLASSACCESS(Integer ptNodeId) {
        List<String> types = Arrays.asList("FunctionCall", "MethodCall", "ClassAccessExpression", "ClassAccess");
        List<Integer> activeChildren = visitChildren(ptNodeId);

        // keep ast node with or without active children
        Integer astNodeId = getNewNode();
        Integer leftBrother = astTable.getNodeASTPtr(ptTable.getLeftBrother(ptNodeId));
        if (leftBrother == null)
            throw new ASTJsonException("CLASSACCESS requires a left brother");
        astTable.removeEdge(astTable.getNodeASTPtr(ptTable.getParent(ptNodeId).get(0)), astNodeId);
        astTable.addEdge(astNodeId, leftBrother);
        for(int i = 0 ; i < activeChildren.size() ; i++) {
            if(i+1 < activeChildren.size() && types.contains(astTable.getType(activeChildren.get(i+1))))
                continue;
            astTable.addEdge(astNodeId, activeChildren.get(i));
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        astTable.setNodeType(astNodeId, "BinOP");
        transferNodeToken(ptnavigator.getChild(ptNodeId, 0), astNodeId);
        transferNodePositions(ptnavigator.getChild(ptNodeId, 0), astNodeId);
        return astNodeId;
    }

    private Integer visit_compress_MEMBERDECLARATION(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        String nodeType;

        if((nChildren == 2 || nChildren == 4) && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Variable"))
            nodeType = "MemberDeclaration";
        else if((nChildren == 3 || nChildren == 5) && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Var"))
            nodeType = "MemberDeclaration";
        else if((nChildren == 3 || nChildren == 5) && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Const"))
            nodeType = "ConstMemberDeclaration";
        else {
            boolean variablesOnly = true;
            for(Integer i = 0 ; i < ptTable.getChildren(ptNodeId).size()-1 ; i++) {
                if(ptTable.getType(ptTable.getChildren(ptNodeId).get(i)).equals("Variable"))
                    continue;

                variablesOnly = false;
                break;
            }

            if(variablesOnly)
                nodeType = "MemberDeclaration";
            else {
                ptnavigator.getChildren(ptNodeId).forEach(i -> System.out.println(ptTable.getType(i)));
                throw new ASTJsonException("MemberDeclaration " + " (" + ptNodeId + ") has " + nChildren + " children that does not follow any known scheme.");
            }
        }

        Integer astNodeId = visit_GENERIC(ptNodeId);

        Integer leftBrother = ptTable.getLeftBrother(ptNodeId);
        if(leftBrother != null && ptTable.getType(leftBrother).equals("DeclarationAttributes"))
            leftBrother = ptnavigator.getChild(leftBrother, 0);
        else
            leftBrother = null;
        if(leftBrother != null && Arrays.asList("Public", "Private", "Protected").contains(ptTable.getType(leftBrother))) {
            nodeType = ptTable.getType(leftBrother)+nodeType;
        }

        astTable.setNodeType(astNodeId,  nodeType);
        return astNodeId;
    }

    private Integer visit_compress_METHOD(Integer ptNodeId) {
        Integer astNodeId = visit_GENERIC(ptNodeId);
        Integer identifier = astTable.getChildren(astNodeId).get(0);
        astTable.setNodeImage(astNodeId, astTable.getImage(identifier));

        Integer leftBrother = ptTable.getLeftBrother(ptNodeId);
        if(leftBrother != null && ptTable.getType(leftBrother).equals("DeclarationAttributes"))
            leftBrother = ptnavigator.getChild(leftBrother, 0);
        else
            leftBrother = null;
        if(leftBrother != null && Arrays.asList("Public", "Private", "Protected").contains(ptTable.getType(leftBrother))) {
            astTable.setNodeType(astNodeId,  ptTable.getType(leftBrother) + astTable.getType(astNodeId));
        }
        if(ptTable.getChildren(ptNodeId).size() >= 2 && ptTable.getType(ptTable.getChildren(ptNodeId).get(2)).equals("AbstractFunction"))
            astTable.setNodeType(astNodeId,  "Abstract" + astTable.getType(astNodeId));
        return astNodeId;
    }
    private Integer visit_compress_FUNCTIONSTATEMENT(Integer ptNodeId) {
        Integer astNodeId = visit_GENERIC(ptNodeId);
        Integer identifier = astTable.getChildren(astNodeId).get(0);
        astTable.setNodeImage(astNodeId, astTable.getImage(identifier));
        return astNodeId;
    }


    private Integer visit_compress_RETURNREFERENCE(Integer ptNodeId) {
        Integer astNodeId = getNewNode();

        List<Integer> children = ptTable.getChildren(ptNodeId);
        List<Integer> activeChildren = new ArrayList<Integer>();
        if (children != null) {
            for (Integer mChild : children) {
                visit(mChild);
                Integer childNodePtr = astTable.getNodeASTPtr(mChild);
                if (childNodePtr != defsInt.UNDEF_VAL) {
                    activeChildren.add(childNodePtr);
                }
            }
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodeType(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);
        // keep ast node with or without active children
        for (Integer aChild : activeChildren) {
            astTable.addEdge(astNodeId, aChild);
            astTable.setNodeImage(astNodeId, astTable.getImage(aChild));
        }
        return astNodeId;
    }

    private Integer visit_compress_ECHOSTATEMENT(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        if (nChildren >= 2 && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Print"))
            return visit_GENERIC_rename(ptNodeId, "PrintStatement");
        else
            return visit_GENERIC(ptNodeId);
    }

    private Integer visit_compress_INCLUDEREQUIRE(Integer ptNodeId) {
        int nChildren = ptnavigator.getChildrenSize(ptNodeId);
        String nodeType;
        if(nChildren == 3 && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Include"))
            nodeType = "IncludeStatement";
        else if(nChildren == 3 && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "IncludeOnce"))
            nodeType = "IncludeOnceStatement";
        else if(nChildren == 3 && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "Require"))
            nodeType = "RequireStatement";
        else if(nChildren == 3 && ptnavigator.match(ptnavigator.getChild(ptNodeId, 0), "RequireOnce"))
            nodeType = "RequireOnceStatement";
        else {
            ptnavigator.getChildren(ptNodeId).forEach(i -> System.out.println(ptTable.getType(i)));
            throw new ASTJsonException(ptTable.getType(ptNodeId) + " (" + ptNodeId + ") has " + nChildren + " children that does not follow any known scheme.");
        }
        return visit_GENERIC_rename(ptNodeId, nodeType);
    }

    private Integer visit_ARGUMENTLIST(Integer ptNodeId) {

        List<Integer> activeChildren = visitChildren(ptNodeId);

        Integer astNodeId = getNewNode();
        for (Integer aChild : activeChildren) {
            Integer argNodeId = getNewNode();
            astTable.setNodeType(argNodeId, "Argument");
            astTable.addEdge(astNodeId, argNodeId);
            astTable.addEdge(argNodeId, aChild);
        }

        astTable.setNodePtr(ptNodeId, astNodeId);
        transferNodeType(ptNodeId, astNodeId);
        transferNodePositions(ptNodeId, astNodeId);
        transferNodeToken(ptNodeId, astNodeId);
        return astNodeId;

    }

    public Integer visit_LAMBDA_FUNCTION_STATEMENT(Integer ptNodeId) {
        Integer newNode = visit_GENERIC(ptNodeId);
        astTable.setNodeImage(newNode, "#lambda_"+(iNextLambda++));
        return newNode;
    }


    //
    // dispatch visit
    //

    public AST visit() throws ASTJsonException {
        visit(ptTable.getRoot());
        new ASTVariableIndexer(astTable);
        return astTable;
    }

    public void visit(Integer ptNodeId) throws ASTJsonException {

        String curType = ptTable.getType(ptNodeId);
        if (curType == null)
            throw new ASTJsonException("ERROR: missing node " + ptNodeId);
        Integer newNode = null;
        switch (curType) {

            //
            // generic nodes to be removed when empty
            //
            case "AbstractFunction":
            case "As":
            case "Arrow":
            case "Extends":
            case "Darrow":
            case "CallEnd":
            case "Declare":
            case "Dollar":
            case "EndFor":
            case "EndForeach":
            case "EndIf":
            case "EndOfFunction":
            case "EndOfStatement":
            case "ExpressionStatement":
            case "EndSwitch":
            case "EndWhile":
            case "Namespace":
            case "Increment":
            case "Include":
            case "IncludeOnce":
            case "Decrement":
            case "Stop":
            case "DeclarationAttributes":
            case "Var":
            case "IncludeEnd":
            case "Public":
            case "Private":
            case "Protected":
            case "New":
            case "Require":
            case "RequireOnce":
            case "Return":
            case "Print":
                newNode = visit_compress_GENERIC(ptNodeId);
                break;

            //
            // not-to-compress GENERIC
            //
            case "AlternateCastExpression":
            case "Arobas":
            case "Array":
            case "Bool":
            case "Boolean":
            case "Callable":
            case "CastExpression":
            case "ClassName":
            case "ClassStatement":
            case "Clone":
            case "ConditionalExpression":
            case "ConditionalFalse":
            case "ConditionalTrue":
            case "DeclareStatement":
            case "Double":
            case "EId":
            case "ExecString":
            case "Float":
            case "ForEachPredicate":
            case "ForPredicate":
            case "Global":
            case "HeredocFlow":
            case "Implements":
            case "Int":
            case "InterfaceStatement":
            case "LegalChar":
            case "NamespaceName":
            case "NamespaceStatement":
            case "Object":
            case "ParameterList":
            case "ParentClassName":
            case "ReturnValueFunction":
            case "ReturnValueMethod":
            case "Static":
            case "String":
            case "TraitStatement":
            case "UseInsteadOf":
            case "UseStatement":
            case "UseTraitAs":
            case "UseTraitDeclaration":
            case "VariableStatement":
            case "VarName":
            case "Id":
            case "ClassInstanciation":
            case "Condition":
            case "True":
            case "False":
            case "StringExpression":
            case "StringLiteral":
            case "IntegerLiteral":
            case "HexLiteral":
            case "DoubleLiteral":
            case "Not":
            case "Null":
            case "Html":
            case "Start":
            case "Integer":
            case "UnsetStatement":
                newNode = visit_GENERIC(ptNodeId);
                break;

            //
            // renamed nodes
            //
            case "TryCatchStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "TryCatch");
                break;
            case "CatchStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "Catch");
                break;
            case "ThrowStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "Throw");
                break;
            case "ContinueStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "Continue");
                break;
            case "BreakStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "Break");
                break;
            case "ForCondition":
                newNode = visit_GENERIC_rename(ptNodeId, "Condition");
                break;
            case "ReturnStatement":
                newNode = visit_GENERIC_rename(ptNodeId, "Return");
                break;
            case "EchoStatement":
                newNode = visit_compress_ECHOSTATEMENT(ptNodeId);
                break;
            case "IncludeStatement":
                newNode = visit_compress_INCLUDEREQUIRE(ptNodeId);
                break;


            //
            // special nodes
            //
            case "ArrayExpression":
                visit_compress_ARRAYEXPRESSION(ptNodeId);
                break;
            case "ArrayInitialisation":
                visit_compress_ARRAYINITIALISATION(ptNodeId);
                break;
            case "ReturnReferenceFunction":
            case "ReturnReferenceMethod":
                newNode = visit_compress_RETURNREFERENCE(ptNodeId);
                break;
            case "FunctionStatement":
                newNode = visit_compress_FUNCTIONSTATEMENT(ptNodeId);
                break;
            case "MethodStatement":
                newNode = visit_compress_METHOD(ptNodeId);
                break;
            case "Const":
                newNode = visit_compress_CONST(ptNodeId);
                break;
            case "CaseStatement":
                newNode = visit_compress_CASE(ptNodeId);
                break;
            case "DefaultStatement":
                newNode = visit_compress_SWITCHDEFAULT(ptNodeId);
                break;
            case "ForInit":
            case "ForIncrement":
                newNode = visit_compress_FORINCREMENTINIT(ptNodeId);
                break;
            case "UnaryExpression":
            case "BitwiseNotExpression":
                newNode = visit_compress_UNARY(ptNodeId);
                break;
            case "CompoundStatement":
            case "StartOfFunction":
                newNode = visit_compress_COMPOUND(ptNodeId);
                break;
            case "FunctionCall":
            case "MethodCall":
                newNode = visit_compress_FUNCTION(ptNodeId);
                break;
            case "ClassAccessExpression":
                newNode = visit_compress_CLASSACCESS(ptNodeId);
                break;
            case "ScopeResolution":
                newNode = visit_compress_GENERIC(ptNodeId);
                break;
            case "PostfixExpression":
                newNode = visit_compress_POSTFIX(ptNodeId);
                break;
            case "Variable":
                newNode = visit_VARIABLE(ptNodeId);
                break;
            case "ForEachStatement":
                newNode = visit_compress_FOREACH(ptNodeId);
                break;
            case "ForStatement":
                newNode = visit_compress_FOR(ptNodeId);
                break;
            case "SwitchStatement":
                newNode = visit_compress_SWITCHSTATEMENT(ptNodeId);
                break;
            case "ConditionalStatement":
                newNode = visit_compress_CONDITIONAL(ptNodeId);
                break;
            case "IfStatement":
            case "ElseifStatement":
            case "ElseStatement":
            case "WhileStatement":
            case "DoWhileStatement":
                newNode = visit_compress_IFELSEWHILESTATEMENT(ptNodeId);
                break;
            case "AssignmentExpression":
            case "LogicalAndExpression":
            case "LogicalOrExpression":
            case "ComparativeExpression":
            case "LowerLogicalAndExpression":
            case "LowerLogicalOrExpression":
            case "LowerLogicalXorExpression":
            case "InstanceofExpression":
            case "AdditiveExpression":
            case "MultiplicativeExpression":
            case "BitwiseAndExpression":
            case "BitwiseOrExpression":
            case "BitwiseXorExpression":
            case "ShiftExpression":
            case "CoalesceExpression":
                newNode = visit_compress_ternary(ptNodeId);
                break;
                //TODO: Handle const / var / global etc + ternary
            case "MemberDeclaration":
            case "VariableDeclaration":
                newNode = visit_compress_MEMBERDECLARATION(ptNodeId);
                break;
            case "Parameter":
                newNode = visit_compress_PARAMETER(ptNodeId);
                break;
            case "ArgumentList":
                newNode = visit_ARGUMENTLIST(ptNodeId);
                break;
            case "Gequal":
            case "Lequal":
            case "Great":
            case "Less":
            case "Equal":
            case "Nequal":
            case "Nequal2":
            case "DefEqual":
            case "DefNequal":
            case "InstanceOf":
                newNode = visit_compress_RelOP(ptNodeId);
                break;
            case "LogicalAnd":
            case "LogicalOr":
            case "LogicalXor":
                newNode = visit_compress_LogicOP(ptNodeId);
                break;
            case "Assign":
            case "PlusAssign":
            case "MinusAssign":
            case "DivideAssign":
            case "ConcatAssign":
            case "ModuloAssign":
            case "ProductAssign":
            case "BinaryAndAssign":
            case "BinaryOrAssign":
            case "BinaryXorAssign":
            case "RightShiftAssign":
            case "LeftShiftAssign":
            case "CoalesceAssign":
            case "Plus":
            case "Minus":
            case "Division":
            case "Modulo":
            case "Star":
            case "Dot":
            case "BinaryNot":
            case "BinaryOr":
            case "BinaryXor":
            case "BinaryAnd":
            case "LeftShift":
            case "RightShift":
            case "Coalesce":
                newNode = visit_compress_BinOP(ptNodeId);
                break;
            case "PostfixIncrement":
            case "PrefixIncrement":
            case "PostfixDecrement":
            case "PrefixDecrement":
                newNode = visit_compress_INCREMENT(ptNodeId);
                break;
            case "LambdaFunctionStatement":
                newNode = visit_LAMBDA_FUNCTION_STATEMENT(ptNodeId);
                break;
            default:
                throw new ASTJsonException("ERROR: invalid type " + curType +
                        " for node " + ptNodeId);
        }
        if (newNode != null && ptTable.getRoot().equals(ptNodeId))
            astTable.setRoot(newNode);
    }


}
