title=Welcome to the Vert.x Spider Project
date=2019-01-20
type=page
status=published
~~~~~~

A simple Web spider implemented with Vert.x

<script>
xmlhttp=new XMLHttpRequest();
xmlhttp.open("GET", "http://freelibrary.info/mvnlookup.php?project=vertx-spider", false);
xmlhttp.send();
$version = xmlhttp.responseText;
</script>

## Using Vert.x Pairtree

To use the Vert.x Pairtree library, reference it in your project's `pom.xml` file.

<pre><code>&lt;dependency&gt;
  &lt;groupId&gt;info.freelibrary&lt;/groupId&gt;
  &lt;artifactId&gt;vertx-spider&lt;/artifactId&gt;
  &lt;version&gt;<script>document.write($version);</script><noscript>${version}</noscript>&lt;/version&gt;
&lt;/dependency&gt;
</code></pre>

<br/>Or, to use it with Gradle/Grails, include the following in your project's `build.gradle` file:

<pre><code>compile &apos;info.freelibrary:vertx-spider:<script>
document.write($version);</script><noscript>${version}</noscript>&apos;</code></pre>
<p/>
## Building Vert.x Spider

If you'd like to build the project yourself, you'll need a current <a href="http://openjdk.java.net/" target="_blank">JDK</a> and <a href="https://maven.apache.org/" target="_blank">Maven</a> installed and added to your system path.  You can then download a [stable release](https://github.com/ksclarke/freelib-utils/releases) or clone the project using <a href="http://git-scm.com" target="_blank">Git</a>. To clone the project, type:

    git clone https://github.com/ksclarke/vertx-spider.git
    cd vertx-spider

<br/>To build the project, type:

    mvn install

<br/>To build the project's documentation, type:

    mvn javadoc:javadoc

<br/>For more information, consult the "Docs" dropdown in the navigation menu at the top of the page.
