/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.configuration.session;

/**
 * An enum representing the different types of Coherence instances we are dealing with. Depending on whether we configure
 * the Coherence Hibernate application to be a Cluster member itself ({@link SessionType#SERVER}) or if we connect to
 * the cluster via Coherence*Extend ({@link SessionType#CLIENT}), we need to set the correct {@link SessionType}.
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
	 * The session is a server session, that is it expects to be a Coherence cluster member. This is the default type if
	 * none is specified for a configuration.
	 */
	SERVER

}
