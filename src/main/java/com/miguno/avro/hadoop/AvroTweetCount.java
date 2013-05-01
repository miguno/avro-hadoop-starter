package com.miguno.avro.hadoop;

import com.miguno.avro.Tweet;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.mapred.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Iterator;

/**
 * This MapReduce job counts the number of tweets created by Twitter users.
 * <p/>
 * It outputs Avro Pair<CharSequence, Integer> records instead of text.
 * <p/>
 * Adapted from AvroWordCount in Apache Avro.
 */
public class AvroTweetCount extends Configured implements Tool {

    private static final Logger LOG = Logger.getLogger(Map.class);

      private static class Map extends MapReduceBase implements Mapper<AvroWrapper<Tweet>, NullWritable, Text, IntWritable> {

        private static final IntWritable ONE = new IntWritable(1);

        private Text username = new Text();

        @Override
        public void map(AvroWrapper<Tweet> key, NullWritable value, OutputCollector<Text, IntWritable> output, Reporter reporter)
                throws IOException {
            username.set(key.datum().getUsername().toString());
            output.collect(username, ONE);
        }
    }

    private static class Reduce extends MapReduceBase
            implements Reducer<Text, IntWritable, AvroWrapper<Pair<CharSequence, Integer>>, NullWritable> {

        @Override
        public void reduce(Text key, Iterator<IntWritable> values,
                OutputCollector<AvroWrapper<Pair<CharSequence, Integer>>, NullWritable> output, Reporter reporter)
                throws IOException {
            int sum = 0;
            while (values.hasNext()) {
                sum += values.next().get();
            }
            output.collect(new AvroWrapper<>(new Pair<CharSequence, Integer>(key.toString(), sum)), NullWritable.get());
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            LOG.error(String.format("Usage: %s <input path> <output path>", getClass().getSimpleName()));
            return ExitCode.ERROR_ILLEGAL_CLI_ARGUMENTS.intValue();
        }

        JobConf conf = new JobConf(AvroTweetCount.class);
        conf.setJobName("avro-tweetcount");

        // Note that AvroJob#setInputSchema() and AvroJob#setOutputSchema() set relevant config options such as
        // input/output format, map output classes, and output key class.
        AvroJob.setInputSchema(conf, Tweet.getClassSchema());
        // We call setOutputSchema first so we can override the configuration parameters it sets
        AvroJob.setOutputSchema(conf, Pair.getPairSchema(Schema.create(Type.STRING), Schema.create(Type.INT)));

        conf.setMapperClass(Map.class);
        conf.setReducerClass(Reduce.class);

        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(IntWritable.class);
        conf.setOutputKeyComparatorClass(Text.Comparator.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion();
        if (job.isSuccessful()) {
            return ExitCode.SUCCESS.intValue();
        }
        else {
            return ExitCode.ERROR_JOB_FAILED.intValue();
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new AvroTweetCount(), args);
        System.exit(exitCode);
    }
}
