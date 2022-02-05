<doc-view>

<h2 id="_coherence_hibernate_second_level_cache">Coherence Hibernate Second-Level Cache</h2>
<div class="section">
<p>This section describes how you can use <a id="" title="" target="_blank" href="https://coherence.community/">Oracle Coherence</a>
as a second-level cache in <a id="" title="" target="_blank" href="http://hibernate.org/orm/">Hibernate ORM</a>, an object-relational mapping library
for Java applications. Since version <code>2.1</code> (released December 11th 2003) Hibernate
has incorporated second-level caching, by allowing an implementation of a Service
Provider Interface (SPI) to be configured. In Hibernate version 3.3 (released
September 11th 2008) the second-level cache SPI was significantly redesigned. Over
the next couple of versions the SPI was further refined leading to breaking changes.</p>


<h3 id="_supported_versions">Supported Versions</h3>
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
<td class="">coherence-hibernate-cache-4</td>
<td class=""><code>4.3.x</code></td>
</tr>
<tr>
<td class="">coherence-hibernate-cache-5</td>
<td class=""><code>5.0.x</code>, <code>5.1.x</code></td>
</tr>
<tr>
<td class="">coherence-hibernate-cache-52</td>
<td class=""><code>5.2.x</code></td>
</tr>
<tr>
<td class="">coherence-hibernate-cache-53</td>
<td class=""><code>5.3.x</code>, <code>5.4.x</code>, <code>5.5.x</code>, <code>5.6.x</code></td>
</tr>
</tbody>
</table>
</div>
<div class="admonition important">
<p class="admonition-inline">Active development (new features) focuses on the <code>coherence-hibernate-cache-53</code> module. This module also
supports the latest stable release version of Hibernate <code>5.6.x</code>.</p>
</div>
</div>

<h3 id="_overview">Overview</h3>
<div class="section">
<p>Using Coherence as a Hibernate second-level cache implementation allows multiple JVMs running the same Hibernate
application to share a second-level cache. The use of Coherence caches in this scenario is completely controlled by
Hibernate. You should have a good understanding of Hibernate second-level caching to successfully use the Coherence
Hibernate second-level cache implementation. For more information on Hibernate second-level caching, see the
<a id="" title="" target="_blank" href="https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#caching">relevant chapter on Caching</a> in the Hibernate Core Reference
Manual at <a id="" title="" target="_blank" href="http://www.hibernate.org/docs">http://www.hibernate.org/docs</a>.</p>

<p>Using Coherence as a Hibernate second-level cache implementation may be a good fit for Java applications that use
Hibernate for data access and management, and that run in a cluster of application servers accessing the same database.</p>

<div class="admonition note">
<p class="admonition-inline">Before you use the Coherence Hibernate Cache support, please also consider other caching strategies. Ultimately,
you should make a decision that is most applicable to the needs of your application.</p>
</div>
</div>

<h3 id="_configuration_and_serialization_requirements">Configuration and Serialization Requirements</h3>
<div class="section">

<h4 id="_serialization">Serialization</h4>
<div class="section">
<p>Familiarize yourself with the Coherence Documentation, especially the chapter on
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/performing-basic-cache-operations.html#GUID-F9BCA574-ABFC-4F0D-94EA-949E5B7621E7">Performing Basic Cache Operations</a>
as it also details the <strong>Requirements for Cached Objects</strong>:</p>

<p>Cache keys and values must be serializable (for example, <code>java.io.Serializable</code> or Coherence <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-portable-object-format.html#GUID-F331E5AB-0B3B-4313-A2E3-AA95A40AD913">Portable Object Format</a>
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

<h4 id="_cache_keys">Cache Keys</h4>
<div class="section">
<p>By default, Coherence Hibernate uses the <code>DefaultCacheKeysFactory</code>. When using Hibernate <code>5.2</code> or later, you can customize
the used <code>CacheKeysFactory</code> using the Hibernate property:</p>

<ul class="ulist">
<li>
<p><code>hibernate.cache.keys_factory</code></p>

</li>
</ul>
<p>You can specify the following values:</p>

<ul class="ulist">
<li>
<p><code>default</code>, which wraps identifiers in the tuple (<code>DefaultCacheKeysFactory</code>)</p>

</li>
<li>
<p><code>simple</code>, uses identifiers as keys without any wrapping (<code>SimpleCacheKeysFactory</code>)</p>

</li>
<li>
<p>fully qualified class name that implements <code>org.hibernate.cache.spi.CacheKeysFactory</code></p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">Hibernate versions before <code>5.3.0</code> have issues with composite keys, when using the <code>DefaultCacheKeysFactory</code>.
If you need to use composite keys, please consider using the <code>SimpleCacheKeysFactory</code> instead.</p>
</div>
</div>

<h4 id="_configuring_clients_and_servers_for_hibernate_second_level_caching">Configuring Clients and Servers for Hibernate Second-Level Caching</h4>
<div class="section">
<p>Both the clients of the Coherence Hibernate second-level caches&#8201;&#8212;&#8201;e.g. application server JVMs running Hibernate-based
applications&#8201;&#8212;&#8201;and the Coherence cache server JVMs actually holding the cache contents need to have a common set of
jar file artifacts available to their ClassLoaders. Specifically, both need
<code>coherence-hibernate-cache-xx-2.1.2-SNAPSHOT.jar</code> and its dependencies Coherence and Hibernate
(and their dependencies).</p>

<p>The Coherence cache server JVMs need the Hibernate core jar file to deserialize <code>CacheEntry</code> classes
(<code>org.hibernate.cache.spi.entry.*</code>) since the Coherence Hibernate second-level cache implementation uses Coherence
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/processing-data-cache.html">EntryProcessors</a> to optimize concurrency control.
However, the cache server JVMs do not need the Hibernate application&#8217;s jar files containing entity classes etc.</p>

<p>The client / application server JVMs do of course need the Hibernate application&#8217;s jar files containing entity classes
etc. They should also be configured to be &#8220;storage-disabled&#8221;, i.e. to not store contents of distributed caches. See
comments in the default <code>hibernate-second-level-cache-config.xml</code> for details on how to accomplish that configuration&#8201;&#8212;&#8201;it amounts to starting clients and servers with slightly different cache configuration files, or passing
<code>–Dtangosol.coherence.distributed.localstorage=false</code> to client JVMs.</p>

<div class="admonition tip">
<p class="admonition-inline">As of Coherence Hibernate <code>2.1.0</code> and using the <code>coherence-hibernate-cache-53</code> module, you can specify Coherence
property overrides via Hibernate properties.
E.g. <code>com.oracle.coherence.hibernate.cache.coherence_properties.tangosol.coherence.distributed.localstorage=false</code></p>
</div>
<p>Both client and server JVMs will need the same Coherence operational configuration specifying necessary cluster
communication parameters. See the chapter on
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/understanding-configuration.html#GUID-360B798E-2120-44A9-8B09-1FDD9AB40EB5">Understanding Configuration</a>
in the reference documentation. Coherence provides default operational configuration, but it is a best practice to
override communication parameters and cluster name to make them unique for each separate application environment.</p>

</div>
</div>

<h3 id="_installing_the_coherence_hibernate_second_level_cache">Installing the Coherence Hibernate Second-Level Cache</h3>
<div class="section">
<p>Installing the Coherence Hibernate second-level cache implementation amounts to obtaining a distribution of
<code>coherence-hibernate-cache-xx-2.1.2-SNAPSHOT.jar</code> for the respective Hibernate version of your application.</p>

<p>The easiest way to do so is to build and execute your Hibernate application with Maven, and add the following dependency
to your application&#8217;s <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-hibernate-cache-53&lt;/artifactId&gt;
    &lt;version&gt;2.1.2-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>Alternatively, you can download <code>coherence-hibernate-cache-53-2.1.2-SNAPSHOT.jar</code> from a Maven repository
(e.g. <a id="" title="" target="_blank" href="https://repo1.maven.org/maven2/">https://repo1.maven.org/maven2/</a>) and use it in JVM classpaths. Or you can <router-link to="/dev/03_build-instructions">build</router-link>
the Coherence Hibernate second-level cache implementation from sources.</p>

<p>Coherence Hibernate depends on Oracle Coherence (E.g. <a id="" title="" target="_blank" href="https://coherence.community/">Coherence CE</a> (Community Edition))
and Hibernate. These dependencies must be declared explicitly as we do not include them transitively. A full dependency
declaration may look like the following:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-hibernate-cache-53&lt;/artifactId&gt;
    &lt;version&gt;2.1.2-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;org.hibernate&lt;/groupId&gt;
    &lt;artifactId&gt;hibernate-core&lt;/artifactId&gt;
    &lt;version&gt;5.6.3.Final&lt;/version&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
    &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;version&gt;21.12.2&lt;/version&gt;
&lt;/dependency&gt;</markup>

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
chapter on <a id="" title="" target="_blank" href="https://docs.jboss.org/hibernate/orm/5.6/userguide/html_singleHibernate_User_Guide.html#caching">Caching</a> of the Hibernate Core Reference Manual.</p>


<h4 id="_hibernate_second_level_cache">Hibernate Second-Level Cache</h4>
<div class="section">
<p>To configure Coherence as the Hibernate <em>second-level</em> cache, set the <code>hibernate.cache.region.factory_class</code>
property in Hibernate configuration to <code>com.oracle.coherence.hibernate.cache.v53.CoherenceRegionFactory</code>. For example,
include the following property setting in <code>hibernate.cfg.xml</code>:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.region.factory_class"&gt;
    com.oracle.coherence.hibernate.cache.v53.CoherenceRegionFactory
&lt;/property&gt;</markup>

<div class="admonition note">
<p class="admonition-inline">Coherence Hibernate Cache <code>5.3+</code> uses the <a id="" title="" target="_blank" href="https://coherence.community/latest/21.12/docs/#/docs/core/02_bootstrap">Coherence Bootstrap API</a>.</p>
</div>
<p>In addition to setting the <code>hibernate.cache.region.factory_class</code> property, you must also configure Hibernate to use
second-level caching by setting the appropriate Hibernate configuration property to <code>true</code>, as follows:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.use_second_level_cache"&gt;true&lt;/property&gt;</markup>

<p>Furthermore, you must configure each entity class mapped by Hibernate, and each Collection-typed field mapped by
Hibernate, to use caching on a case-by-case basis. To configure mapped classes and Collection-typed fields to use
<em>second-level</em> caching, add <code>&lt;cache&gt;</code> elements to the class&#8217;s mapping file as in the following example:</p>

<markup
lang="xml"

>&lt;hibernate-mapping package="org.hibernate.tutorial.domain"&gt;
    &lt;class name="Person" table="PEOPLE"&gt;
        &lt;cache usage="read-write" /&gt;
        &lt;id name="id" column="PERSON_ID"&gt;
            &lt;generator class="native"/&gt;
        &lt;/id&gt;
        &lt;property name="age"/&gt;
        &lt;property name="firstname"/&gt;
        &lt;property name="lastname"/&gt;
        &lt;set name="events" table="PERSON_EVENT"&gt;
            &lt;cache usage="read-write" /&gt;
            &lt;key column="PERSON_ID"/&gt;
            &lt;many-to-many column="EVENT_ID" class="Event"/&gt;
        &lt;/set&gt;
        &lt;set name="emailAddresses" table="PERSON_EMAIL_ADDR"&gt;
            &lt;cache usage="read-write" /&gt;
            &lt;key column="PERSON_ID"/&gt;
            &lt;element type="string" column="EMAIL_ADDR"/&gt;
        &lt;/set&gt;
    &lt;/class&gt;
&lt;/hibernate-mapping&gt;</markup>

<p>The possible values for the usage attribute of the cache element are as follows:</p>

<markup
lang="xml"

>&lt;cache usage="transactional | read-write | nonstrict-read-write | read-only" /&gt;</markup>

<p>Alternatively, you can use the equivalent JPA annotations such as in the following example:</p>

<markup
lang="java"

>@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="PEOPLE")
public class Person {
    // ...
}</markup>

<p>The meaning and effect of each possible value is documented below in the section on cache concurrency strategies.</p>

</div>

<h4 id="_hibernate_query_cache">Hibernate Query Cache</h4>
<div class="section">
<p>When configuring query caching, you must again set the Hibernate property <code>hibernate.cache.region.factory_class</code> property.
Furthermore, you must also configure Hibernate to enable query caching by setting the following Hibernate configuration
property to <code>true</code>:</p>

<markup
lang="xml"

>&lt;property name="hibernate.cache.use_query_cache"&gt;true&lt;/property&gt;</markup>

<p>Moreover, you must call <code>setCacheable(true)</code>, on each <code>org.hibernate.Query</code> executed by your application code, as in
the following example:</p>

<markup
lang="java"

>public List listPersons() {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    Query query = session.createQuery("from Person");
    query.setCacheable(true);
    List result = query.list();
    session.getTransaction().commit();
    return result;
}</markup>

</div>
</div>

<h3 id="_types_of_hibernate_second_level_cache">Types of Hibernate Second-Level Cache</h3>
<div class="section">
<p>Hibernate&#8217;s second-level cache design utilizes five different types of second-level cache, as reflected in the names of
sub-interfaces of <code>org.hibernate.cache.spi.Region</code>:</p>

<ul class="ulist">
<li>
<p><code>EntityRegions</code> cache the data of entity instances mapped by Hibernate.  By default Hibernate uses the fully-qualified
name of the entity class as the name of an <code>EntityRegion</code> cache; though the name can be overridden through configuration.</p>

</li>
<li>
<p><code>CollectionRegions</code> cache the data of Collection-typed fields of mapped entities.  Hibernate names <code>CollectionRegion</code>
caches using the fully-qualified name of the entity class followed by the name of the Collection-typed field, separated
by a period.</p>

</li>
<li>
<p><code>NaturalIdRegions</code> cache mappings of secondary identifiers to primary identifiers for entities.</p>

</li>
<li>
<p><code>QueryResultsRegions</code> cache the result sets of queries executed by Hibernate.  Cache keys are formed using the query
string and parameters, and cache values are collections of identifiers of entities satisfying the query.  By default
Hibernate uses one <code>QueryResultsRegion</code> with the name "<code>org.hibernate.cache.internal.StandardQueryCache</code>".  Hibernate
users can instantiate <code>QueryResultsRegions</code> by calling <code>org.hibernate.Query.setCacheRegion()</code> passing custom cache names
(by convention these names should begin with "<code>query.</code>").</p>

</li>
<li>
<p><code>TimestampsRegions</code> cache timestamps at which database tables were last written by Hibernate.  These timestamps are
used by Hibernate during query processing to determine whether cached query results can be used (if a query involves a
certain table, and that table was written more recently than when the result set for that query was last cached, then
the cached result set may be stale and cannot be used).  Hibernate uses one <code>TimestampsRegion</code> named
&#8220;`org.hibernate.cache.spi.UpdateTimestampsCache`&#8221;.  The keys in this cache are database table names, and the values are
machine clock readings.</p>

</li>
</ul>
<p><code>EntityRegions</code>, <code>CollectionRegions</code>, and <code>NaturalIdRegions</code> are treated by Hibernate as &#8220;transactional&#8221; cache regions,
meaning that the full variety of cache concurrency strategies may be configured (see the next section).  Whereas
<code>QueryResultsRegions</code> and <code>TimestampsRegions</code> are used by Hibernate as &#8220;general data&#8221; regions, rendering cache
concurrency strategies irrelevant for those types of caches.</p>

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

<h3 id="_configuring_coherence_caches_for_hibernate_second_level_caching">Configuring Coherence Caches for Hibernate Second-Level Caching</h3>
<div class="section">
<p>By default, the Coherence Hibernate second-level cache implementation uses a cache configuration file named
<code>hibernate-second-level-cache-config.xml</code> at the root level in <code>coherence-hibernate-cache-53-2.1.2-SNAPSHOT.jar</code>.
This configuration file defines cache mappings for Hibernate second-level caches. You can specify an alternative cache
configuration file for Hibernate second-level caches using the Hibernate or Java property
<code>com.oracle.coherence.hibernate.cache.v53.cache_config_file_path</code>, whose value should be the path to a file or ClassLoader
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

<h4 id="_session_name_5_3">Session Name <code>5.3+</code></h4>
<div class="section">
<p>Property <code>com.oracle.coherence.hibernate.cache.session_name</code> allows to specify a name for the
underlying Coherence session. If not specified, the default session name will be used.</p>

</div>

<h4 id="_session_type_5_3">Session Type <code>5.3+</code></h4>
<div class="section">
<p>Property <code>com.oracle.coherence.hibernate.cache.session_type</code> can take the following values:</p>

<ul class="ulist">
<li>
<p>CLIENT - The session is a client session, that is it expects to be a Coherence*Extend client.</p>

</li>
<li>
<p>GRPC   - The session is a gRPC client session.</p>

</li>
<li>
<p>SERVER - The session is a server session, that is it expects to be a Coherence cluster member.
This is the <strong>default</strong> type if no value is specified.</p>

</li>
</ul>
</div>

<h4 id="_minimal_puts">Minimal Puts</h4>
<div class="section">
<p>Hibernate provides the configuration property <code>hibernate.cache.use_minimal_puts</code>, which optimizes cache management for
clustered caches by minimizing cache update operations. The Coherence caching provider enables this by default. Setting
this property to false might increase overhead for cache management.</p>

</div>

<h4 id="_coherence_specific_properties_5_3">Coherence-specific properties <code>5.3+</code></h4>
<div class="section">
<p>When providing Hibernate properties, you can also specify any
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/system-property-overrides.html#GUID-32230D28-4976-4147-A887-0A0120FF5C7E">Coherence system property overrides</a>
using the following property structure:</p>

<markup
lang="properties"

>com.oracle.coherence.hibernate.cache.coherence_properties.*=my property value</markup>

<div class="admonition important">
<p class="admonition-inline">Specifying Coherence-specific properties is available for the Hibernate Cache 53 module only!</p>
</div>
<p>For instance, in order to redirect the logging output of Coherence (Only Coherence!) to its own log file,
and setting the log level to maximum, you could specify:</p>

<markup
lang="properties"

>com.oracle.coherence.hibernate.cache.coherence_properties.coherence.log=/path/to/coherence.log
com.oracle.coherence.hibernate.cache.coherence_properties.coherence.log.level: 9</markup>

<p>Under the covers, Coherence Hibernate is configured using a custom implementation of a Coherence <code>SystemPropertyResolver</code>.</p>

</div>

<h4 id="_logging_5_3">Logging <code>5.3+</code></h4>
<div class="section">
<p>Without specifying any custom logging properties, Coherence Hibernate will set the logger of Coherence to
<code>slf4j</code>. Therefore, Coherence Hibernate should integrate seamlessly into your application out of the box.</p>

<p>Under the covers, Coherence Hibernate is configured using a custom implementation of a Coherence <code>SystemPropertyResolver</code>.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Properties defined via
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/operational-configuration-elements.html#GUID-6DEB2F17-F6CA-4476-8EF7-2B175191929F">Operational Override Files</a>
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