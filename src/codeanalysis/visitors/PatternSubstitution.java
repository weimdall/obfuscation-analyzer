/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 * @author Ettore Merlo <ettore.merlo@polymtl.ca>
 */

package org.polymtl.codeanalysis.visitors;

import org.polymtl.codeanalysis.exceptions.*;
import org.polymtl.codeanalysis.model.*;
import org.polymtl.codeanalysis.parser.*;
import org.polymtl.codeanalysis.reader.*;
import org.polymtl.codeanalysis.util.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.*;


public class PatternSubstitution {
    private static final Logger LOGGER = CustomLogger.getLogger(PatternSubstitution.class.getName());

    private ASTDynamic astSub                           = null;
    private AST ast                                     = null;
    private ASTNavigatorCl<Integer, String> navigator   = null;
    private String filename                             = null;
    private String inDirAst                             = null;
    private String inDirDd                             = null;
    private boolean evalFound                           = false;
    private ErrorList evalErrors                        = new ErrorList();

    protected Map<Integer, List<Integer>> nodePatternsMatched = new HashMap<>();
    protected Map<Integer, AST> nodeASTResolved = new HashMap<>(); // doesn't handle recursive AST ... create new class ASTDynamic

    static class ErrorList extends ArrayList<Exception> {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean add(Exception e) {
            LOGGER.warning(e.getMessage());
            //e.printStackTrace();
            return super.add(e);
        }
    }

    enum STATUS {
        CHANGE,     // Change occurred, need to re-run
        NOCHANGE,   // No-change, no re-run
        DONE        // Final change at this stage, no re-run at THIS level
    }


    public ASTDynamic getDynamicAst() {
        return astSub;
    }

    public boolean visit(AST ast, String filename, String inDirAst, String inDirCfg) {
        this.astSub = new ASTDynamic(ast);
        this.ast = new AST(ast);
        this.navigator = new ASTNavigatorCl<>(ast.getSuccTable(), ast.getTypeTable());
        this.filename = filename;
        this.inDirDd = inDirCfg;
        this.inDirAst = inDirAst;
        this.evalErrors = new ErrorList();
        return visit(ast.getRoot());
    }

    private boolean visit(Integer nodeId) {
        boolean matched = false;
        switch (ast.getType(nodeId)) { // TODO: Remove recursive calls, handle with a loop here
            case "FunctionCall":
                visit_FunctionCall_init(nodeId);
                matched = true;
                break;
            default:
                break;
        }
        List<Integer> children = ast.getChildren(nodeId);
        if(children != null)
            for (Integer child = 0 ; child < ast.getChildren(nodeId).size() ; child++)
                matched = visit(ast.getChildren(nodeId).get(child)) || matched;
        return matched;
    }

    private STATUS visit_FunctionCall_init(Integer nodeId) {
        if(ast.getChildren(nodeId).size() != 2 || !ast.getType(ast.getChildren(nodeId).get(1)).equals("ArgumentList")) {
            for(Integer childId : ast.getChildren(nodeId))
                System.out.println(ast.getType(childId));
            evalErrors.add(new ASTDynamicException("FunctionCall requires an ArgumentList"));
            dynamicReplace(nodeId, "VisitFailed", "Exception".getBytes());
            return STATUS.NOCHANGE;
        }

        STATUS last_status = STATUS.CHANGE;
        STATUS return_status = STATUS.CHANGE;
        while(last_status == STATUS.CHANGE) {
            last_status = STATUS.NOCHANGE;
            Integer identifier = ast.getChildren(nodeId).get(0);
            if(ast.getType(identifier).equals("Id")) {
                switch (ast.getImage(nodeId)) {
                    case "eval":
                        evalFound = true;
                        try {
                            last_status = applyRule_eval_FunctionCall_init(nodeId);
                        } catch (Exception e) {
                            evalErrors.add(e);
                            e.printStackTrace();
                            LOGGER.warning("Error while resolving eval : "+e.getMessage());
                            return STATUS.NOCHANGE;
                        }
                        break;
                }
                if(last_status == STATUS.DONE) {
                    return_status = STATUS.CHANGE;
                    break;
                }
                if(last_status == STATUS.CHANGE)
                    return_status = STATUS.CHANGE;
            }
        }
        return return_status;
    }

    @SuppressWarnings("fallthrough")
    private STATUS applyRule_eval_FunctionCall_init(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);
        Integer firstArgumentSub = firstArgumentNode;
        if(astSub.getDynamicResolution(firstArgumentNode) != null && astSub.getDynamicResolution(firstArgumentNode).size() != 0)
            firstArgumentSub = astSub.getDynamicResolution(firstArgumentNode).get(astSub.getDynamicResolution(firstArgumentNode).size()-1);
        else if(astSub.getDataflowResolution(firstArgumentNode) != null && astSub.getDataflowResolution(firstArgumentNode).size() != 0)
            firstArgumentSub = astSub.getDataflowResolution(firstArgumentNode).get(astSub.getDataflowResolution(firstArgumentNode).size()-1);


        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                ast.setNodeType(firstArgument, "StringLiteral");
                ast.setNodeImage(firstArgument, new String(decodeHexString(ast.getImage(firstArgument))));
                // FALL-THROUGH
            case "StringExpression": // TODO: check for variable
            case "StringLiteral":
                try {
                    LOGGER.fine("Parse resolved string");
                    astSub.setEvalString(nodeId, StringParser.ReplaceEscapedHex(ast.getImage(firstArgument)));
                    AST newAST = StringParser.parse( ast.getImage(firstArgument) );
                    if(newAST.getTypeTable().keySet().size() > 500) { // Probably a web shell, stop looking for eval
                        LOGGER.info("##WEBSHELL DETECTED##");
                        astSub.addParseEdge(firstArgumentSub, newAST);
                        return STATUS.NOCHANGE;
                    }
                    PatternSubstitution substitution = new PatternSubstitution();
                    substitution.visit(newAST, filename, inDirAst, inDirDd);
                    this.evalErrors.addAll(substitution.getEvalErrors());
                    astSub.addParseEdge(firstArgumentSub, substitution.getDynamicAst());
                } catch (Exception e) {
                    evalErrors.add(new ParseException("Exception occurred while parsing string - " + e.getMessage()));
                    Integer failNode = astSub.getNextNodeId();
                    astSub.setNodeType(failNode, "ParseFailed");
                    astSub.setNodeImage(failNode, "Exception");

                    astSub.addParseEdge(firstArgumentSub, failNode);
                } catch (Throwable e) {
                    evalErrors.add(new ParseException("Error occurred while parsing string - " + e.getMessage()));
                    Integer failNode = astSub.getNextNodeId();
                    astSub.setNodeType(failNode, "ParseFailed");
                    astSub.setNodeImage(failNode, "Error");

                    astSub.addParseEdge(firstArgumentSub, failNode);
                }
                return STATUS.NOCHANGE;
            case "Variable":
                return applyRule_variable_eval(firstArgument);
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            case "BinOP":
                return visit_BinOP_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Found eval("+ast.getType(firstArgument)+")"));
                dynamicReplace(firstArgument, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    @SuppressWarnings("fallthrough")
    private STATUS visit_FunctionCall_eval(Integer nodeId) throws ASTDynamicException { // eval(FuncCall)
        if(ast.getChildren(nodeId).size() != 2 || !ast.getType(ast.getChildren(nodeId).get(1)).equals("ArgumentList")) {
            evalErrors.add(new ASTDynamicException("FunctionCall requires an ArgumentList : found" + ast.getType(ast.getChildren(nodeId).get(1))));
            dynamicReplace(nodeId, "VisitFailed", "Exception".getBytes());
            return STATUS.NOCHANGE;
        }

        STATUS last_status = STATUS.CHANGE;
        STATUS return_status = STATUS.NOCHANGE;
        while(last_status == STATUS.CHANGE) {
            Integer identifier = ast.getChildren(nodeId).get(0);
            String type  = ast.getType(identifier);
            String image = ast.getImage(identifier);
            switch(type) {
                case "DecodeFailed":
                    last_status = STATUS.NOCHANGE;
                    break;
                case "Variable":
                    last_status = applyRule_variable_eval(identifier);
                    /* if(last_status != STATUS.CHANGE) {
                        evalErrors.add(new ASTDataflowException("Found variable func call in eval params"));
                        dynamicReplace(nodeId, "DecodeFailed", "FuncVarUnknown".getBytes());
                        last_status = STATUS.NOCHANGE;
                    }*/
                    break;
                case "FunctionCall":
                    LOGGER.fine("Function identifier is FunctionCall " + image);
                    last_status = visit_FunctionCall_eval(identifier);
                    break;
                case "BinOP":
                    LOGGER.fine("Function identifier is BinOP " + image);
                    last_status = visit_BinOP_eval(identifier);
                    break;
                case "StringLiteral":
                case "StringExpression":
                    image = StringParser.ReplaceEscapedHex(ast.getImage(identifier));
                    // FALL-THROUGH
                case "Id":
                    LOGGER.fine("visit_FunctionCall_eval " + image);
                    switch (image) {
                        case "base64_decode":
                            last_status = applyRule_base64decode_FunctionCall_eval(nodeId);
                            break;
                        case "base64_encode":
                            last_status = applyRule_base64encode_FunctionCall_eval(nodeId);
                            break;
                        case "str_rot13":
                            last_status = applyRule_strrot13_FunctionCall_eval(nodeId);
                            break;
                        case "strrev":
                            last_status = applyRule_strrev_FunctionCall_eval(nodeId);
                            break;
                        case "rawurldecode":
                        case "urldecode":
                            last_status = applyRule_rawurldecode_FunctionCall_eval(nodeId);
                            break;
                        case "gzinflate":
                            last_status = applyRule_gzinflate_FunctionCall_eval(nodeId);
                            break;
                        case "gzdeflate":
                            last_status = applyRule_gzdeflate_FunctionCall_eval(nodeId);
                            break;
                        case "gzcompress":
                            last_status = applyRule_gzcompress_FunctionCall_eval(nodeId);
                            break;
                        case "gzuncompress":
                            last_status = applyRule_gzuncompress_FunctionCall_eval(nodeId);
                            break;
                        default:
                            evalErrors.add(new ASTDynamicException("Found " + image + " func in eval subtree"));
                            dynamicReplace(nodeId, "DecodeFailed", "FuncIdUnknown".getBytes());
                            last_status = STATUS.NOCHANGE;
                    }
                    if(last_status == STATUS.DONE) {
                        return_status = STATUS.CHANGE;
                        break;
                    }
                    if(last_status == STATUS.CHANGE)
                        return_status = STATUS.CHANGE;
                    break;
                default:
                    evalErrors.add(new ASTDynamicException("Found " + type + " as func identifier"));
                    dynamicReplace(nodeId, "DecodeFailed", "FuncUnknown".getBytes());
                    last_status = STATUS.NOCHANGE;
            }
        }
        return return_status;
    }

    private STATUS visit_BinOP_eval(Integer nodeId) {
        if(ast.getChildren(nodeId).size() != 2) {
            evalErrors.add(new ASTDynamicException("BinOP requires 2 children : found" + ast.getChildren(nodeId).size()));
            dynamicReplace(nodeId, "VisitFailed", "Exception".getBytes());
            return STATUS.NOCHANGE;
        }

        STATUS last_status = STATUS.CHANGE;
        STATUS return_status = STATUS.NOCHANGE;
        while(last_status == STATUS.CHANGE) {
            Integer leftChild  = ast.getChildren(nodeId).get(0);
            Integer rightChild = ast.getChildren(nodeId).get(1);
            last_status = applyRule_child_BinOP_eval(leftChild);
            if(last_status == STATUS.CHANGE)
                continue;
            last_status = applyRule_child_BinOP_eval(rightChild);
            if(last_status == STATUS.CHANGE)
                continue;

            switch(ast.getImage(nodeId)) {
                case ".":
                    last_status = applyRule_concat_BinOP_eval(nodeId);
                    if(last_status == STATUS.DONE) {
                        return_status = STATUS.CHANGE;
                        break;
                    }
                    if(last_status == STATUS.CHANGE)
                        return_status = STATUS.CHANGE;
                    break;
                default:
                    evalErrors.add(new ASTDynamicException("Found BinOP image (" + ast.getImage(nodeId) + ")"));
                    dynamicReplace(nodeId, "DecodeFailed", "BinOP".getBytes());
                    last_status = STATUS.NOCHANGE;
            }
        }
        return return_status;
    }

    private STATUS applyRule_concat_BinOP_eval(Integer nodeId) {
        Integer leftChild  = ast.getChildren(nodeId).get(0);
        Integer rightChild = ast.getChildren(nodeId).get(1);
        switch (ast.getType(leftChild)) {
            case "DecodeFailed":
                return STATUS.NOCHANGE;
            case "StringExpression":
            case "StringLiteral":
                switch (ast.getType(rightChild)) {
                    case "DecodeFailed":
                        return STATUS.NOCHANGE;
                    case "StringExpression":
                    case "StringLiteral":
                        String image_left = ast.getImage(leftChild);
                        String image_right = ast.getImage(rightChild);
                        dynamicReplace(nodeId, "StringLiteral", (image_left+image_right).getBytes());
                        return STATUS.DONE;
                    default:
                        evalErrors.add(new ASTDynamicException("Cannot decode concat BinOP rightChild: "+ast.getType(rightChild)+""));
                        dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
                }
                return STATUS.NOCHANGE;
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode concat BinOP leftChild: "+ast.getType(leftChild)+""));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_child_BinOP_eval(Integer nodeId) {
        switch (ast.getType(nodeId)) {
            case "DecodeFailed":
            case "StringExpression":
            case "StringLiteral":
                break;
            case "HexLiteral":
                ast.setNodeType(nodeId, "StringLiteral");
                ast.setNodeImage(nodeId, new String(decodeHexString(ast.getImage(nodeId))));
                return STATUS.DONE;
            case "Variable":
                return applyRule_variable_eval(nodeId);
            case "FunctionCall":
                return visit_FunctionCall_eval(nodeId);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode Child BinOP: "+ast.getType(nodeId)+""));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    @SuppressWarnings("fallthrough")
    private STATUS applyRule_base64decode_FunctionCall_eval(Integer nodeId) {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                ast.setNodeType(firstArgument, "StringLiteral");
                ast.setNodeImage(firstArgument, new String(decodeHexString(ast.getImage(firstArgument))));
                // FALL-THROUGH
            case "StringExpression": // TODO: check for variable
            case "StringLiteral":
                String in = ast.getImage(firstArgument);
                if(in == null) in = "";
                try {
                    byte[] out = DecodeBase64(in.getBytes());
                    dynamicReplace(nodeId, "HexLiteral", out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(new ASTDynamicException("Cannot decode base64_decode("+ast.getType(firstArgument)+") : " + e.getMessage()));
                    dynamicReplace(nodeId, "DecodeFailed", "Bad data".getBytes());
                }
                break;
            case "Variable":
                return applyRule_variable_eval(firstArgument);
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode base64_decode("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_base64encode_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                byte[] in = getTokenByte(firstArgument);
                byte[] out = EncodeBase64(in);

                dynamicReplace(nodeId, "StringLiteral", out);
                return STATUS.DONE;
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode base64_encode("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_strrot13_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "StringExpression": // TODO: check for variable
            case "StringLiteral":
            case "HexLiteral":
                try {
                    String type = ast.getType(firstArgument);
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = DecodeRot13(in);

                    dynamicReplace(nodeId, type, out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "Variable":
                return applyRule_variable_eval(firstArgument);
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode str_rot13("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_rawurldecode_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "StringExpression": // TODO: check for variable
            case "StringLiteral":
            case "HexLiteral":
                try {
                    String type = ast.getType(firstArgument);
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = DecodeRawUrl(in);

                    dynamicReplace(nodeId, type, out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "Variable":
                return applyRule_variable_eval(firstArgument);
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode rawurldecode("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_strrev_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "StringExpression": // TODO: check for variable
            case "StringLiteral":
            case "HexLiteral":
                try {
                    String type = ast.getType(firstArgument);
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = DecodeStrrev(in);

                    dynamicReplace(nodeId, type, out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "Variable":
                return applyRule_variable_eval(firstArgument);
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode strrev("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_gzinflate_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                try {
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = InflateGZ(in);

                    dynamicReplace(nodeId, "HexLiteral", out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode gzinflate("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_gzdeflate_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                try {
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = DeflateGZ(in);

                    dynamicReplace(nodeId, "HexLiteral", out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode gzinflate("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_gzcompress_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                try {
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = CompressGZ(in);

                    dynamicReplace(nodeId, "HexLiteral", out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode gzcompress("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_gzuncompress_FunctionCall_eval(Integer nodeId) throws ASTDynamicException {
        Integer argumentList = ast.getChildren(nodeId).get(1);
        Integer firstArgumentNode = ast.getChildren(argumentList).get(0);
        Integer firstArgument = ast.getChildren(firstArgumentNode).get(0);

        switch (ast.getType(firstArgument)) {
            case "DecodeFailed":
                break;
            case "HexLiteral":
                try {
                    byte[] in = getTokenByte(firstArgument);
                    byte[] out = UncompressGZ(in);

                    dynamicReplace(nodeId, "HexLiteral", out);
                    return STATUS.DONE;
                } catch (Exception e) {
                    evalErrors.add(e);
                    dynamicReplace(nodeId, "DecodeFailed", "Exception".getBytes());
                }
                break;
            case "FunctionCall":
                return visit_FunctionCall_eval(firstArgument);
            default:
                evalErrors.add(new ASTDynamicException("Cannot decode gzuncompress("+ast.getType(firstArgument)+")"));
                dynamicReplace(nodeId, "DecodeFailed", "NoLiteral".getBytes());
        }
        return STATUS.NOCHANGE;
    }

    private STATUS applyRule_variable_eval(Integer nodeId) throws ASTDynamicException {
        LOGGER.fine("applyRule_variable_eval");
        CFGAndDDJsonReader cfgddreader = new CFGAndDDJsonReader();
        CFGJsonReader cfgreader = new CFGJsonReader();
        ASTJsonReader astreader = new ASTJsonReader();
        String cfg_json = null, dd_json = null;
        CFGWithDD cfgdd = null;
        AST astdd = ast;
        String filename = this.filename.replace(".ast.json.gz", "").replace(".ast.json", "");
        String kitname = filename.substring(2, filename.indexOf("/", 3));

        try {
            if(inDirDd == null)
                throw new ASTDataflowException("No DD info, requires -idd ");

            cfg_json = Paths.get(inDirDd, "/cfg/",filename+".cfg.json").toString();
            dd_json  = Paths.get(inDirDd, "/dd/", "kitsDd_"+kitname+".php.cfg.json").toString();
        } catch (Exception  e) {
            evalErrors.add(new ASTDataflowException("Cannot resolve variable - missing file : " + e.getMessage()));
            dataflowReplace(nodeId, "DecodeFailed", "NoDDFile".getBytes());
            return STATUS.NOCHANGE;
        }

        // Read graphs
        try {
            cfgdd = cfgddreader.read_cfg(cfg_json, dd_json);
        } catch (Exception e) {
            evalErrors.add(new ASTDataflowException("Cannot resolve variable - failed to read files : " + e.getMessage()));
            dataflowReplace(nodeId, "DecodeFailed", "CannotReadDDFile".getBytes());
            return STATUS.NOCHANGE;
        }


        Integer astNodeId = nodeId;
        while(astSub.getDataDependency().containsKey(astNodeId)) {
            LOGGER.finer("Follow substitution dd - " + astNodeId + " to " + astSub.getDataDependency(astNodeId));
            astNodeId = astSub.getDataDependency(astNodeId);
        }
        Integer cfgDDSucc = null;
        Integer cfgNodeId = cfgdd.getNodeCfgPtr(astNodeId);
        LOGGER.finer("Looking for dd on AST " + astNodeId + " - CFG " + cfgNodeId);
        List<Integer> cfgNodeIdDefUseRev = cfgdd.getDefUseRev(cfgNodeId);
        LOGGER.finer("Found def_use_rev " + cfgNodeIdDefUseRev);
        if(cfgNodeIdDefUseRev != null) {
            Integer definition = null;
            // Pick local definition if possible
            for(Integer i : cfgNodeIdDefUseRev) {
                if(cfgdd.getType(i) != null)
                    definition = i;
            }
            //Otherwise, pick last definition
            if(definition == null)
                definition = cfgNodeIdDefUseRev.get(cfgNodeIdDefUseRev.size() - 1);
            LOGGER.finer("Pick definition " + definition);
            List<Integer> cfgNodeIdCmdPropagRev = cfgdd.getCmdPropagRev(definition);
            LOGGER.finer("Found cmd_propag_rev " + cfgNodeIdCmdPropagRev);
            cfgDDSucc = cfgNodeIdCmdPropagRev.get(0);
            LOGGER.finer("DDSucc is " + cfgDDSucc);
        }


        if(cfgDDSucc == null) {
            evalErrors.add(new ASTDataflowException("Failed to resolve variable value using dd for cfg `node : " + cfgNodeId));
            dataflowReplace(nodeId, "DecodeFailed", "NoDDOnVar".getBytes());
            return STATUS.NOCHANGE;
        }
        if(cfgdd.getType(cfgDDSucc) == null) {
            LOGGER.info("DD outside current CFG, search in includes");

            List<String> include_files = new ArrayList<>();
            walk(Paths.get(inDirDd, "/cfg/"+kitname).toString(), include_files);
            for(String include_file : include_files) {
                try {
                    CFG inc_cfg = cfgreader.read(include_file);
                    if (inc_cfg.getType(cfgDDSucc) != null) {
                        LOGGER.info("DD found in " + include_file);
                        cfgdd.importSubGraph(inc_cfg, inc_cfg.getRoot());
                        /*for(Integer entryId : inc_cfg.getAllFuncEntryNode()) // Handle func's CFG ?
                            cfgdd.importSubTree(inc_cfg, entryId);*/
                        String f = inc_cfg.getFilename().substring(2);
                        f = f.substring(f.indexOf("/"));
                        astdd = astreader.read(Paths.get(inDirAst, kitname+f + ".ast.json.gz").toString());
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.warning("Cannot read " + include_file + " : " + e.getMessage());
                }
            }

            if(cfgdd.getType(cfgDDSucc) == null) {
                evalErrors.add(new ASTDataflowException("DD outside CFG `node : " + cfgDDSucc));
                dataflowReplace(nodeId, "DecodeFailed", "DDOutsideCFG".getBytes());
                return STATUS.NOCHANGE;
            }
        }
        if(cfgdd.getType(cfgDDSucc) != null && cfgdd.getType(cfgDDSucc).equals("RetValue")) {
            // If affected value is a RetValue, point to the FunctionCall
            Integer callEnd = cfgdd.getParent(cfgDDSucc).get(0);
            Integer callBegin = cfgdd.getCallBegin(callEnd);
            Integer funcId = cfgdd.getCallExpr(callBegin);
            cfgDDSucc = cfgdd.getParent(funcId).get(0);
        }
        Integer astDDSucc = cfgdd.getNodeAstPtr(cfgDDSucc);
        if(astDDSucc.equals(defsInt.UNDEF_VAL) || astdd.getType(astDDSucc) == null) {
            evalErrors.add(new ASTDataflowException("Failed to bind cfg node to ast `node : " + cfgNodeId));
            dataflowReplace(nodeId, "DecodeFailed", "NoASTNode".getBytes());
            return STATUS.NOCHANGE;
        }

        // Replace variable node by subtree
        Integer parent = ast.getParent(nodeId).get(0);
        List<Integer> children = new ArrayList<>(ast.getChildren(parent));
        ast.deleteSubTree(nodeId);

        Integer subTree = ast.importSubGraph(astdd, astDDSucc, astSub.getDataDependency());
        astSub.addDataflowResolution(astSub.getFirstParent(nodeId), ast, subTree);

        // Rebuild edges in order
        for(Integer childId : children)
            ast.removeEdge(parent, childId);
        for(Integer childId : children) {
            if(childId.equals(nodeId))
                ast.addEdge(parent, subTree);
            else
                ast.addEdge(parent, childId);
        }

        return STATUS.CHANGE;
    }

    private static void walk( String path, List<String> filelist ) {
        File root = new File( path );
        File[] list = root.listFiles();
        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() )
                walk( f.getPath(), filelist );
            else
                filelist.add(f.getPath());
        }
    }


    // Reverse encode functions
    protected static byte[] DecodeBase64(byte[] str) throws IllegalArgumentException { // base64_decode
        String s = new String(str);
        s = s.replaceAll("\n", "").replaceAll("\r", "");
        return Base64.getDecoder().decode(s);
    }

    protected static byte[] EncodeBase64(byte[] str) throws IllegalArgumentException { // base64_encode
        return Base64.getEncoder().encode(str);
    }

    protected static byte[] DecodeRot13(byte[] str) { // str_rot13
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < str.length; i++) {
            byte c = str[i];
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            outputStream.write(c);
        }
        return outputStream.toByteArray();
    }

    protected static byte[] DecodeRawUrl(byte[] str) throws UnsupportedEncodingException { // rawurldecode
        return java.net.URLDecoder.decode(new String(str), "UTF-8").getBytes();
    }

    protected static byte[] DecodeStrrev(byte[] str) throws UnsupportedEncodingException { // strrev
        byte[] out = new byte[str.length];
        for (int i = 0; i < out.length; i++)
            out[i] = str[str.length - i - 1];

        return out;
    }

    protected static byte[] InflateGZ(byte[] str) throws IOException { // gzinflate
        InputStream inflInstream = new InflaterInputStream(
                new ByteArrayInputStream(str),
                new Inflater(true)
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        shovelInToOut(inflInstream, outputStream);
        return outputStream.toByteArray();
    }

    protected static byte[] DeflateGZ(byte[] str) throws IOException { // gzdeflate
        InputStream deflInstream = new DeflaterInputStream(
                new ByteArrayInputStream(str),
                new Deflater()
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        shovelInToOut(deflInstream, outputStream);
        return outputStream.toByteArray();
    }

    protected static byte[] UncompressGZ(byte[] str) throws IOException { // gzuncompress
        InputStream bIn = new ByteArrayInputStream(str);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InflaterInputStream in = new InflaterInputStream(bIn);
        shovelInToOut(in, out);
        return out.toByteArray();
    }

    protected static byte[] CompressGZ(byte[] str) throws IOException { // gzcompress
        InputStream in = new ByteArrayInputStream(str);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        DeflaterOutputStream out = new DeflaterOutputStream(bOut);
        shovelInToOut(in, out);
        return bOut.toByteArray();
    }



    // Helper function
    private static void shovelInToOut(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }


    private void replaceSubTreeByNode(Integer rootId, String type, byte[] image) {
        Integer parent = ast.getParent(rootId).get(0);
        List<Integer> childParent = new ArrayList<>(ast.getChildren(parent));

        ast.deleteSubTree(rootId);
        ast.setNodeType(rootId, type);
        if(image != null)
            setTokenByte(rootId, image);
        // Rebuild children in order
        for(Integer childId : childParent)
            ast.removeEdge(parent, childId);
        for(Integer childId : childParent)
            ast.addEdge(parent, childId);
    }
    private void dataflowReplace(Integer rootId, String type, byte[] image) {
        replaceSubTreeByNode(rootId, type, image);
        astSub.addDataflowResolution(astSub.getFirstParent(rootId), ast, rootId);
    }
    private void dynamicReplace(Integer rootId, String type, byte[] image) {
        replaceSubTreeByNode(rootId, type, image);
        astSub.addDynamicResolution(astSub.getFirstParent(rootId), ast, rootId);
    }



    // Helper method to integrate binary data (byte[]) to the AST (as HexLiteral node)
    // encodeHexString takes binary (byte[]) and return a string "0x..." (to be used as an image of a HexLiteral node)
    // decodeHexString takes a string "0x..." and convert the hexadecimal information to a binary array byte[]

    private byte[] getTokenByte(Integer nodeId) {
        if(ast.getType(nodeId).equals("HexLiteral"))
            return decodeHexString(ast.getImage(nodeId));
        return ast.getImage(nodeId).getBytes();
    }
    private void setTokenByte(Integer nodeId, byte[] data) {
        if(ast.getType(nodeId).equals("HexLiteral"))
            ast.setNodeImage(nodeId, encodeHexString(data));
        else
            ast.setNodeImage(nodeId, new String(data));
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuilder.append(byteToHex(byteArray[i]));
        }
        return hexStringBuilder.toString();
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = Character.digit(hexString.charAt(0), 16);
        int secondDigit = Character.digit(hexString.charAt(1), 16);
        if(firstDigit == -1 || secondDigit == -1) {
            throw new IllegalArgumentException("Invalid Hexadecimal Character: "+ hexString);
        }
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException("Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    public boolean isEvalFound() {
        return evalFound;
    }

    public List<Exception> getEvalErrors() {
        return evalErrors;
    }
}
