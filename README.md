# Clara

[![Build status](https://github.com/smancill/clara-java/actions/workflows/build.yaml/badge.svg)](https://github.com/smancill/clara-java/actions/workflows/build.yaml)

A micro-services framework to provide a heterogeneous computing environment for efficient
Big Data processing.


## Build notes

Clara requires the [Java 17 JDK](https://whichjdk.com/).
Prefer [Eclipse Temurin](https://adoptium.net/) for Java binaries,
and a Java version manager to install and switch JDKs.

[SDKMAN!], [Jabba] or [JEnv] can be used to manage multiple Java versions.
See this [StackOverflow answer](https://stackoverflow.com/a/52524114) for more details
(the answer is for macOS but they work with any Unix system).

[SDKMAN!]: https://sdkman.io/
[Jabba]: https://github.com/shyiko/jabba
[JEnv]: https://www.jenv.be/

With [SDKMAN!]:

``` console
$ sdk list java
$ sdk install java 17.0.1-tem                   # there may be a newer version listed above
$ sdk use java 17.0.1-tem
```

With [Jabba] (no Temurin yet):

``` console
$ jabba ls-remote
$ jabba install amazon-corretto@1.17.0-0.35.1   # there may be a newer version listed above
$ jabba use amazon-corretto@1.17.0-0.35.1
```

To install Java 17 system wide,
then use the OpenJDK 17 package from the Linux distribution if available,
or the [Amazon Corretto DEB repo][] for Ubuntu/Debian,
or [Homebrew](https://brew.sh/) with the [temurin cask] for macOS.

[Amazon Corretto DEB repo]: https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/generic-linux-install.html
[temurin cask]: https://formulae.brew.sh/cask/temurin


### Installation

To build Clara use the provided [Gradle](https://gradle.org/) wrapper.
It will download the required Gradle version and all the Clara dependencies.

    $ ./gradlew

To install the Clara artifact to the local Maven repository:

    $ ./gradlew install

To deploy the binary distribution to `$CLARA_HOME`:

    $ ./gradlew deploy


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
