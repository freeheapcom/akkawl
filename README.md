# akkawl
Akka Crawler

## Requirement project
 ```
drawler - https://github.com/freeheapcom/drawler
 ```
 
## Build
 We need to create 2 folders: `dist` and `lib`. The `lib` folder contains all libraries used by the *akkawl*. To get all the requried libraries, just **uncomment** the [line](https://github.com/freeheapcom/akkawl/blob/master/build.sbt#L27) and build with the following sbt build command. Next, copy **all .jar file** from `lib_managed` to `lib` folder, copy the **akkawl_2.11-1.0-SNAPSHOT.jar** to the `dist` folder and comment out the line (for faster build).
 ```
 sbt clean package
 ```
 
## Prerequisites 
 * Start 2 storage engines: *Redis* and *cassandra*
 * Change the config param of the file [app.properties](https://github.com/freeheapcom/akkawl/blob/master/conf/app.properties)
 * Put some *urls* to redis queue defined at [queue](https://github.com/freeheapcom/akkawl/blob/master/conf/app.properties#L3) by issue command like: `redis-cli lpush http://www.nytimes.com`

## Run
 ```
 chmod +x conf/env.sh
 chmod +x bin/*
 ```
 For foreground running
 ```
 ./bin/start.sh 
 ```
 For background 
 ```
 ./bin/daemon.sh start
 ```
