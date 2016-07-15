#!/usr/bin/env bash

this="${BASH_SOURCE-$0}"
while [ -h "$this" ]; do
    ls=`ls -ld "$this"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	this="$link"
    else
	this=`dirname "$this"`/"$link"
    fi
done

# convert relative path to absolute path
bin=`dirname "$this"`
script=`basename "$this"`
bin=`cd "$bin">/dev/null; pwd`
this="$bin/$script"

if [ -z "$APP_HOME" ]; then
    export APP_HOME=`dirname "$this"`/..
fi

APP_CONF_DIR="${APP_CONF_DIR:-$APP_HOME/conf}"

if [ -f "$APP_CONF_DIR/env.sh" ]; then
    . "$APP_CONF_DIR/env.sh"
fi

if [ -z "$JAVA_HOME" ]; then
    cat 1>&2 <<EOF
+======================================================================+
|      Error: JAVA_HOME is not set and Java could not be found         |
+----------------------------------------------------------------------+
| Please download the latest Sun JDK from the Sun Java web site        |
|       > http://java.sun.com/javase/downloads/ <                      |
|                                                                      |
| This app requires Java 1.6 or later.                                 |
| NOTE: This script will find Sun Java whether you install using the   |
|       binary or the RPM based installer.                             |
+======================================================================+
EOF
    exit 1
fi

JAVA=$JAVA_HOME/bin/java
JAVA_HEAP_MAX=-Xmx100m

if [ "$APP_HEAPSIZE" != "" ]; then
    SUFFIX="m"
    if [ "${APP_HEAPSIZE: -1}" == "m" ] || [ "${APP_HEAPSIZE: -1}" == "M" ]; then
	SUFFIX=""
    fi
    if [ "${APP_HEAPSIZE: -1}" == "g" ] || [ "${APP_HEAPSIZE: -1}" == "G" ]; then
	SUFFIX=""
    fi
    JAVA_HEAP_MAX="-Xmx""$APP_HEAPSIZE""$SUFFIX"
fi

CLASSPATH="${APP_CONF_DIR}"
for f in $APP_HOME/lib/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
done
for f in $APP_HOME/dist/*.jar; do
    CLASSPATH=${CLASSPATH}:$f;
done
if [ "$APP_CLASSPATH" != "" ]; then
    CLASSPATH=${CLASSPATH}:${APP_CLASSPATH}
fi

if [ "$APP_LOG_DIR" = "" ]; then
    APP_LOG_DIR="$APP_HOME/logs"
fi
if [ "$APP_LOGFILE" = "" ]; then
    APP_LOGFILE='app.log'
fi
if [ "$APP_ROOT_LOGGER" = "" ]; then
    APP_ROOT_LOGGER='INFO,console'
fi

APP_OPTS="$APP_OPTS -Dapp.log.dir=$APP_LOG_DIR"
APP_OPTS="$APP_OPTS -Dapp.log.file=$APP_LOGFILE"
APP_OPTS="$APP_OPTS -Dapp.root.logger=${APP_ROOT_LOGGER:-INFO,console}"

if [ -z $APP_MAIN_CLASS ]; then
    echo "You must set app main class name in conf/env.sh"
    exit 1
fi

if [ ! -z $APP_NAME ]; then
    APP_OPTS="$APP_OPTS -Dproc_$APP_NAME"
fi

export CLASSPATH
exec $JAVA -XX:OnOutOfMemoryError="kill -9 %p" $JAVA_HEAP_MAX $APP_OPTS $APP_MAIN_CLASS $@
