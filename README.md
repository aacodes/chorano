## Chorano: A simple demo of a client-server distributed system for routing http requests with *some* fault tolerance

This project is for educational purposes.

**Summary:** The project is an example of a distributed system where concurrent incoming http requests are load balanced by a client program called chorano-client among a set of servers called chorano-servers in a round-robin fashion. The chorano-servers register themselves to zookeeper server and the chorano-client talks to zookeeper server to route the http requests to  the *active/alive* chorano-servers. 

The project consists of two modules with a parent pom file at the root.

**Module 1 (chorano-client):** A simple client program that uses the zookeeper client object to find the currently *active/registered* chorano-servers in zookeeper. It then load balances the HTTP request by cycling through the active chorano servers (similar to round-robin). The http requests are built and sent by a JAX-RS [Jersey](https://jersey.github.io/documentation/latest/client.html) client.

**Module 2 (chorano-servers):** A chorano server is a lightweight server which listens to a user-specified port number. The server handles incoming HTTP requests by utilizing worker threads from a fixed thread pool. The server writes back a (*hardcoded*) http response with http status 200(OK). 

**How does it work ?**

Whenever we start a chorano-server, the server program register itself in zookeeper under the root path/znode `/chorano` as a child (e.g. `/chorano/x_1`) via the zookeeper client object. Then it keeps on listening to a port for incoming streams of data. The `/chorano` is a persistent node in zookeeper while the children znodes(servers) `e.g. x_1` are ephemeral znode. This means when a server is shut down the corresponding znode disappears in zookeeper. On the other hand, the chorano-client keeps a watch at the `/chorano` path and gets notified of any changes (e.g. a shutdown or an addition or location change of a server) via the zookeeper client object. Note: both the client and servers need to know *only* the root path i.e. `/chorano` but do not need to know the children paths. This is okay since both the client and server code will be maintained by the same developer(s). As soon as the chorano-client gets notified it updates the list of active servers so that subsequent http requests are sent only them. When all the servers are shutdown the client exits.

**Demo:** In our demo we will play around with three chorano-servers and one chorano-client. Using the client we will load balance the http requests to the three chorano-servers. We will shutdown the servers one by one and observe how the client keeps routing the requests only to the active servers.

**Cloning and building the project:**

Clone or download the project. The properties files are located at:
```
cd choranoclient/src/main/resources/properties
zkclient.connection=localhost:2181 (the zookeeper client connection string)
http.request.count=100 (the number of http requests to be sent to the servers)
```
```
cd choranoserver/src/main/resources/properties
request.handler.thread.pool.size=30 (the number of threads that should handle the incoming http requests)
zkclient.connection=localhost:2181  (the zookeeper client connection string)
```
Build the projects from the root directory where the parent pom exists (*e.g. chorano-master/pom.xml*)
```
cd chorano/
mvn clean package
```
It builds both the modules and creates jar file (with all dependencies) at the following locations:
```
chorano/choranoserver/target/chorano-servers.jar
chorano/choranoclient/target/chorano-client.jar
```

**Pre-requisite:** To continue further please make sure you have a running [zookeeper](https://zookeeper.apache.org/doc/current/zookeeperStarted.html) server(s).  

Once the projects have successfully built, start three chorano servers. NOTE: We can spin up any number of servers [1-n]. 

**Go to a terminal (Terminal-1)**
```
cd choranoserver/target/
java -jar chorano-server.jar
DEBUG ChoranoServer - Server >> Specify the port to listen to:
8111
DEBUG ChoranoServer - Connecting to zookeeper client at: localhost:2181, timeout: 3000
DEBUG ChoranoServer - Creating root persistant node named "/chorano" in zk server
DEBUG ChoranoServer - Registering server at the root node e.g. "/chorano/x_1"
DEBUG ChoranoServer - Starting the chorano server so that it keeps listening to port: 8111
```
**Go to a second terminal (Terminal-2)**
```
cd choranoserver/target/
java -jar chorano-server.jar
DEBUG ChoranoServer - Server >> Specify the port to listen to:
8112
DEBUG ChoranoServer - Connecting to zookeeper client at: localhost:2181, session timeout: 3000
DEBUG ChoranoServer - Creating root persistant node named "/chorano" in zk server
DEBUG ChoranoServer - Registering server at the root node e.g. "/chorano/x_1"
DEBUG ChoranoServer - Starting the chorano server so that it keeps listening to port: 8112
```
**Go to a third terminal (Terminal-3)**
```
java -jar chorano-server.jar
DEBUG ChoranoServer - Server >> Specify the port to listen to:
8113
DEBUG ChoranoServer - Connecting to zookeeper client at: localhost:2181, session timeout: 3000
DEBUG ChoranoServer - Creating root persistant node named "/chorano" in zk server
DEBUG ChoranoServer - Registering server at the root node e.g. "/chorano/x_1"
DEBUG ChoranoServer - Starting the chorano server so that it keeps listening to port: 8113
```

Notice that the ports are 8111, 8112 and 8113 in the three terminals respectively. This setup is similar to a distributed environment with three machines each running a chorano-server.

**Running the chorano-client**
```
Go to a fourth terminal (Terminal-4)
cd choranoclient/target
java -jar chorano-client.jar
DEBUG ChoranoClient - Connecting to zookeeper client at: localhost:2181, timeout: 3000
DEBUG ChoranoClient - Count of active servers: 3
DEBUG ChoranoClient - server: x_0000000053 location: localhost:8111
DEBUG ChoranoClient - server: x_0000000054 location: localhost:8112
DEBUG ChoranoClient - server: x_0000000055 location: localhost:8113
```
Notice that the client has already detected the presence of 3 servers by talking to the zookeeper server via the zookeeper client. It will then began sending the http requests to each of the server located at port 8111, 8112 and 8113.
```
DEBUG ClientRequestServiceImpl - Sending http request no. 0 to chorano server: x_0000000053 at url http://localhost:8111
DEBUG ClientRequestServiceImpl - Node: x_0000000053 Response entity: Serving: Client request no: 0
DEBUG ClientRequestServiceImpl - Sending http request no. 1 to chorano server: x_0000000054 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: x_0000000054 Response entity: Serving: Client request no: 1
DEBUG ClientRequestServiceImpl - Sending http request no. 2 to chorano server: x_0000000055 at url http://localhost:8113
DEBUG ClientRequestServiceImpl - Node: x_0000000055 Response entity: Serving: Client request no: 2
DEBUG ClientRequestServiceImpl - Sending http request no. 3 to chorano server: x_0000000053 at url http://localhost:8111
DEBUG ClientRequestServiceImpl - Node: x_0000000053 Response entity: Serving: Client request no: 3
DEBUG ClientRequestServiceImpl - Sending http request no. 4 to chorano server: x_0000000054 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: x_0000000054 Response entity: Serving: Client request no: 4
DEBUG ClientRequestServiceImpl - Sending http request no. 5 to chorano server: x_0000000055 at url http://localhost:8113
DEBUG ClientRequestServiceImpl - Node: x_0000000055 Response entity: Serving: Client request no: 5
```
An example of a server handling the incoming post as they arrive:
```
DEBUG HttpRequestHandler - POST / HTTP/1.1Content-Type: text/plainUser-Agent: Jersey/2.12 (HttpUrlConnection 1.8.0_131)Host: localhost:8113Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2Connection: keep-aliveContent-Length: 20
```
**Shutting down a server:**

Let us shut down `server-1` located at port `8111` by pressing `ctrl+c` at the terminal (`terminal-1`). We will see that the shutdown is detected by the chorano-client and subsequently http requests are  routed to only `8112(server-2)` and `8113 (server-3)`. Following is the log from chorano-client terminal (`terminal-4`)
```
DEBUG ChoranoClient - Change detected in one of the servers
DEBUG ChoranoClient - event: WatchedEvent state:SyncConnected type:NodeDeleted path:/chorano/x_0000000061
DEBUG ChoranoClient - Removing server node from zk path: /chorano/x_0000000061
DEBUG ChoranoClient - New server node created under chorano /chorano
DEBUG ChoranoClient - Count of active servers: 2
DEBUG ChoranoClient - server: x_0000000058 location: localhost:8112
DEBUG ChoranoClient - server: x_0000000059 location: localhost:8113
DEBUG ClientRequestServiceImpl - Sending http request no. 12 to chorano server: /chorano/x_0000000058 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000058 Response entity: Serving: Client request no: 12
DEBUG ClientRequestServiceImpl - Sending http request no. 13 to chorano server: /chorano/x_0000000059 at url http://localhost:8113
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000059 Response entity: Serving: Client request no: 13
DEBUG ClientRequestServiceImpl - Sending http request no. 14 to chorano server: /chorano/x_0000000058 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000058 Response entity: Serving: Client request no: 14
DEBUG ClientRequestServiceImpl - Sending http request no. 15 to chorano server: /chorano/x_0000000059 at url http://localhost:8113
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000059 Response entity: Serving: Client request no: 15
DEBUG ClientRequestServiceImpl - Sending http request no. 16 to chorano server: /chorano/x_0000000058 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000058 Response entity: Serving: Client request no: 16
DEBUG ClientRequestServiceImpl - Sending http request no. 17 to chorano server: /chorano/x_0000000059 at url http://localhost:8113
```
**Starting back the dead server (server-1)**

Let us start the first server again from `terminal-1`. Following is the log:
```
java -jar chorano-server.jar
DEBUG ChoranoServer - Server >> Specify the port to listen to:
8111
DEBUG ChoranoServer - Connecting to zookeeper client at: localhost:2181, session timeout: 3000
DEBUG ChoranoServer - Creating root persistant node named "/chorano" in zk server
DEBUG ChoranoServer - Registering server at the root node e.g. "/chorano/x_1"
DEBUG ChoranoServer - Starting the chorano server so that it keeps listening to port: 8111
```

The client immediately finds `server-1` at `port 8111` and starts sending http requests to it.
```
DEBUG ChoranoClient - New server node created under chorano /chorano
DEBUG ChoranoClient - Count of active servers: 3
DEBUG ChoranoClient - server: x_0000000066 location: localhost:8111
DEBUG ChoranoClient - server: x_0000000058 location: localhost:8112
DEBUG ChoranoClient - server: x_0000000059 location: localhost:8113
DEBUG ClientRequestServiceImpl - Sending http request no. 15 to chorano server: /chorano/x_0000000066 at url http://localhost:8111
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000066 Response entity: Serving: Client request no: 15
DEBUG ClientRequestServiceImpl - Sending http request no. 16 to chorano server: /chorano/x_0000000058 at url http://localhost:8112
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000058 Response entity: Serving: Client request no: 16
DEBUG ClientRequestServiceImpl - Sending http request no. 17 to chorano server: /chorano/x_0000000059 at url http://localhost:8113
DEBUG ClientRequestServiceImpl - Node: /chorano/x_0000000059 Response entity: Serving: Client request no: 17
```
Notes:
Ssh tunnelling might be required if the zookeeper server is located in a VM.
```
e.g. ssh -L 2181:localhost:2181 <user>@<machine>
```
We can stop a chorano-server by pressing `ctrl+c` from the terminal. Or for a more graceful shutdown use the following curl command (might need to repeat the curl more than once).
```
curl -X POST -H "Content-Type: text/plain" --data "stop" http://localhost:8111
```
When the server finds a “stop” keyword in its request body it signals the main server thread to exit, thereby shutting down the chorano-server.

To learn more about [zookeeper server, client and watchers](https://zookeeper.apache.org/doc/current/zookeeperStarted.html)
Quick [tutorial](https://www.baeldung.com/java-zookeeper) on how to use the zookeeper client.
