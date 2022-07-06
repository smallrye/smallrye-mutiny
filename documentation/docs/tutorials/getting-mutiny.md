---
tags:
- tutorial
- beginner
---

# Getting started with Mutiny

## Using Mutiny in a Java application

Add the _dependency_ to your project using your preferred build tool:

=== "Apache Maven"

    ```xml
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>mutiny</artifactId>
        <version>{{ attributes.versions.mutiny }}</version>
    </dependency>
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'io.smallrye.reactive:mutiny:{{ attributes.versions.mutiny }}'
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.smallrye.reactive:mutiny:{{ attributes.versions.mutiny }}")
    ```

=== "JBang"

    ```java
    //DEPS io.smallrye.reactive:mutiny:{{ attributes.versions.mutiny }}
    ```

## Using Mutiny with Quarkus

Most of the [Quarkus](https://quarkus.io) extensions with reactive capabilities already depend on Mutiny.

You can also add the `quarkus-mutiny` dependency explicitly from the command-line:

```bash
mvn quarkus:add-extension -Dextensions=mutiny
```

or by editing the `pom.xml` file and adding:

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-mutiny</artifactId>
</dependency>
```

## Using Mutiny with Vert.x

Most of the [Eclipse Vert.x](https://vertx.io) stack modules are available through the [SmallRye Mutiny Vert.x Bindings](https://smallrye.io/smallrye-mutiny-vertx-bindings/) project.

Bindings for Vert.x modules are named by prepending `smallrye-mutiny-`.
As an example here's how to add a dependency to the `vertx-core` Mutiny bindings:

=== "Apache Maven"

    ```xml
    <dependency>
        <groupId>io.smallrye.reactive</groupId>
        <artifactId>smallrye-mutiny-vertx-core</artifactId>
        <version>{{ attributes.versions.vertx_bindings }}</version>
    </dependency>
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'io.smallrye.reactive:smallrye-mutiny-vertx-core:{{ attributes.versions.vertx_bindings }}'
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-core:{{ attributes.versions.vertx_bindings }}")
    ```

=== "JBang"

    ```java
    //DEPS io.smallrye.reactive:smallrye-mutiny-vertx-core:{{ attributes.versions.vertx_bindings }}
    ```
