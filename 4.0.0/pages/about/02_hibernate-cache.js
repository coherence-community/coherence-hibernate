<doc-view>

<h2 id="_coherence_hibernate_second_level_cache">Coherence Hibernate Second-Level Cache</h2>
<div class="section">
<p>This section describes how you can use <a target="_blank" href="https://coherence.community/">Oracle Coherence</a>
as a second-level cache in <a target="_blank" href="http://hibernate.org/orm/">Hibernate ORM</a>, an object-relational mapping library
for Java applications. Since version <code>2.1</code> (released December 11th 2003) Hibernate
has incorporated second-level caching, by allowing an implementation of a Service
Provider Interface (SPI) to be configured. In Hibernate version 3.3 (released
September 11th 2008) the second-level cache SPI was significantly redesigned. Over
the next couple of versions the SPI was further refined leading to breaking changes.</p>


<h3 id="_supported_hibernate_versions">Supported Hibernate Versions</h3>
<div class="section">
<p>We provide dedicated releases of the Hibernate Second-Level Cache implementations
for Oracle Coherence depending on the Hibernate versions. The following versions
are supported:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Module Name</th>
<th>Supported Hibernate Versions</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">coherence-hibernate-cache-7</td>
<td class=""><code>7.4.x</code></td>
</tr>
</tbody>
</table>
</div>

<p>The default <code>4.x</code> build uses Hibernate ORM <code>7.4.9.Final</code>. CI tests the supported <code>7.4.x</code> line with <code>7.4.1.Final</code>,
<code>7.4.5.Final</code>, and <code>7.4.9.Final</code> against each compatibility-tested Coherence version on Java 17. The complete default
dependency combination is tested separately on Java 17, 21, and 25.</p>

<div class="admonition important">
<p class="admonition-inline">Coherence Hibernate <code>4.x</code> supports Hibernate ORM <code>7.4.x</code> only. Hibernate <code>5.6.x</code> and <code>6.x</code> applications
must use the matching <code>cache-53</code> or <code>cache-6</code> adapter from the Coherence Hibernate <code>3.x</code> maintenance line.</p>
</div>

<p>The Hibernate 7 adapter supports Hibernate&#8217;s entity, collection, natural-ID, query-result, and update-timestamp
second-level cache integration points. Its compatibility tests also cover Hibernate 7 cache interactions through
stateful sessions, stateless <code>CacheMode.NORMAL</code>, <code>GET</code>, and <code>IGNORE</code> operations, multi-load operations,
<code>KeyType.NATURAL</code> (including <code>@NaturalIdClass</code>), and <code>CacheMode.REFRESH_SESSION</code>. Hibernate ORM features that do not
interact with the second-level cache provider remain the responsibility of Hibernate itself.</p>

</div>


<h3 id="_upgrading_from_coherence_hibernate_3_x">Upgrading from Coherence Hibernate 3.x</h3>
<div class="section">
<p>Replace the 3.x adapter with <code>coherence-hibernate-cache-7</code>, upgrade Hibernate ORM to <code>7.4.x</code>, and configure
<code>com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory</code>. The following constraints apply when upgrading
from either the Hibernate 6 <code>cache-6</code> adapter or the Hibernate 5.6 <code>cache-53</code> adapter.</p>

<ul class="ulist">
<li>
<p><strong>Database compatibility:</strong> The upgraded application can use the existing database, provided its mappings and schema
expectations remain compatible. This cache-provider upgrade does not itself require a separate database. Check the
Hibernate migration guides for changes affecting your application; rollback also requires compatibility with the
old application&#8217;s mappings and schema expectations.</p>

</li>
<li>
<p><strong>Cache format compatibility:</strong> Old and new Hibernate versions must not share physical Hibernate cache regions or
reuse each other&#8217;s serialized entries. Use a different <code>hibernate.cache.region_prefix</code>, or empty existing regions
before reusing them with the new version. Rebuild cache contents from the database.</p>

</li>
<li>
<p><strong>Cache-server dependencies:</strong> A region prefix separates cache names; it does not isolate Java classpaths. Storage
members need compatible libraries to deserialize entries in every region they host. Separate storage or a
coordinated storage upgrade may be required. Loading both Hibernate and adapter versions into one JVM does not
establish compatibility.</p>

</li>
<li>
<p><strong>Cache consistency:</strong> Separate prefixes do not share invalidations. A database write through either deployment can
leave the other&#8217;s cached data stale. Prefix isolation alone therefore does not make concurrent operation or a
rolling upgrade safe.</p>

</li>
<li>
<p><strong>Stale caches at handover:</strong> Warmed, retained, or persistently restored caches may be stale after the other deployment
has written to the database.</p>

</li>
<li>
<p><strong>Cutover and rollback:</strong> Unless a separately validated mechanism keeps caches consistent, stop and drain the outgoing
deployment&#8217;s traffic and background work. Also stop and drain incoming validation or warming that could refill its
caches. Clear all incoming Hibernate entity, collection, natural-ID, query-result, and update-timestamp regions
before enabling incoming traffic or background work. Keep activity paused throughout clearing. Apply the same
sequence when rolling back.</p>

</li>
</ul>

</div>


<h3 id="_stateless_session_cache_modes_in_hibernate_7">Stateless-Session Cache Modes in Hibernate 7</h3>
<div class="section">
<p>Hibernate 6 and earlier stateless sessions bypassed the second-level cache. Hibernate 7 changed the default so a
<code>StatelessSession</code> uses the configured second-level cache for cacheable entities. A stateless session still has no
first-level persistence context, but its database reads can now retrieve entries from and store entries in Coherence.
Review every <code>openStatelessSession()</code> call when upgrading, especially bulk-processing workloads that previously assumed
they could not affect the second-level cache. See the
<a target="_blank" href="https://docs.hibernate.org/orm/7.0/migration-guide/">Hibernate 7 migration guide</a> for the Hibernate behavior change.</p>

<p>Use <code>StatelessSession.setCacheMode()</code> to choose the appropriate behavior for a workload:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 16.667%;">
<col style="width: 33.333%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Cache mode</th>
<th>Coherence behavior</th>
<th>Typical use</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>NORMAL</code></td>
<td class="">Retrieve cached entries and store database results in Coherence.</td>
<td class="">Read-heavy processing intended to benefit from and warm the shared cache.</td>
</tr>
<tr>
<td class=""><code>GET</code></td>
<td class="">Retrieve existing entries from Coherence, but do not store database results after a cache miss.</td>
<td class="">Scans that may benefit from already-cached data but must not warm or flood the cache.</td>
</tr>
<tr>
<td class=""><code>IGNORE</code></td>
<td class="">Bypass both retrieval from and storage into Coherence.</td>
<td class="">Database-authoritative reconciliation, imports, exports, and large maintenance jobs.</td>
</tr>
</tbody>
</table>
</div>

<p>During migration, measure cache hit, miss, put, and eviction counts as well as network traffic and storage volume. The
new default may improve repeated reads, but an unreviewed bulk scan using <code>NORMAL</code> can also populate many entries that
Hibernate 6 would never have placed in Coherence.</p>

<p>Applications select cache modes through Hibernate&#8217;s <code>Session</code>, <code>StatelessSession</code>, and query APIs. Hibernate interprets
those modes and decides whether to invoke the second-level cache provider. Coherence implements the shared provider SPI:
region lifecycle and storage, cache keys, concurrency strategies, invalidation, and timestamps. There is no separate
stateless-session Coherence SPI, and the provider must not duplicate Hibernate&#8217;s cache-mode policy.</p>

<p>Second-level caching remains optional. When no provider is configured, Hibernate applications do not require it. When
Coherence is configured and second-level caching is enabled, Hibernate 7 uses it according to cacheable mappings and the
selected cache mode; Coherence must fully implement every provider SPI surface and concurrency strategy it advertises.</p>

</div>


<h3 id="_supported_coherence_versions">Supported Coherence Versions</h3>
<div class="section">
<p>The default Coherence Hibernate <code>4.x</code> build uses Coherence CE <code>15.1.1-0-5</code>. CI also compatibility-tests Coherence CE
<code>26.07</code>. Both versions are tested with Hibernate <code>7.4.1.Final</code>, <code>7.4.5.Final</code>, and <code>7.4.9.Final</code> on Java 17; the default
<code>15.1.1-0-5</code> and Hibernate <code>7.4.9.Final</code> combination receives the complete Java 17, 21, and 25 reactor validation.</p>

<p>Coherence and Hibernate are provided dependencies and must be declared by the application. Use one of the tested
combinations unless you independently validate a different patch level.</p>

</div>


<h3 id="_overview">Overview</h3>
<div class="section">
<p>Using Coherence as a Hibernate second-level cache implementation allows multiple JVMs running the same Hibernate
application to share a second-level cache. The use of Coherence caches in this scenario is completely controlled by
Hibernate. You should have a good understanding of Hibernate second-level caching to successfully use the Coherence
Hibernate second-level cache implementation. For more information on Hibernate second-level caching, see the
<a target="_blank" href="https://docs.hibernate.org/orm/7.4/userguide/html_single/#caching">relevant chapter on Caching</a> in the Hibernate Core Reference
Manual at <a target="_blank" href="http://www.hibernate.org/docs" class="bare">http://www.hibernate.org/docs</a>.</p>

<p>Using Coherence as a Hibernate second-level cache implementation may be a good fit for Java applications that use
Hibernate for data access and management, and that run in a cluster of application servers accessing the same database.</p>

<div class="admonition note">
<p class="admonition-inline">Before you use the Coherence Hibernate Cache support, please also consider other caching strategies. Ultimately,
you should make a decision that is most applicable to the needs of your application.</p>
</div>

</div>


<h3 id="_getting_started">Getting Started</h3>
<div class="section">
<p>Installing the Coherence Hibernate second-level cache implementation amounts to obtaining a distribution of
<code>coherence-hibernate-cache-xx-${project.version}.jar</code> for the respective Hibernate version of your application.</p>

<p>The easiest way to do so is to build and execute your Hibernate application with Maven, and add the following dependency
to your application&#8217;s <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-hibernate-cache-7&lt;/artifactId&gt;
    &lt;version&gt;${project.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>Alternatively, you can download <code>coherence-hibernate-cache-7-${project.version}.jar</code> from a Maven repository
(e.g. <a target="_blank" href="https://repo1.maven.org/maven2/" class="bare">https://repo1.maven.org/maven2/</a>) and use it in JVM classpaths. Or you can
<a target="_blank" href="../dev/03_build-instructions.adoc">build</a>
the Coherence Hibernate second-level cache implementation from sources.</p>

<p>Coherence Hibernate depends on Oracle Coherence (E.g. <a target="_blank" href="https://coherence.community/">Coherence CE</a> (Community Edition))
and Hibernate. These dependencies must be declared explicitly as we do not include them transitively. A full dependency
declaration may look like the following:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-hibernate-cache-7&lt;/artifactId&gt;
    &lt;version&gt;${project.version}&lt;/version&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;org.hibernate.orm&lt;/groupId&gt;
    &lt;artifactId&gt;hibernate-core&lt;/artifactId&gt;
    &lt;version&gt;${hibernate7.version}&lt;/version&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
    &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<div class="admonition tip">
<p class="admonition-inline">In the GitHub repository under samples, you will find a Spring Boot-based application that using Coherence Hibernate.
Please see the respective
<a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/blob/main/samples/coherence-hibernate-demo/README.adoc">README</a> for details.</p>
</div>

</div>


<h3 id="_serialization_requirements">Serialization Requirements</h3>
<div class="section">
<p>Familiarize yourself with the Coherence Documentation, especially the chapter on
<a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/performing-basic-cache-operations.html#GUID-F9BCA574-ABFC-4F0D-94EA-949E5B7621E7">Performing Basic Cache Operations</a>
as it also details the <strong>Requirements for Cached Objects</strong>:</p>

<p>Cache keys and values must be serializable (for example, <code>java.io.Serializable</code> or Coherence <a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/using-portable-object-format.html#GUID-F331E5AB-0B3B-4313-A2E3-AA95A40AD913">Portable Object Format</a>
(POF) serialization). Furthermore, cache keys must provide an implementation of</p>

<ul class="ulist">
<li>
<p><code>hashCode()</code> and</p>

</li>
<li>
<p><code>equals()</code></p>

</li>
</ul>

<p>Those methods must return consistent results across cluster nodes. This implies that the implementation of <code>hashCode()</code>
and <code>equals()</code> must be based solely on the object&#8217;s serializable state (that is, the object&#8217;s non-transient fields). Most
built-in Java types, such as <code>String</code>, <code>Integer</code> and <code>Date</code>, meet this requirement. Some cache implementations
(specifically the partitioned cache) use the serialized form of the key objects for equality testing, which means that
keys for which <code>equals()</code> returns <code>true</code> must serialize identically. Most built-in Java types meet this requirement as
well.</p>

</div>


<h3 id="_cache_keys">Cache Keys</h3>
<div class="section">
<p>Coherence Hibernate uses Hibernate&#8217;s <code>DefaultCacheKeysFactory</code> unless <code>hibernate.cache.keys_factory</code> is configured.
The default preserves tenant and entity metadata in cache keys and supports composite identifiers.
Although Hibernate deprecates this setting because support depends on the cache provider, Coherence Hibernate
continues to honor it. No replacement property is required.</p>

<p>The setting accepts <code>default</code>, <code>simple</code>, the fully qualified name of a class implementing
<code>org.hibernate.cache.spi.CacheKeysFactory</code>, or (through programmatic configuration) a factory class or instance.
For example, a custom factory with a public no-argument constructor can be configured in <code>hibernate.properties</code>:</p>

<markup
lang="properties"

>hibernate.cache.keys_factory=com.example.CustomCacheKeysFactory</markup>

<p>An invalid factory setting causes startup to fail. When no setting is supplied, the provider uses
<code>DefaultCacheKeysFactory.INSTANCE</code>.
Use <code>simple</code> only when cache regions separate entity types and collection roles and the application does not use
multi-tenancy: simple entity and collection keys omit that identifying metadata.
Custom factories must produce keys compatible with the configured Coherence serializer, with consistent equality
and hash codes across cluster members. All applications sharing a cache region must use compatible key factories;
evict existing entries when changing the key format.</p>

</div>


<h3 id="_configuring_clients_and_servers_for_hibernate_second_level_caching">Configuring Clients and Servers for Hibernate Second-Level Caching</h3>
<div class="section">
<p>Both the clients of the Coherence Hibernate second-level caches&#8201;&#8212;&#8201;e.g. application server JVMs running Hibernate-based
applications&#8201;&#8212;&#8201;and the Coherence cache server JVMs actually holding the cache contents need to have a common set of
jar file artifacts available to their ClassLoaders. Specifically, both need
<code>coherence-hibernate-cache-xx-${project.version}.jar</code> and its dependencies Coherence and Hibernate
(and their dependencies).</p>

<p>The Coherence cache server JVMs need the Hibernate core jar file to deserialize <code>CacheEntry</code> classes
(<code>org.hibernate.cache.spi.entry.*</code>) since the Coherence Hibernate second-level cache implementation uses Coherence
<a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/processing-data-cache.html">EntryProcessors</a> to optimize concurrency control.
However, the cache server JVMs do not need the Hibernate application&#8217;s jar files containing entity classes etc.</p>

<p>The client / application server JVMs do of course need the Hibernate application&#8217;s jar files containing entity classes
etc.</p>

<p>When configuring Coherence you should also consider the following two points in regard to storage-enabled cache server JVMs:</p>

<ul class="ulist">
<li>
<p>If there is no separate tier of storage-enabled cache server JVMs in the Coherence cluster, then having application JVMs
be storage-enabled is feasible, at the cost of increased heap utilization (by second-level cache contents) in those JVMs</p>

</li>
<li>
<p>If there is a separate tier of storage-enabled cache server JVMs in the Coherence cluster, then application JVMs should
be storage-disabled cluster members or remote clients of Coherence*Extend or gRPC proxy servers.</p>

</li>
</ul>

<p>See the comments in the default <code>hibernate-second-level-cache-config.xml</code> for details on how to accomplish the relevant
configuration. It amounts to enabling/disabling local storage by making changes to the cache configuration files, or by
passing <code>–Dtangosol.coherence.distributed.localstorage=false</code> to client JVMs.</p>

<div class="admonition tip">
<p class="admonition-inline">You can specify Coherence property overrides via Hibernate properties.
E.g. <code>com.oracle.coherence.hibernate.cache.coherence_properties.tangosol.coherence.distributed.localstorage=false</code></p>
</div>

<p>Both client and server JVMs will need the same Coherence operational configuration specifying necessary cluster
communication parameters. See the chapter on
<a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/understanding-configuration.html#GUID-360B798E-2120-44A9-8B09-1FDD9AB40EB5">Understanding Configuration</a>
in the reference documentation. Coherence provides default operational configuration, but it is a best practice to
override communication parameters and cluster name to make them unique for each separate application environment.</p>

</div>


<h3 id="_configuring_hibernate_second_level_and_query_caching">Configuring Hibernate Second-Level and Query Caching</h3>
<div class="section">
<p>Hibernate uses three forms of caching:</p>

<ul class="ulist">
<li>
<p>Session cache</p>

</li>
<li>
<p>Second-level cache</p>

</li>
<li>
<p>Query cache</p>

</li>
</ul>

<p>The <em>session cache</em> caches entities within a Hibernate Session. A Hibernate Session is a transaction-level cache of
persistent data, potentially spanning multiple database transactions, and typically scoped on a per-thread basis. As a
non-clustered cache, the session cache is managed entirely by Hibernate.</p>

<p>The <em>second-level</em> and <em>query caches</em> span multiple transactions, and support the use of Coherence as a cache provider.
The second-level cache is responsible for caching records across multiple Sessions (for primary key lookups). The <em>query
cache</em> caches the result sets generated by Hibernate queries. Hibernate manages data in an internal representation in the
second-level and query caches, meaning that these caches are usable only by Hibernate. For more information, see the
chapter on <a target="_blank" href="https://docs.hibernate.org/orm/7.4/userguide/html_single/#caching">Caching</a> of the Hibernate Core Reference Manual.</p>


<h4 id="_hibernate_second_level_cache">Hibernate Second-Level Cache</h4>
<div class="section">
<p>To configure Coherence as the Hibernate <em>second-level</em> cache, set the <code>hibernate.cache.region.factory_class</code>
property in Hibernate configuration to <code>com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory</code>. For example,
include the following property setting in <code>hibernate.cfg.xml</code>:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.region.factory_class"&gt;
    com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory
&lt;/property&gt;</markup>

<p>In addition to setting the <code>hibernate.cache.region.factory_class</code> property, you must also configure Hibernate to use
second-level caching by setting the appropriate Hibernate configuration property to <code>true</code>, as follows:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.use_second_level_cache"&gt;true&lt;/property&gt;</markup>

<p>Configure each entity and collection that should use second-level caching. For example:</p>

<markup
lang="java"

>@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="PEOPLE")
public class Person {
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "person")
    private Set&lt;Event&gt; events;
    // ...
}</markup>

<p>Supported <code>CacheConcurrencyStrategy</code> values are described below.</p>

</div>


<h4 id="_hibernate_query_cache">Hibernate Query Cache</h4>
<div class="section">
<p>When configuring query caching, you must again set the Hibernate property <code>hibernate.cache.region.factory_class</code> property.
Furthermore, you must also configure Hibernate to enable query caching by setting the following Hibernate configuration
property to <code>true</code>:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.use_query_cache"&gt;true&lt;/property&gt;</markup>

<p>Moreover, you must call <code>setCacheable(true)</code> on each cacheable <code>SelectionQuery</code>, as in
the following example:</p>

<markup
lang="java"

>public List&lt;Person&gt; listPersons() {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    SelectionQuery&lt;Person&gt; query = session.createSelectionQuery("from Person", Person.class);
    query.setCacheable(true);
    List&lt;Person&gt; result = query.getResultList();
    session.getTransaction().commit();
    return result;
}</markup>

</div>

</div>


<h3 id="_types_of_hibernate_second_level_cache">Types of Hibernate Second-Level Cache</h3>
<div class="section">
<p>Hibernate&#8217;s second-level cache design uses domain-data, query-result, and timestamp regions:</p>

<ul class="ulist">
<li>
<p><code>DomainDataRegion</code> instances cache entity, collection, and natural-id data. By default, entity region names use the
fully-qualified entity name, while collection region names append the collection attribute name.</p>

</li>
<li>
<p><code>QueryResultsRegion</code> instances cache the result sets of queries executed by Hibernate. Cache keys are formed using the query
string and parameters, and cache values are collections of identifiers of entities satisfying the query.  By default
Hibernate uses one <code>QueryResultsRegion</code> with the name "<code>default-query-results-region</code>". Hibernate
users can instantiate query-result regions by calling <code>SelectionQuery.setCacheRegion()</code> with custom cache names
(by convention these names should begin with "<code>query.</code>").</p>

</li>
<li>
<p><code>TimestampsRegion</code> instances cache timestamps at which database tables were last written by Hibernate. These timestamps are
used by Hibernate during query processing to determine whether cached query results can be used (if a query involves a
certain table, and that table was written more recently than when the result set for that query was last cached, then
the cached result set may be stale and cannot be used).  Hibernate uses one <code>TimestampsRegion</code> named
&#8220;`default-update-timestamps-region`&#8221;. The keys in this cache are database table names, and the values are
machine clock readings.</p>

</li>
</ul>

<p>Entity, collection, and natural-id data use configurable cache concurrency strategies. Query-result and timestamp
regions are general-data regions, where cache concurrency strategies do not apply.</p>

</div>


<h3 id="_cache_concurrency_strategies">Cache Concurrency Strategies</h3>
<div class="section">
<p>The Hibernate cache architecture defines four different "cache concurrency strategies" in association with its
<em>second-level</em> cache. These are intended to allow Hibernate users to configure the degree of database consistency and
transaction isolation desired for <em>second-level</em> cache contents, for data concurrently read and written through Hibernate.
The following table describes the four Hibernate second-level cache concurrency strategies:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 20%;">
<col style="width: 40%;">
<col style="width: 40%;">
</colgroup>
<thead>
<tr>
<th>Strategy</th>
<th>Intent</th>
<th>Write Transaction Sequence</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">transactional</td>
<td class="">Guarantee cache consistency with database, and repeatable read isolation, via JTA transactions enlisting both as resources.</td>
<td class="">Cache and database committed atomically in same JTA transaction.</td>
</tr>
<tr>
<td class="">read/write</td>
<td class="">Maintain strong consistency with database, and read committed isolation in second-level cache.</td>
<td class="">Database committed first, then cache updated using locking model.</td>
</tr>
<tr>
<td class="">nonstrict read/write</td>
<td class="">Better performance, but no guarantee of consistency with database or read committed isolation in second-level cache.</td>
<td class="">Database committed first, then cache invalidated to cause subsequent read-through.</td>
</tr>
<tr>
<td class="">read only</td>
<td class="">Best performance for read-only data.</td>
<td class="">Not applicable.</td>
</tr>
</tbody>
</table>
</div>

<p>For <code>EntityRegions</code>, <code>CollectionRegions</code>, and <code>NaturalIdRegions</code>, the appropriate cache concurrency strategy can be
configured via the usage attribute of the cache element in the Hibernate mapping file for a mapped entity class, or via
equivalent annotation.</p>

<div class="admonition important">
<p class="admonition-inline">The Coherence Hibernate second-level cache implementation does not support the transactional cache concurrency strategy.</p>
</div>

</div>


<h3 id="_coherence_cache_configuration">Coherence Cache Configuration</h3>
<div class="section">
<p>By default, the Coherence Hibernate second-level cache implementation uses a cache configuration file named
<code>hibernate-second-level-cache-config.xml</code> at the root level in <code>coherence-hibernate-cache-7-${project.version}.jar</code>.
This configuration file defines cache mappings for Hibernate second-level caches. You can specify an alternative cache
configuration file for Hibernate second-level caches using the Hibernate or Java property
<code>com.oracle.coherence.hibernate.cache.cache_config_file_path</code>, whose value should be the path to a file or ClassLoader
resource, or a <code>file://</code> URL.</p>

<p>In fact, it is recommended and expected that you specify an alternative cache configuration file customized for the
domain model and consistency / isolation requirements of your particular Hibernate application. For each mapped entity
class and Collection-typed field, it is recommended that you configure an explicit cache mapping to the scheme (with
expiry and size parameters) appropriate for that cache given application requirements. See comments in the default
cache configuration file for more detail on customizing cache configuration for your application. The default cache
configuration file takes a conservative approach, and it is likely that you can optimize cache access latency and hit
ratio (via size) for entity and collection caches with relaxed consistency / isolation requirements.</p>

<p>In any case, it is recommended that you configure dedicated cache services for Hibernate second-level caches (as is done
in the default cache configuration file), to avoid the potential for reentrant calls into cache services when
Hibernate-based <code>CacheStores</code> are used. Furthermore, second-level caches should be size-limited in all tiers to avoid
the possibility of heap exhaustion. Query caches in particular should be size-limited because the Hibernate API does
not provide any means of controlling the query cache other than a complete eviction. Finally, expiration should be
considered if the underlying database can be written by clients other than the Hibernate application.</p>

</div>


<h3 id="_additional_configuration_options">Additional Configuration Options</h3>
<div class="section">

<h4 id="_session_name">Session Name</h4>
<div class="section">
<p>Property <code>com.oracle.coherence.hibernate.cache.session_name</code> allows to specify a name for the
underlying Coherence session. If not specified, the default session name will be used. Named sessions are available on
all supported Coherence versions.</p>

</div>


<h4 id="_session_type">Session Type</h4>
<div class="section">
<p>Using property <code>com.oracle.coherence.hibernate.cache.session_type</code> you can specify the type of the session. By default,
the session type is <code>server</code> which means that the Coherence Hibernate application becomes a node in the Coherence cluster
using the Tangosol Cluster Management Protocol (TCMP). Please see the chapter Introduction to Coherence Clusters of
the Coherence reference guide for more details.</p>

<p>The session is created with Coherence&#8217;s <code>ClusterMember</code> mode for <code>server</code> (or an unspecified type), and <code>Client</code> mode
for <code>client</code>. This also supplies the corresponding <code>coherence.client</code> parameter to cache configurations that use it
to select their cache schemes.</p>

<ul class="ulist">
<li>
<p>Client</p>

</li>
<li>
<p>Server (default)</p>

</li>
</ul>

<p>If, on the other hand, you would like to connect to Coherence in strict client mode using either Coherence*Extend or
gRPC, you will need to set the session type to <code>client</code>. In that case, the Coherence Hibernate application will not use
TCMP.</p>

<div class="admonition tip">
<p class="admonition-inline">When using Coherence Hibernate in pure client mode, please also set the Coherence property
<code>coherence.tcmp.enabled</code> to <code>false</code>, either via System property of the custom Hibernate property:
<code>com.oracle.coherence.hibernate.cache.coherence_properties.coherence.tcmp.enabled: false</code>.</p>
</div>

</div>


<h4 id="_start_full_cache_server">Start full Cache Server</h4>
<div class="section">
<p>By default, Coherence Hibernate starts a minimal Coherence cluster node without starting any additional services. Set
<code>com.oracle.coherence.hibernate.cache.start_cache_server</code> to <code>true</code> to start Coherence using the <code>DefaultCacheServer</code>.
That way a fully featured Coherence Cluster node is started, allowing for the configuration of e.g. Management over
REST, which allows for convenient introspection of the Cluster node and its caches using the
<a target="_blank" href="https://github.com/oracle/coherence-cli">Coherence CLI</a>.</p>

<div class="admonition note">
<p class="admonition-inline">This option is ignored if you set <code>com.oracle.coherence.hibernate.cache.session_type</code> to <code>client</code>.</p>
</div>

</div>


<h4 id="_minimal_puts">Minimal Puts</h4>
<div class="section">
<p>Hibernate provides the configuration property <code>hibernate.cache.use_minimal_puts</code>, which optimizes cache management for
clustered caches by minimizing cache update operations. The Coherence caching provider enables this by default. Setting
this property to false might increase overhead for cache management.</p>

</div>


<h4 id="_coherence_specific_properties">Coherence-specific properties</h4>
<div class="section">
<p>When providing Hibernate properties, you can also specify any
<a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/system-property-overrides.html#GUID-32230D28-4976-4147-A887-0A0120FF5C7E">Coherence system property overrides</a>
using the following property structure:</p>

<markup
lang="properties"

>com.oracle.coherence.hibernate.cache.coherence_properties.*=my property value</markup>

<p>For instance, in order to redirect the logging output of Coherence (Only Coherence!) to its own log file,
and setting the log level to maximum, you could specify:</p>

<markup
lang="properties"

>com.oracle.coherence.hibernate.cache.coherence_properties.coherence.log=/path/to/coherence.log
com.oracle.coherence.hibernate.cache.coherence_properties.coherence.log.level: 9</markup>

</div>


<h4 id="_logging">Logging</h4>
<div class="section">
<p>Without specifying any custom logging properties, Coherence Hibernate will set the logger of Coherence to
<code>slf4j</code>. Therefore, Coherence Hibernate should integrate seamlessly into your application out of the box.</p>

<p>Under the covers, Coherence Hibernate is configured using a custom implementation of a Coherence <code>SystemPropertyResolver</code>.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Properties defined via
<a target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/15.1.1/develop-applications/operational-configuration-elements.html#GUID-6DEB2F17-F6CA-4476-8EF7-2B175191929F">Operational Override Files</a>
take precedence. For example, if your application provides a custom <code>tangosol-coherence-override.xml</code> file,
such as the following, then providing a respective Coherence Hibernate property will not have any effect.</p>
</p>
</div>

<markup
lang="xml"

>&lt;logging-config&gt;
    &lt;destination&gt;slf4j&lt;/destination&gt;
&lt;/logging-config&gt;</markup>

</div>

</div>

</div>

</doc-view>
