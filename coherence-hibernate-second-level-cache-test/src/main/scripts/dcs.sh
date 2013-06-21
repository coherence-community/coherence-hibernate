#!/bin/bash

mvn exec:java \
-Dexec.mainClass="com.tangosol.net.DefaultCacheServer" \
-Dtangosol.coherence.wka="localhost" \
-Dtangosol.coherence.wka.port="8088" \
-Dtangosol.coherence.cacheconfig="hibernate-second-level-cache-config.xml" \
-Dcom.sun.management.jmxremote \
-Dtangosol.coherence.management=all