# default: install lint build test

compile:
    javac -d bin TCPChatRoom/src/main/java/*.java
    
server:
    java -cp bin Server

client:
    java -cp bin Client

