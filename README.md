avro-hadoop-starter
===================

Example MapReduce jobs that read and/or write data in Avro format.

---

Table of Contents

* <a href="#Java">Java</a>
* <a href="#Hadoop Streaming">Hadoop Streaming</a>

---


<a name="Java"></a>

# Java

## Usage

To prepare your Java IDE:

    # IntelliJ IDEA
    $ gradle cleanIdea idea   # then File > Open... > avro-hadoop-starter.ipr

    # Eclipse
    $ gradle cleanEclipse eclipse


To build the Java code and to compile the Avro-based Java classes from the schemas (``*.avsc``) in
``src/main/resources/avro/``:

    $ gradle clean build

The generated Avro-based Java classes are written under the directory tree ``generated-sources/``.

To run the unit tests (notably ``TweetCountTest``, see section _Examples_ below):

    $ gradle test

Note: ``gradle test`` executes any JUnit unit tests.  If you add any TestNG unit tests you need to run ``gradle testng``
for executing those.


## Examples

The class ``com.miguno.avro.hadoop.TweetCount`` implements a MapReduce job that counts the number of tweets created by
Twitter users.  You can run this class from the command line.

    TweetCount: Usage: TweetCount <input path> <output path>

The class ``com.miguno.avro.hadoop.TweetCountTest`` is very similar to ``TweetCount``.  It uses a small test input Avro
file ``src/test/resources/avro/twitter.avro`` and runs a unit test with the same MapReduce job as ``TweetCount`` on it.
The unit test includes comparing the actual MapReduce output (in Snappy-compressed Avro format) with expected output.
``TweetCountTest`` extends ``org.apache.hadoop.mapred.ClusterMapReduceTestCase`` (Hadoop MRv1), which means that the
corresponding MapReduce job is launched in-memory via ``MiniMRCluster``.

Here is the Avro schema of the stub Twitter input data:

```json
{
  "type" : "record",
  "name" : "Tweet",
  "namespace" : "com.miguno.avro",
  "fields" : [ {
    "name" : "username",
    "type" : "string",
    "doc"  : "Name of the user account on Twitter.com"
  }, {
    "name" : "tweet",
    "type" : "string",
    "doc"  : "The content of the user's Twitter message"
  }, {
    "name" : "timestamp",
    "type" : "long",
    "doc"  : "Unix epoch time in seconds"
  } ],
  "doc:" : "A basic schema for storing Twitter messages"
}
```


## MiniMRCluster and Hadoop MRv2

The MiniMRCluster that is used by ``ClusterMapReduceTestCase`` in MRv1 is deprecated in Hadoop MRv2.  When using MRv2
you should switch to ``org.apache.hadoop.mapred.MiniMRClientClusterFactory``
([source](https://svn.apache.org/repos/asf/hadoop/common/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/MiniMRClientClusterFactory.java)),
which provides a wrapper interface called ``MiniMRClientCluster`` around the ``MiniMRYarnCluster``:

> MiniMRClientClusterFactory:
> A MiniMRCluster factory. In MR2, it provides a wrapper MiniMRClientCluster interface around the MiniMRYarnCluster.
> While in MR1, it provides such wrapper around MiniMRCluster. This factory should be used in tests to provide an easy
> migration of tests across MR1 and MR2.

See [Experimenting with MapReduce 2.0](http://blog.cloudera.com/blog/2012/07/experimenting-with-mapreduce-2-0/) for more
information.


## Further Reading

The Apache Avro documentation provides detailed information on how you should use the Avro Java API to implement
MapReduce jobs that read and/or write data in Avro format:

* [Package Documentation for org.apache.avro.mapred](http://avro.apache.org/docs/1.7.4/api/java/index.html?org/apache/avro/mapred/package-summary.html)
  -- Run Hadoop MapReduce jobs over Avro data, with map and reduce functions written in Java.


<a name="Hadoop Streaming"></a>

# Hadoop Streaming

TODO
