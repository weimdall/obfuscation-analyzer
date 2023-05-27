/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.exceptions;

public class ASTDataflowException extends CodeAnalysisException {
    private static final long serialVersionUID = 1L;
    public ASTDataflowException(String errorMessage) {
        super(errorMessage);
    }
}