/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration.session;

/**
 * An enum representing the different types of {@link com.tangosol.net.SessionConfiguration}
 * that can be configured in the Coherence Hibernate configuration.
 *
 * @author Gunnar Hillert
 * @since 2.1
 */
public enum SessionType {

	/**
	 * The session is a client session, that is it expects to be a Coherence*Extend client.
	 */
	CLIENT,

	/**
	 * The session is a gRPC client session.
	 */
	GRPC,

	/**
	 * The session is a server session, that is it expects to be a Coherence cluster member.
	 * <p>This is the default type if none is specified for a configuration.
	 */
	SERVER

}
