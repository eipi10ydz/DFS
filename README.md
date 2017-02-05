# Implementation of cross-platform NAT Traversal and data transfer interface  

## This is Our OS Project implemented by
- Dezhong Yang(me)
- [Zhengyang Wang](https://github.com/wzy9607)
- Yaxi Li
- [Xiao Su](https://github.com/sugarshaw95)

## based on
- [barchart-udt](https://github.com/barchart/barchart-udt)
- [gson](https://github.com/google/gson)
- [httpclient](http://hc.apache.org/httpcomponents-client-ga/index.html)

## Requirements
- Java SE 8

## Configuration
- git clone https://github.com/eipi10ydz/DFS.git
- cd DFS
- make help

## Run
### Windows
- java -cp lib/*;build/classes/node nodetest.NodeTest
- java -cp lib/*;build/classes/server server.MainClass

### Linux
- java -cp build/classes/node/:lib/* nodetest.NodeTest
- java -cp build/classes/server/:lib/* server.MainClass

### Or
- java -jar dist/nodeTest.jar
- java -jar dist/Server.jar