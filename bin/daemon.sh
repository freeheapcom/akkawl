#!/usr/bin/env bash

waitForProcessEnd() {
    pidKilled=$1
    processedAt=`date +%s`
    while kill -0 $pidKilled > /dev/null 2>&1;
    do
	echo -n "."
	sleep 1;
	if [ $(( `date +%s` - $processedAt )) -gt 60 ]; then
	    break;
	fi
    done
    # process still there : kill -9
    if kill -0 $pidKilled > /dev/null 2>&1; then
	kill -9 $pidKilled > /dev/null 2>&1
    fi
    # Add a CR after we're done w/ dots.
    echo
}

usage="Usage: daemon (start|stop) <args...>"

if [ $# -le 0 ]; then
    echo $usage
    exit 1
fi

startStop=$1
shift

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

if [ -z $APP_MAIN_CLASS ]; then
    echo "Use must set app main class name in conf/env.sh"
    exit 1
fi

if [ "$APP_LOG_DIR" = "" ]; then
    APP_LOG_DIR="$APP_HOME/logs"
fi
mkdir -p $APP_LOG_DIR
logout=$APP_LOG_DIR/app.out

if [ "$APP_PID_DIR" = "" ]; then
    APP_PID_DIR="/tmp"
fi
pid=$APP_PID_DIR/$APP_MAIN_CLASS.pid

case $startStop in 
    (start)
	mkdir -p "$APP_PID_DIR"
	if [ -f $pid ]; then
	    if kill -0 `cat $pid` > /dev/null 2>&1; then
		echo App running as process `cat $pid`.  Stop it first.
		exit 1
	    fi
	fi
	nohup $bin/start.sh $@ < /dev/null > ${logout} 2>&1  &
	echo $! > $pid
	echo "App started"
	;;
    (stop)
	if [ -f $pid ]; then
	    pidToKill=`cat $pid`
	    if kill -0 $pidToKill > /dev/null 2>&1; then
		echo -n "stopping app"
		kill $pidToKill > /dev/null 2>&1
		waitForProcessEnd $pidToKill
	    else
		retval=$?
		echo no app to stop because kill -0 of pid $pidToKill failed with status $retval
	    fi
	else
	    echo no app to stop because no pid file $pid
	fi
	rm -f $pid
	;;
esac
