# Implementation of cross-platform NAT Traversal and data transfer interface  

## Requirements
- Java SE 8

## Configuration
- git clone https://git.ustclug.org/eipi10/DFS.git
- cd DFS
- make help

## Run
###Windows
- java -cp lib/*;build/classes/node nodetest.NodeTest
- java -cp lib/*;build/classes/server server.MainClass

###linux
- java -cp build/classes/node/:lib/* nodetest.NodeTest
- java -cp build/classes/server/:lib/* server.MainClass

### Or
- java -jar dist/nodeTest.jar
- java -jar dist/Server.jar