/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.identity;

import javax.security.auth.Subject;
import com.tangosol.net.Service;
import com.tangosol.net.security.IdentityTransformer;

/**
*
* @author Gunnar Hillert
*
*/
public class ClientSideIdentityTransformer implements IdentityTransformer {
	public Object transformIdentity(Subject subject, Service service) throws SecurityException {
		return System.getProperty(IdentityTokenConstants.JVM_TOKEN_ARGUMENT_NAME);
	}
}
