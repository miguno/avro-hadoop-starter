package com.miguno.avro.hadoop;

import com.miguno.avro.Tweet;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.mapred.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * This MapReduce job counts the number of tweets created by Twitter users.
 * <p/>
 * It uses AvroMapper and AvroReducer to implement the Map and Reduce steps, respectively.
 */
public class TweetCount extends Configured implements Tool {

    private static final Logger LOG = Logger.getLogger(MyMapper.class);

    private static class MyMapper extends AvroMapper<Tweet, Pair<CharSequence, Integer>> {

        private static final Integer ONE = Integer.valueOf(1);

        private CharSequence username;

        @Override
        public void map(Tweet tweet, AvroCollector<Pair<CharSequence, Integer>> collector, Reporter reporter)
                throws IOException {
            username = tweet.getUsername();
            collector.collect(new Pair<CharSequence, Integer>(username, ONE));
        }
    }

    private static class MyReducer extends AvroReducer<CharSequence, Integer, Pair<CharSequence, Integer>> {

        @Override
        public void reduce(CharSequence username, Iterable<Integer> tweetCounts,
                AvroCollector<Pair<CharSequence, Integer>> collector, Reporter reporter) throws IOException {
            int numTotalTweets = 0;
            for (Integer count : tweetCounts) {
                numTotalTweets += count.intValue();
            }
            collector.collect(new Pair<CharSequence, Integer>(username, numTotalTweets));
        }

    }

    @Override
    public int run(String[] args) throws Exception {
        if (!hasValidCommandLineParameters(args)) {
            return ExitCode.ERROR_ILLEGAL_CLI_ARGUMENTS.intValue();
        }
        Path inputPath = extractInputPathFromArgs(args);
        Path outputPath = extractOutputPathFromArgs(args);
        JobConf conf = createJobConfiguration(inputPath, outputPath);
        RunningJob job = runAndWaitForJobToFinish(conf);
        ExitCode exitCode = deriveExitCodeBasedOnJobSuccess(job);
        return exitCode.intValue();
    }

    private boolean hasValidCommandLineParameters(String[] args) {
        if (args.length != 2) {
            LOG.error(String.format("Usage: %s <input path> <output path>", getClass().getSimpleName()));
            return false;
        }
        else {
            return true;
        }
    }

    private Path extractInputPathFromArgs(String[] args) {
        return new Path(args[0]);
    }

    private Path extractOutputPathFromArgs(String[] args) {
        return new Path(args[1]);
    }

    private JobConf createJobConfiguration(Path inputPath, Path outputPath) {
        JobConf conf = new JobConf(TweetCount.class);
        conf.setJobName("tweetcount");

        // Note that AvroJob#setInputSchema() and AvroJob#setOutputSchema() set relevant config options such as
        // input/output format, map output classes, and output key class.
        AvroJob.setInputSchema(conf, Tweet.getClassSchema());
        AvroJob.setOutputSchema(conf, Pair.getPairSchema(Schema.create(Type.STRING), Schema.create(Type.INT)));

        AvroJob.setMapperClass(conf, MyMapper.class);
        AvroJob.setReducerClass(conf, MyReducer.class);

        FileInputFormat.setInputPaths(conf, inputPath);
        FileOutputFormat.setOutputPath(conf, outputPath);
        return conf;
    }

    private RunningJob runAndWaitForJobToFinish(JobConf conf) throws IOException {
        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion();
        return job;
    }

    private ExitCode deriveExitCodeBasedOnJobSuccess(RunningJob job) throws IOException {
        if (job.isSuccessful()) {
            return ExitCode.SUCCESS;
        }
        else {
            return ExitCode.ERROR_JOB_FAILED;
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new TweetCount(), args);
        System.exit(exitCode);
    }
}
