/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.exceptions;

import org.json.JSONException;

public class CodeAnalysisException extends JSONException {
    private static final long serialVersionUID = 1L;
    public CodeAnalysisException(String errorMessage) {
        super(errorMessage);
    }
}