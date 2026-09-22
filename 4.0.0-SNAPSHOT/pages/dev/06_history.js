<doc-view>

<h2 id="_change_history">Change History</h2>
<div class="section">
<p>The following sections outline Oracle Coherence Hibernate Integration changes in
reverse chronological order.</p>


<h3 id="_version_4_0_0_in_development">Version 4.0.0 (in development)</h3>
<div class="section">
<ul class="ulist">
<li>
<p>Require Java 17 and validate the project on Java 17, 21, and 25.</p>

</li>
<li>
<p>Upgrade the current integration baseline to Hibernate ORM <code>7.4.x</code> and Jakarta Persistence <code>3.2</code>.</p>

</li>
<li>
<p>Publish <code>coherence-hibernate-cache-7</code> as the sole 4.x second-level cache adapter.</p>

</li>
<li>
<p>Restore support for <code>hibernate.cache.keys_factory</code>. Earlier 4.0 snapshots ignored this setting and always used
the default factory. When upgrading from those snapshots with <code>simple</code> or a custom factory that changes the key
format, stop users of the affected cache regions and evict those regions before restarting, or use new isolated
regions. Do not mix members using different key formats in the same regions.</p>

</li>
<li>
<p>Keep Hibernate <code>5.6.x</code> and <code>6.x</code> support on the Coherence Hibernate <code>3.x</code> maintenance line.</p>

</li>
<li>
<p>Remove the obsolete <code>coherence14_1_1</code> Maven profile and Coherence 14.1.1 compatibility code. Create sessions using
<code>SessionConfiguration</code>; Coherence 14.1.1 is not supported by the 4.x release.</p>

</li>
<li>
<p>Compatibility-test Hibernate <code>7.4.1.Final</code>, <code>7.4.5.Final</code>, and <code>7.4.9.Final</code> with Coherence CE <code>15.1.1-0-5</code> and
<code>26.07</code> on the Java 17 bytecode baseline, in addition to the complete default-version reactor on Java 17, 21, and 25.</p>

</li>
</ul>

</div>


<h3 id="_version_3_0_3_october_6_2024">Version 3.0.3 (October 6, 2024)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.3" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.3</a></p>

</div>


<h3 id="_version_3_0_2_march_18_2024">Version 3.0.2 (March 18, 2024)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.2" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.2</a></p>

</div>


<h3 id="_version_3_0_1_january_27_2024">Version 3.0.1 (January 27, 2024)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.1" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.1</a></p>

</div>


<h3 id="_version_3_0_0_july_4_2023">Version 3.0.0 (July 4, 2023)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.0" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v3.0.0</a></p>

</div>


<h3 id="_version_2_3_3_july_2_2023">Version 2.3.3 (July 2, 2023)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.3" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.3</a></p>

</div>


<h3 id="_version_2_3_2_january_26_2023">Version 2.3.2 (January 26, 2023)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.2" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.2</a></p>

</div>


<h3 id="_version_2_3_1_november_3_2022">Version 2.3.1 (November 3, 2022)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.1" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.1</a></p>

</div>


<h3 id="_version_2_3_0_august_25_2022">Version 2.3.0 (August 25, 2022)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.0" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.3.0</a></p>

</div>


<h3 id="_version_2_2_0_august_2022">Version 2.2.0 (August, 2022)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.2.0" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.2.0</a></p>

</div>


<h3 id="_version_2_1_1_jan_2022">Version 2.1.1 (Jan, 2022)</h3>
<div class="section">
<p><a target="_blank" href="https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.1.1" class="bare">https://github.com/coherence-community/coherence-hibernate/releases/tag/v2.1.1</a></p>

</div>


<h3 id="_version_2_1_0_m1_nov_2021">Version 2.1.0-M1 (Nov 2021)</h3>
<div class="section">

<h4 id="_global_and_cross_module_changes">Global and Cross-Module Changes</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Add sample application</p>

</li>
<li>
<p>Require <code>Java 11</code> to build the project. When solely using, Java 8 is still supported</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_second_level_cache">coherence-hibernate-second-level-cache</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Add support for configurable CacheKeysFactories in Hibernate <code>5.2.x</code></p>

</li>
<li>
<p>Add support for Hibernate <code>5.3.x</code>, <code>5.4.x</code>, <code>5.5.x</code>, <code>5.6.x</code></p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_site">coherence-hibernate-site</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Migrate to new website format</p>

</li>
<li>
<p>Publish JavaDoc</p>

</li>
</ul>

</div>

</div>


<h3 id="_version_2_0_0_2020_11_25_1530">Version 2.0.0 @ 2020-11-25 15:30</h3>
<div class="section">

<h4 id="_global_and_cross_module_changes_2">Global and Cross-Module Changes</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Change licensing from the COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL)
to The Universal Permissive License (UPL)</p>

</li>
<li>
<p>Updating of dependencies.</p>

</li>
<li>
<p>Merge functional tests into the respective versions</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_cache_store">coherence-hibernate-cache-store</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Update dependencies</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_second_level_cache_2">coherence-hibernate-second-level-cache</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Add support for Hibernate <code>4.3.x</code>, <code>5.0.x</code>, <code>5.1.x</code>, <code>5.2.x</code></p>

</li>
<li>
<p>Add additional modules for the various Hibernate versions</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_site_2">coherence-hibernate-site</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Polishing and updating for <code>2.0.0</code></p>

</li>
<li>
<p>Update site code</p>

</li>
</ul>

</div>

</div>


<h3 id="_version_1_0_0_2013_09_12_1530">Version 1.0.0 @ 2013-09-12 15:30</h3>
<div class="section">

<h4 id="_source_and_documentation_contributors">Source and Documentation Contributors</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Randy Stafford</p>

</li>
</ul>

</div>


<h4 id="_global_and_cross_module_changes_3">Global and Cross-Module Changes</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_cache_store_2">coherence-hibernate-cache-store</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_cache_store_tests">coherence-hibernate-cache-store-tests</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_second_level_cache_3">coherence-hibernate-second-level-cache</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_second_level_cache_tests">coherence-hibernate-second-level-cache-tests</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>


<h4 id="_coherence_hibernate_site_3">coherence-hibernate-site</h4>
<div class="section">
<ul class="ulist">
<li>
<p>Initial release.</p>

</li>
</ul>

</div>

</div>

</div>

</doc-view>