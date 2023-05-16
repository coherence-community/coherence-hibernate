/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.identity;

/**
* Common properties that are used by the the {@link ServerSideIdentityAsserter} and
* the {@link ClientSideIdentityTransformer}.
* @author Gunnar Hillert
*
*/
public class IdentityTokenConstants {

    /**
     * Name of the property that holds the authentication token.
     */
    public static final String JVM_TOKEN_ARGUMENT_NAME = "authenticationToken";

}
