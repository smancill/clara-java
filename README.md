# Clara Framework

[![Build status](https://github.com/smancill/clara-java/actions/workflows/build.yaml/badge.svg)](https://github.com/smancill/clara-java/actions/workflows/build.yaml)

A microservices framework to provide a heterogeneous computing environment for efficient
Big Data processing.


## Build notes

Clara requires the [Java 17 JDK](https://whichjdk.com/).
Prefer [Eclipse Temurin](https://adoptium.net/) for Java binaries,
and a Java version manager to install and switch JDKs.

[SDKMAN!] or [asdf-java] can be used to manage multiple Java versions.

[SDKMAN!]: https://sdkman.io/
[asdf-java]: https://github.com/halcyon/asdf-java

With SDKMAN!:

``` console
$ sdk list java
$ sdk install java 17.0.5-tem           # there may be a newer version listed above
$ sdk use java 17.0.5-tem
```

With asdf-java:

``` console
$ asdf list-all java
$ asdf install java temurin-17.0.5+8    # there may be a newer version listed above
$ asdf local java temurin-17.0.5+8      # or global
```

To install Temurin 17 system-wide,
follow [the instructions in the official site][temurin-install].

[temurin-binaries]: https://github.com/adoptium/temurin17-binaries/releases
[temurin-install]: https://adoptium.net/installation/


### Installation

To build Clara use the provided [Gradle](https://gradle.org/) wrapper.
It will download the required Gradle version and all the Clara dependencies.

    $ ./gradlew

To deploy the binary distribution to `$CLARA_HOME`:

    $ ./gradlew deploy

To publish the Clara artifacts to the local Maven repository:

    $ ./gradlew publishToMavenLocal


### Importing the project into your IDE

Gradle can generate the required configuration files to import the Clara
project into [Eclipse](https://eclipse.org/ide/) and
[IntelliJ IDEA](https://www.jetbrains.com/idea/):

    $ ./gradlew cleanEclipse eclipse

    $ ./gradlew cleanIdea idea

See also the [Eclipse Buildship plugin](http://www.vogella.com/tutorials/EclipseGradle/article.html)
and the [Intellij IDEA Gradle Help](https://www.jetbrains.com/help/idea/2016.2/gradle.html).


## Authors

* Vardan Gyurjyan
* Sebastián Mancilla
* Ricardo Oyarzún


## License

Clara is licensed under the [Apache License, Version 2.0](./LICENSES/Apache-2.0.txt).

This project also includes code from the [SLF4J](http://www.slf4j.org/) authors,
licensed under the [MIT License](./LICENSES/MIT.txt).
