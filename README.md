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

### TweetCount

The class ``com.miguno.avro.hadoop.TweetCount`` implements a MapReduce job that counts the number of tweets created by
Twitter users.

    TweetCount: Usage: TweetCount <input path> <output path>

### TweetCountTest

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

## Preliminaries

## How Streaming sees data when reading via AvroAsTextInputFormat

When using [AvroAsTextInputFormat](http://avro.apache.org/docs/1.7.4/api/java/org/apache/avro/mapred/AvroAsTextInputFormat.html)
as the input format your streaming code will receive the data in JSON format, one record ("datum" in Avro parlance) per
line.  Note that Avro will also add a trailing TAB (``\t``) at the end of each line.

    <JSON representation of Avro record #1>\t
    <JSON representation of Avro record #2>\t
    <JSON representation of Avro record #3>\t
    ...

Here's the basic data flow from your input data in binary Avro format to our streaming mapper:

    input.avro (binary)  ---AvroAsTextInputFormat---> deserialized data (JSON) ---> Mapper


## Examples

_Important: The examples below assume you have access to a running Hadoop cluster._

The example commands below use the Hadoop Streaming jar (for MRv1) shipped with Cloudera CDH4.  If you are not using
CDH4 just replace the jar file with the one included in your Hadoop installation.  The Avro jar files are straight
from the [Avro project](https://avro.apache.org/releases.html).

### Preparing the input data

The example input data we are using is ``twitter.avro`` from ``src/test/resources/avro/``.  Here is an excerpt of
``twitter.avro``, shown in JSON representation:

    {"username":"miguno","tweet":"Rock: Nerf paper, scissors is fine.","timestamp": 1366150681 }
    {"username":"BlizzardCS","tweet":"Works as intended.  Terran is IMBA.","timestamp": 1366154481 }
    {"username":"DarkTemplar","tweet":"From the shadows I come!","timestamp": 1366154681 }
    {"username":"VoidRay","tweet":"Prismatic core online!","timestamp": 1366160000 }


Upload ``twitter.avro`` to HDFS to make the input data available to our streaming jobs.

    # upload the input data
    $ hadoop fs -mkdir streaming/input
    $ hadoop fs -copyFromLocal src/test/resources/avro/twitter.avro streaming/input


### Reading Avro, writing plain-text

The following command reads Avro data from the relative HDFS directory ``streaming/input/`` (which normally resolves
to ``/user/<your-unix-username>/streaming/input/``).  It writes the
deserialized version of each data record (see section _How Streaming sees data when reading via AvroAsTextInputFormat_
above) as is to the output HDFS directory ``streaming/output/``.  For this simple demonstration we are using
the Unix tool ``cat`` as a naive map step implementation.  We do not need to run a reduce phase here, which is why
we disable the reduce step via the option ``-D mapred.reduce.tasks=0`` (see
[Specifying Map-Only Jobs](http://hadoop.apache.org/docs/r1.1.2/streaming.html#Specifying+Map-Only+Jobs) in the
Hadoop Streaming documenation).

    # run the streaming job
    $ hadoop jar hadoop-streaming-2.0.0-mr1-cdh4.2.0.jar \
        -D mapred.reduce.tasks=0 \
        -files avro-1.7.4.jar,avro-mapred-1.7.4-hadoop1.jar \
        -libjars avro-1.7.4.jar,avro-mapred-1.7.4-hadoop1.jar \
        -input  streaming/input/ \
        -output streaming/output/ \
        -mapper /bin/cat \
        -inputformat org.apache.avro.mapred.AvroAsTextInputFormat

Once the job completes you can inspect the output data as follows:

    $ hadoop fs -cat streaming/output/part-00000 | head -4
    {"username": "miguno", "tweet": "Rock: Nerf paper, scissors is fine.", "timestamp": 1366150681}
    {"username": "BlizzardCS", "tweet": "Works as intended.  Terran is IMBA.", "timestamp": 1366154481}
    {"username": "DarkTemplar", "tweet": "From the shadows I come!", "timestamp": 1366154681}
    {"username": "VoidRay", "tweet": "Prismatic core online!", "timestamp": 1366160000}


# Reading Avro, writing Avro

To write the output in Avro format instead of plain-text, use the same general options as in the previous example but
also add:

    -outputformat org.apache.avro.mapred.AvroTextOutputFormat

[AvroTextOutputFormat](http://avro.apache.org/docs/1.7.4/api/java/index.html?org/apache/avro/mapred/AvroTextOutputFormat.html)
is the equivalent of TextOutputFormat.  It writes Avro data files with a "bytes" schema.

Note that using ``cat`` as a naive mapper as shown in the previous example will not result in the output file being
identical to the input file.  This is because ``AvroTextOutputFormat`` will escape (quote) the input data it receives
from ``cat``.  An illustration might be worth a thousand words:

    # When using /bin/cat as mapper as in the previous example
    $ hadoop fs -copyToLocal streaming/output/part-00000.avro .
    $ java -jar avro-tools-1.7.4.jar tojson part-00000.avro  | head -4
    "{\"username\": \"miguno\", \"tweet\": \"Rock: Nerf paper, scissors is fine.\", \"timestamp\": 1366150681}\t"
    "{\"username\": \"BlizzardCS\", \"tweet\": \"Works as intended.  Terran is IMBA.\", \"timestamp\": 1366154481}\t"
    "{\"username\": \"DarkTemplar\", \"tweet\": \"From the shadows I come!\", \"timestamp\": 1366154681}\t"
    "{\"username\": \"VoidRay\", \"tweet\": \"Prismatic core online!\", \"timestamp\": 1366160000}\t"


# Related Documentation

* [Reading and Writing Avro Files From the Command Line](http://www.michael-noll.com/blog/2013/03/17/reading-and-writing-avro-files-from-the-command-line/)
