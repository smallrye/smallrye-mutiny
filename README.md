![Build Status](https://github.com/smallrye/smallrye-mutiny/actions/workflows/build-main.yml/badge.svg) 
![Build status (1.x branch)](https://github.com/smallrye/smallrye-mutiny/actions/workflows/build-1.x.yml/badge.svg) 
![License](https://img.shields.io/github/license/smallrye/smallrye-mutiny.svg) 
![Maven Central](https://img.shields.io/maven-central/v/io.smallrye.reactive/mutiny?color=green) 
![Javadoc](https://javadoc.io/badge2/io.smallrye.reactive/mutiny/javadoc.svg)

# ⚡️ Mutiny, an Intuitive Event-Driven Reactive Programming Library for Java

[Mutiny is a novel reactive programming library](https://smallrye.io/smallrye-mutiny/).

Mutiny provides a simple but powerful asynchronous development model that lets you build reactive applications.

## 🚀 Overview

Mutiny can be used in any Java application exhibiting asynchrony.

From reactive microservices, data streaming, event processing to API gateways and network utilities, Mutiny is a great fit.

### Event-Driven

Mutiny places events at the core of its design. 
With Mutiny, you observe events, react to them, and create elegant and readable processing pipelines.

**💡 A PhD in functional programming is not required.**

### Navigable

Even with smart code completion, classes with hundred of methods are confusing.

Mutiny provides a navigable and explicit API driving you towards the operator you need.

### Non-Blocking I/O

Mutiny is the perfect companion to tame the asynchronous nature of applications with non-blocking I/O.

Declaratively compose operations, transform data, enforce progress, recover from failures and more.

### Quarkus and Vert.x native

Mutiny is integrated in [Quarkus](https://quarkus.io) where every reactive API uses Mutiny, and [Eclipse Vert.x](https://vertx.io) clients are made available using [Mutiny bindings](https://github.com/smallrye/smallrye-mutiny-vertx-bindings).

Mutiny is however an independent library that can ultimately be used in any Java application.

### Reactive Converters Built-In

Mutiny is based on the [Reactive Streams protocol](https://www.reactive-streams.org/), and so it can be integrated with any other reactive programming library.

In addition, Mutiny offers converters to interact with other popular libraries and [Kotlin](https://kotlinlang.org/).

## 📦 Build instructions

Mutiny is built with Apache Maven, so all you need is:

```shell
./mvnw install
```

| Git branch | Versions                       | Baseline                              | Compliance                 |
|------------|--------------------------------|---------------------------------------|----------------------------|
| `main`     | 2.x *(in development)*         | Java 11, `java.util.concurrent.Flow ` | Reactive Streams TCK 1.0.4 |
| `1.x`      | 1.x.y *(backports, bug fixes)* | Java 8, Reactive Streams 1.0.4        | Reactive Streams TCK 1.0.4 |

## ✨ Contributing

See [the contributing guidelines](CONTRIBUTING.md)

Mutiny is an open project, feel-free to:

- [report issues](https://github.com/smallrye/smallrye-mutiny/issues), and
- [propose enhancements via pull-requests](https://github.com/smallrye/smallrye-mutiny/pulls).

## 👋 Discussions and support

For anything related to the usage of Mutiny in Quarkus, please refer to the [Quarkus support](https://quarkus.io/support/)

For more general discussions about Mutiny, you can:

- [start a new discussion thread in GitHub Discussions (preferred option)](https://github.com/smallrye/smallrye-mutiny/discussions), or
- [use the `mutiny` tag on StackOverflow](https://stackoverflow.com/questions/tagged/mutiny).

## 🧪 Publications

Julien Ponge, Arthur Navarro, Clément Escoffier, and Frédéric Le Mouël. 2021. **[Analysing the Performance and Costs of Reactive Programming Libraries in Java](https://doi.org/10.1145/3486605.3486788).** *In Proceedings of the 8th ACM SIGPLAN International Workshop on Reactive and Event-Based Languages and Systems (REBLS ’21)*, October 18, 2021, Chicago, IL, USA. ACM, New York, NY, USA, 10 pages. [(PDF)](https://hal.inria.fr/hal-03409277/document)
