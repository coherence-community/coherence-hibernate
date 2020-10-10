Welcome to the Oracle Coherence Hibernate Project
=============================================

[![Build Status](https://github.com/ghillert/coherence-hibernate/workflows/CI%20Coherence%20Hibernate/badge.svg)](https://github.com/ghillert/coherence-hibernate/actions)

The Oracle Coherence Hibernate project implements integration points between Oracle Coherence/[Coherence CE](https://coherence.community) and the [Hibernate Object/Relational Mapping (ORM)](https://hibernate.org/orm/) framework.

Two primary integration points are covered currently:

- using Coherence as a [second-level cache](https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#caching) in Hibernate
- using Hibernate as a Coherence CacheStore implementation

Due to Hibernate Cache API changes, Coherence Hibernate `2.0.0` provides dedicated Maven modules targeting those Hibernate versions:

* `coherence-hibernate-cache-4` supports Hibernate `4.3.x`
* `coherence-hibernate-cache-5` supports Hibernate `5.0.x` and `5.1.x`
* `coherence-hibernate-cache-52` supports Hibernate `5.2.x`

Detailed project documentation for the latest release (2.0.0) is available
here: [http://coherence-community.github.io/coherence-hibernate/2.0.0/](http://coherence-community.github.io/coherence-hibernate/2.0.0/)

> **NOTE**
>
> The [Apache Maven](http://maven.apache.org)-based snapshots and releases for this project are published to Maven Central.
