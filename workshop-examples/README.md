# Mutiny workshop examples

This project contains learning examples for Mutiny, covering a wide range of topics such as the basics, transformations, error recovery and more.

## Structure

The examples are found in `src/main/java` and use a non-conventional file naming strategy.

For instance you will find the failure handling examples in `_04_failures` where `_04_Uni_Failure_Retry.java` holds one such example.

This naming scheme is not usual for Java programs, but it makes for a great way to organize files 😄

## Running the examples

Each file is a self-contained class with a `main` method.

A friendly option is to open the Maven project in your favorite IDE, then run the `main` method of each example of interest.

The other option is to use [JBang](https://www.jbang.dev/) as each example is a valid JBang script:

```shell
jbang src/main/java/_05_backpressure/_03_Visual_Drop.java
```

What's more each file is Unix-executable, so you can also run as:

```shell
./src/main/java/_05_backpressure/_03_Visual_Drop.java
```
