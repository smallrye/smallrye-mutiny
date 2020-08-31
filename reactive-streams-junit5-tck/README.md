# Reactive Streams TCK for Junit 5 

_This module is a fork of the Reactive Streams TCK (https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck)
 but use Junit 5 instead of TestNG. This project does not depend on Mutiny._

The purpose of the *Reactive Streams Technology Compatibility Kit* (from here on referred to as: *the TCK*) is to guide
and help Reactive Streams library implementers to validate their implementations against the rules defined in
 [the Specification](https://github.com/reactive-streams/reactive-streams-jvm).

## Structure of the TCK

The TCK aims to cover all rules defined in the Specification, however for some rules outlined in the Specification it is
not possible (or viable) to construct automated tests, thus the TCK can not claim to fully verify an implementation, however it 
is very helpful and is able to validate the most important rules.

The TCK is split up into 4 test classes which are to be extended by implementers, providing their `Publisher` / `Subscriber` 
/ `Processor` implementations for the test harness to validate.

The tests are split in the following way:

* `PublisherVerification`
* `SubscriberWhiteboxVerification`
* `SubscriberBlackboxVerification`
* `IdentityProcessorVerification`

The sections below include examples on how these can be used and describe the various configuration options.


```xml
<dependency>
  <groupId>io.smallrye.reactive</groupId>
  <artifactId>reactive-streams-junit5-tck</artifactId>
  <version>${last mutiny release}</version>
  <classifier>tests</classifier>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

Please refer to the [Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm) for the official,
 and to the last Mutiny release to find the latest version. Make sure that your Reactive Streams API and TCK dependency versions match.
