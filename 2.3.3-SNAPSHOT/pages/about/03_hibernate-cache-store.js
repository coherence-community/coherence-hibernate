<doc-view>

<h2 id="_coherence_hibernate_cachestore">Coherence Hibernate CacheStore</h2>
<div class="section">
<p>This page describes how you can use Hibernate as the implementation of a Coherence <code>CacheStore</code>.</p>

<p>Using <a id="" title="" target="_blank" href="https://hibernate.org/orm/">Hibernate</a> as the implementation of a Coherence <code>CacheStore</code> may be a good fit for Java applications that use
Coherence APIs for data access and management, whose cache entries are objects or graphs appropriate for mapping
to relational tables via Hibernate, and that have simple transactional requirements (e.g. transactions affecting a
single cache entry at a time).</p>


<h3 id="_installing_the_coherence_hibernate_cachestore">Installing the Coherence Hibernate CacheStore</h3>
<div class="section">
<p>Installing the Coherence Hibernate CacheStore implementation amounts to obtaining a distribution of
<code>coherence-hibernate-cache-store-2.3.3-SNAPSHOT.jar</code> and making it available to JVM ClassLoaders.  The easiest way to do
so is to build and execute your Hibernate application with Maven, and add the following dependency to your application&#8217;s
<code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-hibernate-cache-store&lt;/artifactId&gt;
    &lt;version&gt;2.3.3-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>Alternatively, you can download <code>coherence-hibernate-cache-store-2.3.3-SNAPSHOT.jar</code> from a Maven repository
(e.g. <a id="" title="" target="_blank" href="https://repo1.maven.org/maven2/com/oracle/coherence/hibernate/coherence-hibernate-cache-store/">https://repo1.maven.org/maven2/com/oracle/coherence/hibernate/coherence-hibernate-cache-store/</a>) and use the respective
jars manually in your application&#8217;s JVM classpath.</p>

<div class="admonition tip">
<p class="admonition-inline">If you prefer building the project from source, please check out the
<a id="" title="" target="_blank" href="../dev/03_build-instructions.adoc">build instructions</a>.</p>
</div>
<div class="admonition important">
<p class="admonition-inline">The Coherence Hibernate CacheStore implementation depends at runtime on Oracle
Coherence and Hibernate. These dependencies are most easily managed using Maven (Or Gradle), but you must explicitly
declare those dependencies as do not transitively include them.</p>
</div>
</div>

<h3 id="_hibernate_configuration_requirements">Hibernate Configuration Requirements</h3>
<div class="section">
<p>Hibernate entities written and read via the Hibernate CacheStore module must use the <code>assigned</code> ID generator
in Hibernate, and also have a defined ID property.</p>

<p>Disable the <code>hibernate.hbm2ddl.auto</code> property in the <code>hibernate.cfg.xml</code> file used by the <code>HibernateCacheStore</code> module
to avoid excessive schema updates and possible deadlocks when starting a Coherence cluster with multiple storage members.</p>

</div>

<h3 id="_configuring_a_hibernatecachestore_constructor">Configuring a HibernateCacheStore Constructor</h3>
<div class="section">
<p>The following examples illustrate how to configure a simple <code>HibernateCacheStore</code> constructor, which accepts only an
entity name. This configures Hibernate by using the default configuration path, which looks for a <code>hibernate.cfg.xml</code>
file in the class path. You can also include a resource name or file specification for the <code>hibernate.cfg.xml</code> file as
the second <code>&lt;init-param&gt;</code> (set the <code>&lt;param-type&gt;</code> element to <code>java.lang.String</code> for a resource name and <code>java.io.File</code>
for a file specification). See the Javadoc for <code>HibernateCacheStore</code> for more information.</p>

<p>The following example illustrates a simple <code>coherence-cache-config.xml</code> file used to define a NamedCache cache object
named <code>TableA</code> that caches instances of a Hibernate entity (<code>com.company.TableA</code>). To define more entity caches, add
additional <code>&lt;cache-mapping&gt;</code> elements.</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;TableA&lt;/cache-name&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
      &lt;init-params&gt;
        &lt;init-param&gt;
          &lt;param-name&gt;entityname&lt;/param-name&gt;
          &lt;param-value&gt;com.company.TableA&lt;/param-value&gt;
        &lt;/init-param&gt;
      &lt;/init-params&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;read-write-backing-map-scheme&gt;
          &lt;internal-cache-scheme&gt;
            &lt;local-scheme&gt;&lt;/local-scheme&gt;
          &lt;/internal-cache-scheme&gt;
          &lt;cachestore-scheme&gt;
            &lt;class-scheme&gt;
              &lt;class-name&gt;
              com.oracle.coherence.hibernate.cachestore.HibernateCacheStore
              &lt;/class-name&gt;
              &lt;init-params&gt;
                &lt;init-param&gt;
                  &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                  &lt;param-value&gt;{entityname}&lt;/param-value&gt;
                &lt;/init-param&gt;
              &lt;/init-params&gt;
            &lt;/class-scheme&gt;
          &lt;/cachestore-scheme&gt;
        &lt;/read-write-backing-map-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<p>The next example illustrates that you can also use the predefined <code>The next example illustrates that you can also use the predefined `+{cache-name}+` macro to eliminate the need for the
`<init-params>` portion of the cache mapping.</code> macro to eliminate the need for the
<code>&lt;init-params&gt;</code> portion of the cache mapping.</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;TableA&lt;/cache-name&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;read-write-backing-map-scheme&gt;
          &lt;internal-cache-scheme&gt;
            &lt;local-scheme&gt;&lt;/local-scheme&gt;
          &lt;/internal-cache-scheme&gt;

          &lt;cachestore-scheme&gt;
            &lt;class-scheme&gt;
              &lt;class-name&gt;
              com.oracle.coherence.hibernate.cachestore.HibernateCacheStore
              &lt;/class-name&gt;
              &lt;init-params&gt;
                &lt;init-param&gt;
                  &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                  &lt;param-value&gt;com.company.{cache-name}&lt;/param-value&gt;
                &lt;/init-param&gt;
              &lt;/init-params&gt;
            &lt;/class-scheme&gt;
          &lt;/cachestore-scheme&gt;
        &lt;/read-write-backing-map-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<p>The final example illustrates that, if naming conventions allow, the mapping can be completely generalized to enable a
cache mapping for any qualified class name (entity name).</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;com.company.*&lt;/cache-name&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;read-write-backing-map-scheme&gt;
          &lt;internal-cache-scheme&gt;
            &lt;local-scheme&gt;&lt;/local-scheme&gt;
          &lt;/internal-cache-scheme&gt;

          &lt;cachestore-scheme&gt;
            &lt;class-scheme&gt;
              &lt;class-name&gt;
              com.oracle.coherence.hibernate.cachestore.HibernateCacheStore
              &lt;/class-name&gt;
              &lt;init-params&gt;
                &lt;init-param&gt;
                  &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                  &lt;param-value&gt;{cache-name}&lt;/param-value&gt;
                &lt;/init-param&gt;
              &lt;/init-params&gt;
            &lt;/class-scheme&gt;
          &lt;/cachestore-scheme&gt;
        &lt;/read-write-backing-map-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

</div>

<h3 id="_creating_a_custom_hibernate_based_cachestore">Creating a Custom Hibernate-Based CacheStore</h3>
<div class="section">
<p>While the provided <code>HibernateCacheStore</code> module provides a solution for most entity-based caches, there may be cases where
an application-specific, Hibernate-based <code>CacheStore</code> module is necessary. For example, for providing parameterized queries,
or including or post-processing query results.</p>

<p>Care must be taken in this scenario to avoid causing re-entrant calls into Coherence cache services, which could be
possible (depending on service names) if Hibernate is also configured to use the Coherence-based second-level cache
implementation.  Therefore, all methods in a custom Hibernate-based <code>CacheLoader</code> or <code>CacheStore</code> implementation should
be careful to call the Hibernate <code>Session.setCacheMode(CacheMode.IGNORE)</code> method to disable cache access. Better yet,
the Hibernate configuration used by the custom Hibernate-based <code>CacheStore</code> should disable second-level caching.</p>

<p>In some cases, you may want to extend the provided <code>HibernateCacheStore</code> with application-specific functionality.
The most obvious reason for this is to take advantage of a preexisting, programmatically configured <code>SessionFactory</code>
instance. But note that it is possible to inject a pre-configured <code>SessionFactory</code> instance into the provided
<code>HibernateCacheStore</code> via Spring integration.</p>

</div>

<h3 id="_jdbc_isolation_level">JDBC Isolation Level</h3>
<div class="section">
<p>In cases where all access to a database is through Coherence, cache store modules naturally enforce ANSI-style repeatable
read isolation as read operations, and write operations are executed serially on a per-key basis (by using the Partitioned
Cache Service). Increasing database isolation above the repeatable read level does not yield increased isolation because
cache store operations might span multiple partitioned cache nodes (and thus multiple database transactions). Using
database isolation levels below the repeatable read level does not result in unexpected anomalies, and might reduce
processing load on the database server.</p>

</div>

<h3 id="_fault_tolerance_for_hibernate_cache_store_operations">Fault-Tolerance for Hibernate Cache Store Operations</h3>
<div class="section">
<p>For single-cache-entry updates, cache store operations are fully fault-tolerant in that the cache and database are
guaranteed to be consistent during any server failure (including failures during partial updates). While the mechanisms
for fault-tolerance vary, this is true for both write-through and write-behind caches.</p>

<p>Coherence does not support two-phase cache store operations across multiple cache store instances. In other words, if
two cache entries are updated, triggering calls to cache store modules sitting on separate servers, it is possible for
one database update to succeed and for the other to fail. In this case, you might want to use a cache-aside architecture
(updating the cache and database as two separate components of a single transaction) with the application server
transaction manager. In many cases, it is possible to design the database schema to prevent logical commit failures
(but obviously not server failures). Write-behind caching avoids this issue because put operations are not affected by
database behavior (and the underlying issues have been addressed earlier in the design process).</p>

</div>

<h3 id="_using_fully_cached_data_sets">Using Fully Cached Data Sets</h3>
<div class="section">
<p>There are two scenarios where using fully cached data sets would be advantageous. One is when you are performing
distributed queries on the cache; the other is when you want to provide continued application processing despite a
database failure.</p>

<p>Distributed queries offer the potential for lower latency, higher throughput, and less database server load, as opposed
to executing queries on the database server. For set-oriented queries, the data set must be entirely cached to produce
correct query results. More precisely, for a query issued against the cache to produce correct results, the query must
not depend on any uncached data.</p>

<p>Distributed queries enable you to create hybrid caches. For example, it is possible to combine two uses of NamedCache:
a fully cached size-limited data set for querying (for example, the data for the most recent week), and a partially
cached historical data set used for singleton read operations. This approach avoids data duplication and minimizes
memory usage.</p>

<p>While fully cached data sets are usually bulk-loaded during application startup (or on a periodic basis), cache store
integration can be used to ensure that both cache and database are kept fully synchronized.</p>

<p>Another reason for using fully cached data sets is to provide the ability to continue application processing even if the
underlying database fails. Using write-behind caching extends this mode of operation to support full read-write
applications. With write-behind, the cache becomes (in effect) the temporary system of record. Should the database fail,
updates are queued in Coherence until the connection is restored. At this point, all cache changes are sent to the database.</p>

</div>

<h3 id="_api_for_hibernatecachestore_and_hibernatecacheloader">API for HibernateCacheStore and HibernateCacheLoader</h3>
<div class="section">
<p>The Oracle Coherence Hibernate Integration project includes a default entity-based <code>CacheStore</code> implementation,
<code>HibernateCacheStore</code>, and a corresponding <code>CacheLoader</code> implementation, <code>HibernateCacheLoader</code>, in the
<code>com.oracle.coherence.hibernate.cachestore</code> package.</p>

<p>The following table describes the different constructors for the <code>HibernateCacheStore</code> and <code>HibernateCacheLoader</code>
classes. For more detailed technical information, see the <a id="" title="" target="_blank" href="https://hibernate.coherence.community/2.3.3-SNAPSHOT/api/com/oracle/coherence/hibernate/cachestore/package-summary.html">Javadoc</a> for these classes:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Constructor</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">HibernateCacheLoader() and HibernateCacheStore()</td>
<td class="">These constructors are the default constructors for creating a new instance of a cache loader or cache store. They do not create a Hibernate SessionFactory object. To inject a Hibernate SessionFactory object after you use these constructors, call the setSessionFactory() method.</td>
</tr>
<tr>
<td class="">HibernateCacheLoader(java.lang.String entityName) and HibernateCacheStore(java.lang.String entityName)</td>
<td class="">These constructors create a Hibernate SessionFactory object using the default Hibernate configuration (hibernate.cfg.xml) in the classpath.</td>
</tr>
<tr>
<td class="">HibernateCacheStore(java.lang.String entityName, java.lang.String sResource) and HibernateCacheStore(java.lang.String entityName, java.lang.String sResource)</td>
<td class="">These constructors create a Hibernate SessionFactory object based on the configuration file provided (sResource).</td>
</tr>
<tr>
<td class="">HibernateCacheLoader(java.lang.String entityName, java.io.File configurationFile) and HibernateCacheStore(java.lang.String entityName, java.io.File configurationFile)</td>
<td class="">These constructors create a Hibernate SessionFactory object based on the configuration file provided (configurationFile).</td>
</tr>
<tr>
<td class="">HibernateCacheStore(java.lang.String entityName, org.hibernate.SessionFactory sFactory) and HibernateCacheStore(java.lang.String entityName, org.hibernate.SessionFactory sFactory)</td>
<td class="">These constructors accept an entity name name and a Hibernate SessionFactory.</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>

<h2 id="_sample">Sample</h2>
<div class="section">
<p>For an example please see take a look at the Coherence Spring JPA Repository CacheStore Demo.</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://github.com/coherence-community/coherence-spring/tree/main/samples/cachestore-demo">https://github.com/coherence-community/coherence-spring/tree/main/samples/cachestore-demo</a></p>

</li>
</ul>
</div>
</doc-view>