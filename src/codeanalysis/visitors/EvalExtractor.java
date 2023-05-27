package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.util.*;

import java.util.ArrayList;
import java.util.List;


public class EvalExtractor {
    protected ASTDynamic tree;
    protected ASTNavigatorCl<Integer, String> navigator;
    protected List<ASTDynamic> evals = new ArrayList<>();
    protected Integer lastParsedEdgeDest = null;
    protected List<Integer> payloadSizes = new ArrayList<>();

    public void extract(ASTDynamic tree) {
        this.evals.clear();
        this.tree = tree;
        this.navigator  = new ASTNavigatorCl<>(tree.getSuccTable(), tree.getTypeTable());
        this.visit_extract(tree.getRoot(), -1);

        for(ASTDynamic d : evals) {
            this.lastParsedEdgeDest = null;
            this.visit_delpayload(d.getRoot(), d);
            if (this.lastParsedEdgeDest != null) {
                payloadSizes.add(n_nodes_regular_edges(this.lastParsedEdgeDest, d, 0));
                Integer parent = d.getParseEdgeRev(this.lastParsedEdgeDest).get(0);
                d.deleteSubTree(this.lastParsedEdgeDest);
                d.setNodeType(this.lastParsedEdgeDest, "Payload");
                d.addParseEdge(parent, this.lastParsedEdgeDest);
            }
            else
                payloadSizes.add(0);
        }
    }

    public List<ASTDynamic> getEvalList() {
        return evals;
    }

    protected void visit_extract(Integer nodeId, Integer evalId) {

        if(
                evalId == -1 &&
                "eval".equals(tree.getImage(nodeId)) &&
                "FunctionCall".equals(tree.getType(nodeId)) &&
                navigator.match(navigator.getChild(nodeId, 0), "Id") &&
                navigator.match(navigator.getChild(nodeId, 1), "ArgumentList"))
        {
            evalId = evals.size();
            ASTDynamic astDyn = new ASTDynamic();
            astDyn.copyNode(nodeId, tree, nodeId);
            astDyn.setRoot(nodeId);
            astDyn.setFilename(tree.getFilename());
            evals.add(astDyn);
        }
        else if(evalId != -1) {
            evals.get(evalId).copyNode(nodeId, tree, nodeId);
        }

        for (Integer childId : tree.getChildren(nodeId)) {
            this.visit_extract(childId, evalId);
            if (evalId != -1)
                evals.get(evalId).addEdge(nodeId, childId);
        }

        for (Integer childId : tree.getDynamicResolution(nodeId)) {
            this.visit_extract(childId, evalId);
            if (evalId != -1)
                evals.get(evalId).addDynamicResolution(nodeId, childId);
        }
        for (Integer childId : tree.getDataflowResolution(nodeId)) {
            this.visit_extract(childId, evalId);
            if (evalId != -1)
                evals.get(evalId).addDataflowResolution(nodeId, childId);
        }
        for (Integer childId : tree.getParseEdge(nodeId)) {
            this.visit_extract(childId, evalId);
            if (evalId != -1)
                evals.get(evalId).addParseEdge(nodeId, childId);
        }
    }

    protected void visit_delpayload(Integer nodeId, ASTDynamic eval) {
        if(eval.getType(nodeId).endsWith("Error")) {
            this.lastParsedEdgeDest = null;
            return;
        }
        for (Integer childId : eval.getChildren(nodeId))
            this.visit_delpayload(childId, eval);

        for (Integer childId : eval.getDynamicResolution(nodeId))
            this.visit_delpayload(childId, eval);

        for (Integer childId : eval.getDataflowResolution(nodeId))
            this.visit_delpayload(childId, eval);

        for (Integer childId : eval.getParseEdge(nodeId)) {
            this.lastParsedEdgeDest = childId;
            if(n_nodes_regular_edges(childId, eval, 0) > 500) // Significant code
                return;
            this.visit_delpayload(childId, eval);
        }
    }

    protected int n_nodes_regular_edges(Integer nodeId, ASTDynamic tree, int curr_count) {
        for(Integer childId : tree.getChildren(nodeId)) {
            curr_count = n_nodes_regular_edges(childId, tree, curr_count);
        }
        return curr_count+1;
    }

    public Integer getPayloadSize(int i) {
        return payloadSizes.get(i);
    }
}
