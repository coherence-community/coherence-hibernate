/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.identity;

import javax.security.auth.Subject;

import com.tangosol.net.Service;
import com.tangosol.net.security.IdentityAsserter;

/**
 * Implementation of the {@link IdentityAsserter} that validates a token in order to establish a user's identity.
 * @author Gunnar Hillert
 */
public class ServerSideIdentityAsserter implements IdentityAsserter {

    public Subject assertIdentity(Object identityToken, Service service) throws SecurityException {
        if (identityToken instanceof String) {
            if (((String) identityToken).equals(System.getProperty(IdentityTokenConstants.JVM_TOKEN_ARGUMENT_NAME))) {
                return null;
            }
        }
        throw new SecurityException("Access denied");
    }
}
