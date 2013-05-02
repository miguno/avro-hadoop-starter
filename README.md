avro-hadoop-starter
===================

Example MapReduce jobs that read and/or write data in Avro format.

---

Table of Contents

* <a href="#Java">Java</a>
    * <a href="#Usage-Java">Usage</a>
    * <a href="#Examples-Java">Examples</a>
    * <a href="#MiniMRCluster and Hadoop MRv2">MiniMRCluster and Hadoop MRv2</a>
    * <a href="#Further readings on Java">Further readings on Java</a>
* <a href="#Hadoop Streaming">Hadoop Streaming</a>
    * <a href="#Preliminaries-Streaming">Preliminaries</a>
    * <a href="#Streaming data">How Streaming sees data when reading via AvroAsTextInputFormat</a>
    * <a href="#Examples-Streaming">Examples</a>
    * <a href="#Further readings on Hadoop Streaming">Further readings on Hadoop Streaming</a>
* <a href="#Hive">Hive</a>
* <a href="#Pig">Pig</a>
* <a href="#Related documentation">Related documentation</a>

---


<a name="Java"></a>

# Java


<a name="Usage-Java"></a>

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


<a name="Examples-Java"></a>

## Examples

### TweetCount

[TweetCount](src/main/java/com/miguno/avro/hadoop/TweetCount.java) implements a MapReduce job that counts the number
of tweets created by Twitter users.

    TweetCount: Usage: TweetCount <input path> <output path>


### TweetCountTest

[TweetCountTest](src/test/java/com/miguno/avro/hadoop/TweetCountTest.java) is very similar to ``TweetCount``.  It uses
a small test input Avro file [twitter.avro](src/test/resources/avro/twitter.avro) and runs a unit test on it with the
same MapReduce job as ``TweetCount``.  The unit test includes comparing the actual MapReduce output (in
Snappy-compressed Avro format) with expected output.  ``TweetCountTest`` extends
[ClusterMapReduceTestCase](https://github.com/apache/hadoop-common/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/ClusterMapReduceTestCase.java)
(MRv1), which means that the corresponding MapReduce job is launched in-memory via
[MiniMRCluster](https://github.com/apache/hadoop-common/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/MiniMRCluster.java).

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


<a name="MiniMRCluster and Hadoop MRv2"></a>

## MiniMRCluster and Hadoop MRv2

The MiniMRCluster that is used by ``ClusterMapReduceTestCase`` in MRv1 is deprecated in Hadoop MRv2.  When using MRv2
you should switch to
[MiniMRClientClusterFactory](https://github.com/apache/hadoop-common/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapred/MiniMRClientClusterFactory.java),
which provides a wrapper interface called ``MiniMRClientCluster`` around the
[MiniMRYarnCluster](https://github.com/apache/hadoop-common/blob/trunk/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-jobclient/src/test/java/org/apache/hadoop/mapreduce/v2/MiniMRYarnCluster.java) (MRv2):

> MiniMRClientClusterFactory:
> A MiniMRCluster factory. In MR2, it provides a wrapper MiniMRClientCluster interface around the MiniMRYarnCluster.
> While in MR1, it provides such wrapper around MiniMRCluster. This factory should be used in tests to provide an easy
> migration of tests across MR1 and MR2.

See [Experimenting with MapReduce 2.0](http://blog.cloudera.com/blog/2012/07/experimenting-with-mapreduce-2-0/) for more
information.


<a name="Further readings on Java"></a>

## Further readings on Java

* [Package Documentation for org.apache.avro.mapred](http://avro.apache.org/docs/1.7.4/api/java/index.html?org/apache/avro/mapred/package-summary.html)
  -- Run Hadoop MapReduce jobs over Avro data, with map and reduce functions written in Java.  This document provides
  detailed information on how you should use the Avro Java API to implement MapReduce jobs that read and/or write data
  in Avro format.
* [Java MapReduce and Avro](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Installation-Guide/cdh4ig_topic_26_5.html)
  -- Cloudera CDH4 documentation


<a name="Hadoop Streaming"></a>

# Hadoop Streaming


<a name="Preliminaries-Streaming"></a>

## Preliminaries

Important: The examples below assume you have access to a running Hadoop cluster.


<a name="Streaming data"></a>

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


<a name="Examples-Streaming"></a>

## Examples


### Prerequisites

The example commands below use the Hadoop Streaming jar _for MRv1_ shipped with Cloudera CDH4:

* [hadoop-streaming-2.0.0-mr1-cdh4.2.0.jar](https://repository.cloudera.com/artifactory/cloudera-repos/org/apache/hadoop/hadoop-streaming/2.0.0-mr1-cdh4.2.0/hadoop-streaming-2.0.0-mr1-cdh4.2.0.jar)
  (as of April 2013)

If you are not using Cloudera CDH4 or are using a new version of CDH4 just replace the jar file with the one included
in your Hadoop installation.

The Avro jar files are straight from the [Avro project](https://avro.apache.org/releases.html):

* [avro-1.7.4.jar](http://www.eu.apache.org/dist/avro/avro-1.7.4/java/avro-1.7.4.jar)
* [avro-mapred-1.7.4-hadoop1.jar](http://www.eu.apache.org/dist/avro/avro-1.7.4/java/avro-mapred-1.7.4-hadoop1.jar)
* [avro-tools-1.7.4.jar](http://www.eu.apache.org/dist/avro/avro-1.7.4/java/avro-tools-1.7.4.jar)


### Preparing the input data

The example input data we are using is [twitter.avro](src/test/resources/avro/twitter.avro).  Here is an excerpt of
``twitter.avro``, shown in JSON representation:

    $ java -jar avro-tools-1.7.4.jar tojson src/test/resources/avro/twitter.avro | head -4
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
the ``IdentityMapper`` as a naive map step implementation -- it outputs its input data unmodified (equivalently we
coud use the Unix tool ``cat``, here) .  We do not need to run a reduce phase here, which is why we disable the reduce
step via the option ``-D mapred.reduce.tasks=0`` (see
[Specifying Map-Only Jobs](http://hadoop.apache.org/docs/r1.1.2/streaming.html#Specifying+Map-Only+Jobs) in the
Hadoop Streaming documenation).

    # run the streaming job
    $ hadoop jar hadoop-streaming-2.0.0-mr1-cdh4.2.0.jar \
        -D mapred.job.name="avro-streaming" \
        -D mapred.reduce.tasks=0 \
        -files avro-1.7.4.jar,avro-mapred-1.7.4-hadoop1.jar \
        -libjars avro-1.7.4.jar,avro-mapred-1.7.4-hadoop1.jar \
        -input  streaming/input/ \
        -output streaming/output/ \
        -mapper org.apache.hadoop.mapred.lib.IdentityMapper \
        -inputformat org.apache.avro.mapred.AvroAsTextInputFormat

Once the job completes you can inspect the output data as follows:

    $ hadoop fs -cat streaming/output/part-00000 | head -4
    {"username": "miguno", "tweet": "Rock: Nerf paper, scissors is fine.", "timestamp": 1366150681}
    {"username": "BlizzardCS", "tweet": "Works as intended.  Terran is IMBA.", "timestamp": 1366154481}
    {"username": "DarkTemplar", "tweet": "From the shadows I come!", "timestamp": 1366154681}
    {"username": "VoidRay", "tweet": "Prismatic core online!", "timestamp": 1366160000}

Please be aware that the output data just happens to be JSON.  This is because we opted not to modify any of the input
data in our MapReduce job.  And since the input data to our MapReduce job is deserialized by Avro into JSON, the output
turns out to be JSON, too.  With a different MapReduce job you could of course write the output data in TSV or CSV
format, for instance.


### Reading Avro, writing Avro

#### AvroTextOutputFormat (implies "bytes" schema)

To write the output in Avro format instead of plain-text, use the same general options as in the previous example but
also add:

    -outputformat org.apache.avro.mapred.AvroTextOutputFormat

[AvroTextOutputFormat](http://avro.apache.org/docs/1.7.4/api/java/index.html?org/apache/avro/mapred/AvroTextOutputFormat.html)
is the equivalent of TextOutputFormat.  It writes Avro data files with a "bytes" schema.

Note that using ``IdentityMapper`` as a naive mapper as shown in the previous example will not result in the output file
being identical to the input file.  This is because ``AvroTextOutputFormat`` will escape (quote) the input data it
receives from ``cat``.  An illustration might be worth a thousand words:

    # After having used IdentityMapper as in the previous example
    $ hadoop fs -copyToLocal streaming/output/part-00000.avro .

    $ java -jar avro-tools-1.7.4.jar tojson part-00000.avro  | head -4
    "{\"username\": \"miguno\", \"tweet\": \"Rock: Nerf paper, scissors is fine.\", \"timestamp\": 1366150681}\t"
    "{\"username\": \"BlizzardCS\", \"tweet\": \"Works as intended.  Terran is IMBA.\", \"timestamp\": 1366154481}\t"
    "{\"username\": \"DarkTemplar\", \"tweet\": \"From the shadows I come!\", \"timestamp\": 1366154681}\t"
    "{\"username\": \"VoidRay\", \"tweet\": \"Prismatic core online!\", \"timestamp\": 1366160000}\t"


#### Custom Avro output schema

This looks not to be supported by stock Avro at the moment.  A related JIRA ticket
[AVRO-1067](https://issues.apache.org/jira/browse/AVRO-1067), created in April 2012, is still unresolved as of May 2013.

For a workaround take a look at the section _Avro output for Hadoop Streaming_ at
[avro-utils](https://github.com/tomslabs/avro-utils), a third-party library for Avro.


#### Enabling compression of Avro output data (Snappy or Deflate)

If you want to enable compression for the Avro output data, you must add the following parameters to the streaming job:

    # for Snappy
    -D mapred.output.compress=true -D avro.output.codec=snappy

    # for Deflate
    -D mapred.output.compress=true -D avro.output.codec=deflate

Be aware that if you enable compression with ``mapred.output.compress`` but are NOT specifying an Avro output format
(such as AvroTextOutputFormat) your cluster's configured default compression codec will determine the final format
of the output data.  For instance, if ``mapred.output.compression.codec`` is set to
``com.hadoop.compression.lzo.LzopCodec`` then the job's output files would be compressed with LZO (e.g. you would
see ``part-00000.lzo`` output files instead of uncompressed ``part-00000`` files).

See also [Compression and Avro](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Installation-Guide/cdh4ig_topic_26_2.html)
in the CDH4 documentation.


<a name="Further readings on Hadoop Streaming"></a>

## Further readings on Hadoop Streaming

* [Streaming and Avro](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Installation-Guide/cdh4ig_topic_26_6.html)
  -- Cloudera CDH4 documentation


<a name="Hive"></a>

# Hive

TODO


<a name="Pig"></a>

# Pig

TODO


<a name="Related documentation"></a>

# Related documentation

* [Reading and Writing Avro Files From the Command Line](http://www.michael-noll.com/blog/2013/03/17/reading-and-writing-avro-files-from-the-command-line/)
