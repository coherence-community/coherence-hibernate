<doc-view>
<p>To build the Coherence Hibernate project you need to have the following software installed:</p>

<ul class="ulist">
<li>
<p><a target="_blank" href="https://www.oracle.com/java/technologies/downloads/">Java SE Development Kit 17, 21, or 25</a>.</p>

</li>
<li>
<p><a target="_blank" href="http://maven.apache.org/">Apache Maven</a></p>

</li>
</ul>

<p>Once you have these installed building the entire source tree is as simple as executing the following shell command
(from the root of the source directory):</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<p>The parent POM defaults to Hibernate <code>7.4.10.Final</code> and Coherence <code>15.1.1-0-6</code>; Java 17 remains the bytecode baseline.
The GitHub Actions workflow configures this default reactor on Java 17, 21, and 25. Its compatibility job
configures Hibernate <code>7.4.1.Final</code>, <code>7.4.5.Final</code>, and <code>7.4.9.Final</code> against Coherence <code>15.1.1-0-6</code> on
Java 17, and against <code>26.10</code> on Java 21 with explicit hardened mode.
The compatibility job runs on <code>main</code>, for pull requests targeting <code>main</code>, and for manual workflow runs.</p>

<p>To test with Coherence <code>26.10</code> locally, select JDK 21 through <code>JAVA_HOME</code> and <code>PATH</code>, then run:</p>

<markup
lang="bash"

>./mvnw -Dcoherence.version=26.10 -Dcoherence.security.mode=hardened \
  -pl coherence-hibernate-cache-7,coherence-hibernate-cache-store-tests -am test</markup>

<p>This uses the default Hibernate <code>7.4.10.Final</code>; add <code>-Dhibernate7.version=&lt;version&gt;</code> to select a compatibility version.
The build uses standard Maven release repositories for Coherence <code>26.10</code> and its matching security-config Shade
transformer; no snapshot repository settings or snapshot cache refresh are needed. To test published artifacts without
local installations, select an empty, dedicated Maven repository with <code>-Dmaven.repo.local=/path/to/temporary-repository</code>,
and remove that repository when finished.</p>

<p>CI cache keys and restore prefixes separate the default build from compatibility jobs and separate
JDK/Hibernate/Coherence combinations; POM and <code>.mvn</code> changes invalidate the exact cache key.</p>

<p>Coherence <code>26.10</code> requires Java 21, while the adapter still targets Java 17. Compatibility jobs stop at <code>test</code>;
the default reactor&#8217;s <code>verify</code> includes the packaged security checks. To include those checks and build the samples
and documentation with Coherence <code>26.10</code>, run the full reactor:</p>

<markup
lang="bash"

>./mvnw -Dcoherence.version=26.10 -Dcoherence.security.mode=hardened clean verify</markup>

<p>A configured workflow does not establish a completed hosted run or deployment qualification.</p>

<p>For detailed instructions, please see the main
<a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/blob/main/README.adoc">README</a> of the Coherence Hibernate
source code repository.</p>

</doc-view>