# Mutiny Documentation

This module contains the Mutiny documentation.
All the content from the website here.

## Structure

The site uses [Jekyll](https://jekyllrb.com/).
The `src/main/jekyll` directory contains all the assets uses for the site:

* guides - `src/main/jekyll/guides`
* getting-started - `src/main/jekyll/getting-started`
* styles - `src/main/jekyll/_sass`
* images - `src/main/jekyll/assets/images`
* ...

Code snippets are located in `src/test/java`.
Each snippet is in a _test_ which guarantees that the snippet compiles and works as documented.

## Generating the web site

### Prerequisites

1. Be sure you have `rbenv` installed
2. Run `rbenv init`
3. (Optional) Verify with `curl -fsSL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-doctor | bash`
4. Run:
```bash
RUBY_CONFIGURE_OPTS="--with-openssl-dir=/usr/local/opt/openssl" rbenv install -v 2.7.1
rbenv global 2.7.1
ruby -v # Should print 2.7.1
gem install --user-install bundler jekyll
bundle install
```

### Generating the web site

From the root of this module, run:

```bash
mvn clean test && cd src/main/jekyll  && bundle exec jekyll build  && cd -
```

The web site is generated in `target/_site`.

### Hot reload

To develop the web site and gets hot reload, run:

```bash
cd src/main/jekyll
bundle exec jekyll serve 
```

You can browse the web site on http://127.0.0.1:4000/mutiny-doc-sandbox/.


## Adding a guide

To add a guide, create an _.adoc_ file in `src/main/jekyll/guides`. 
The file name will be the id of the guide.

A guide must have a set of attributes.
Add the following attributes at the top of the created file

```
:page-guide-id: file-name # Update to set the file name 
:page-liquid:
:page-show-toc: true  # Enable or Disable the table of content.
:include_dir: ../../../../src/test/java/guides/ # Ease snippet integration
``` 

Then, write your guide. 
The first level of header must be `==`.
If your guide has no headers, disable the table of content using the following attribute: `:page-show-toc: false`

To include a snippet, create a `Test` in `src/main/java/guides`.
All snippets must be tests, as it guarantees they work.
For example:

```java
package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetitionsTest {

    @Test
    public void distinct() {
        Multi<Integer> multi = Multi.createFrom().items(1, 1, 2, 3, 4, 5, 5, 6,  1, 4, 4);
        // tag::distinct[]
        List<Integer> list = multi
                .transform().byDroppingDuplicates()
                .collectItems().asList()
                .await().indefinitely();
        // end::distinct[]

        // tag::repetition[]
        List<Integer> list2 = multi
                .transform().byDroppingRepetitions()
                .collectItems().asList()
                .await().indefinitely();
        // end::repetition[]
        assertThat(list).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6, 1, 4);
    }
}
```

You can have multiple test methods. 
The snippet to include are delimited using `tag::name[]` and `end::name[]`, with `name` being your snippet identifier.

Then, go back to your `.adoc` file and include the snippet using:

```
[source,java,indent=0]
----
include::{include_dir}/YourTestClass.java[tag=snippet-identifier]
----
```

Note that you may have to adjust your `include_dir` attribute if you use sub-packages.

**IMPORTANT**: Do not forget the `indent=0` attribute.

Once your guide is written, you need to add it to the list.
Edit the `src/main/jekyll/_data/guides.yml` file, and add your guide at the end of the file:

```yaml
guide-id: # to be updated with your file name
  title: your title
  text: a one sentend description, ending with a dot.
  labels:
    - beginner # labels
  related:
    - guide: filter # the id of a related guide, you can have multiple related guide)
    - url: https://javadoc.io/doc/io.smallrye.reactive/mutiny/latest/io/smallrye/mutiny/groups/MultiTransform.html # an absolute url for external content
      text: <tt>MultiTransform</tt>  # The external content title (HTML)
```

The _related_ section contains a set of related content.
You can have either `guide` - in this case just indicate the id of the guide; or external resources - in this case you indicate the url and the text.

Once added to the `guides.yml` file, edit the `src/main/jekyll/_data/categories.yml` file, and add your guide to the associated category:

```yaml
- name: Misc
  guides:
    - unchecked
    - my-new-guide # use your id
``` 

## Sandbox deployment

The web site is deployed in a sandbox project until the official switch.

**IMPORTANT**: Before running the deployment, you must have a `GITHUB_TOKEN` declared in your environment: `export GITHUB_TOKEN=...`

To deploy the web site to the sandbox, run:

```bash
./deploy-to-sandbox.sh
```
