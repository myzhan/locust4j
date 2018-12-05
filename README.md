# Locust4j [![Build Status](https://travis-ci.org/myzhan/locust4j.svg?branch=master)](https://travis-ci.org/myzhan/locust4j) [![Coverage Status](https://codecov.io/gh/myzhan/locust4j/branch/master/graph/badge.svg)](https://codecov.io/gh/myzhan/locust4j)

## Links

* Locust Website: <a href="http://locust.io">locust.io</a>
* Locust Documentation: <a href="http://docs.locust.io">docs.locust.io</a>

## Description

Locust4j is a load generator for locust, written in Java. It's inspired by [boomer](https://github.com/myzhan/boomer) 
and [nomadacris](https://github.com/vrajat/nomadacris).

## Features

* **Write user test scenarios in Java** <br>
Because it's written in Java, you can use all the things in the Java Ecosystem.

* **Thread-based concurrency** <br>
Locust4j uses threadpool to execute your code with low overhead.

## Build

```bash
git clone https://github.com/myzhan/locust4j
cd locust4j
mvn package
```

## Run BuiltIn Example

```bash
# start locust master
locust -f dummy.py --master --master-bind-host=127.0.0.1 --master-bind-port=5557

# start Locust4j
java -cp target/locust4j-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.myzhan.locust4j.examples.Main
```

## Maven

Add this to your Maven project's pom.xml.

```xml
<dependency>
    <groupId>com.github.myzhan</groupId>
    <artifactId>locust4j</artifactId>
    <version>1.0.1</version>
</dependency>
```

## More Examples

See [Main.java](src/main/java/com/github/myzhan/locust4j/examples/Main.java).

This file represents all the exposed APIs of Locust4j.

## Author

* myzhan
* vrajat

## Known Issues

* When stop-the-world happens in the JVM, you may get wrong response time reported to the master.
* Because of the JIT compiler, Locust4j will run faster as time goes by, which will lead to shorter response time.

## TODO

* Add more tests.
* Add more documentations.

## License

Open source licensed under the MIT license (see _LICENSE_ file for details).
