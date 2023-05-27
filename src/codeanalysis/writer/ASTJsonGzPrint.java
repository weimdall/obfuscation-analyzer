package org.polymtl.codeanalysis.writer;

import org.polymtl.codeanalysis.model.*;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class ASTJsonGzPrint extends ASTJsonPrint {
    public ASTJsonGzPrint(AST ast, File f) throws IOException {
        FileOutputStream outStr = new FileOutputStream(f);
        GZIPOutputStream gzipTokenJsonFile = new GZIPOutputStream(outStr);
        writer = new PrintWriter(gzipTokenJsonFile);
        this.ast = ast;
    }
}
