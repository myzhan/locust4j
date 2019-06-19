# Locust4j [![Build Status](https://travis-ci.org/myzhan/locust4j.svg?branch=master)](https://travis-ci.org/myzhan/locust4j) [![Coverage Status](https://codecov.io/gh/myzhan/locust4j/branch/master/graph/badge.svg)](https://codecov.io/gh/myzhan/locust4j)

## Links

* Locust Website: <a href="http://locust.io">locust.io</a>
* Locust Documentation: <a href="http://docs.locust.io">docs.locust.io</a>

## Description

Locust4j is a load generator for locust, written in Java. It's inspired by [boomer](https://github.com/myzhan/boomer) 
and [nomadacris](https://github.com/vrajat/nomadacris).

It's a **benchmarking library**, not a general purpose tool. To use it, you must implement test scenarios by yourself.

### Usage examples

- [locust4j-http](https://github.com/myzhan/locust4j-http) is a demo and a good start
- [nejckorasa/locust4j-http-load](https://github.com/nejckorasa/locust4j-http-load) is another example project

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

## Locally Install
```bash
mvn install
```

## Maven

Add this to your Maven project's pom.xml.

```xml
<dependency>
    <groupId>com.github.myzhan</groupId>
    <artifactId>locust4j</artifactId>
    <version>LATEST</version>
</dependency>
```

## More Examples

See [Main.java](examples/task/Main.java).

This file represents all the exposed APIs of Locust4j.

## NOTICE
1. The task instance is shared across multiply threads, the execute method must be thread-safe.
2. Don't catch all exceptions in the execute method, just leave every unexpected exceptions to locust4j.

## Author

* myzhan
* vrajat

## Known Issues

* When stop-the-world happens in the JVM, you may get wrong response time reported to the master.
* Because of the JIT compiler, Locust4j will run faster as time goes by, which will lead to shorter response time.

## License

Open source licensed under the MIT license (see _LICENSE_ file for details).
