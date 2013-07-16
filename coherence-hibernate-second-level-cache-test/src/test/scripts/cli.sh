#!/bin/bash

mvn exec:java \
-DskipTests \
-Dmaven.test.skip=true \
-Dexec.mainClass="com.tangosol.net.CacheFactory" \
-Dtangosol.coherence.wka="localhost" \
-Dtangosol.coherence.wka.port="8088" \
-Dtangosol.coherence.cacheconfig="hibernate-second-level-cache-config.xml" \
-Dtangosol.coherence.distributed.localstorage=false