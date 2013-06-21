#!/bin/bash

mvn exec:java \
-Dexec.mainClass="org.hibernate.tutorial.EventManager" \
-Dexec.args="store" \
-Dtangosol.coherence.wka="localhost" \
-Dtangosol.coherence.wka.port="8088" \
-Dtangosol.coherence.distributed.localstorage=false \
-Dtangosol.coherence.log.level=7 \
-Dcom.oracle.coherence.hibernate.cache.dump_stack_on_debug_message=true

