Multiplexor tester readme

- checkout javaCard module.
- build Server
- in javaCard\im\Server\src\ru\openmim\mim\server\messaging\multiplexor\LoadBalancer.properties , 
  specify multiplexor hosts, ports, and socketLimit values. 

For example:

multiplexors.host.addresses.ports=\
194.149.226.70:4444:15000, \
  194.149.226.97:4445:15000, \
  194.149.226.98:4446:15000

Here, 15000 is the sicketLimit for each multiplexor (maximum number of connections 
on the given multiplexor).

- in javaCard\im\Server\src\ru\openmim\mim\server\messaging\test\MultiplexorMeasurer.properties , 
  specify number of connections to open, min & max outgoing packet size, maximum network traffic, 
  and hosts & ports of echoservers.

For example:

REQPARAM_TEST_A1_SOCKETS_COUNT=30000
REQPARAM_PACKET_SIZE_BYTES_MIN=6
REQPARAM_PACKET_SIZE_BYTES_MAX=200
REQPARAM_TRAFFIC_LIMIT_MAX_KBYTES_PER_SECOND=1024
REQPARAM_ECHO_SERVERS_PORTS=194.149.225.17:8888, 194.149.225.18:8888

- cd javaCard/im/Server
- run ./test_runMultiplexorMeasurer.sh 
- when `q' is pressed, the test is stopped (using `killall java').