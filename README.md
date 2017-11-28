# Locust4j

## Links

* Locust Website: <a href="http://locust.io">locust.io</a>
* Locust Documentation: <a href="http://docs.locust.io">docs.locust.io</a>

## Description

Locust4j is a load generator for locust, written in Java. It's inspired by [boomer](https://github.com/myzhan/boomer) 
and [nomadacris](https://github.com/vrajat/nomadacris).

## Features

* ** Write user test scenarios in Java ** <br>
Because it's written in Java, you can use all the things in the Java Ecosystem.

* ** Thread-based concurrency ** <br>
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

## More Examples

See [Main.java](src/main/java/com/github/myzhan/locust4j/examples/Main.java).

This file represents all the exposed APIs of Locust4j.

## Author

* myzhan
* vrajat

## TODO

* Add Locust4j to maven center repository.
* Add more tests.
* Add more documentations.

## License

Open source licensed under the MIT license (see _LICENSE_ file for details).
