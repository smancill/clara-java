#!/bin/bash

<#if farm.javaOpts??>
export JAVA_OPTS="${farm.javaOpts}"
</#if>

export CLARA_HOME="${clara.dir}"
<#if clara.monitorFE??>
export CLARA_MONITOR_FE="${clara.monitorFE}"
</#if>

"$CLARA_HOME/bin/kill-dpes"

sleep $[ ( $RANDOM % 20 )  + 1 ]s

${farm.command}
