# Java to use
#export JAVA_HOME=/usr

# Main class name, must be set
export APP_MAIN_CLASS=Main

# App name, will appear in jps
export APP_NAME=stream-processor

# Heap size
export APP_HEAPSIZE=1024

# Extra class path
# export APP_CLASSPATH=

# Extra runtime opt
export APP_OPTS="-XX:+UseConcMarkSweepGC"

# Log
# export APP_LOG_DIR=../logs
# export APP_LOGFILE=server.log
#export APP_ROOT_LOGGER=INFO,DRFA
export APP_ROOT_LOGGER=INFO,console
#export APP_ROOT_LOGGER=TRACE,console

# Daemonize
export APP_PID_DIR=logs

# Set run mode
if [ -z $RUNMODE ]; then
    export RUNMODE="dev"
fi

