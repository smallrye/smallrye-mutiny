---
tags:
- guide
- advanced
---

# Can I have custom operators?

Yes, but please write operators responsibly!

Both `Uni` and `Multi` support custom operators using the `plug` operator.
Here is an example where we use a custom `Multi` operator that randomly drops items:

```java linenums="1"
{{ insert('java/guides/PlugTest.java', 'plug') }}
```

with the operator defined as follows:

```java linenums="1"
{{ insert('java/guides/PlugTest.java', 'custom-operator') }}
```


!!! caution
    
    Custom operators are an advanced feature: when possible please use the existing operators and use helpers such as `stage` to write readable code.
    
    In the case of custom `Multi` operators it is wise to test them against the _Reactive Streams TCK_.
