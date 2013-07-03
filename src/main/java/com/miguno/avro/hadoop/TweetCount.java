package com.miguno.avro.hadoop;

import com.miguno.avro.Tweet;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.mapred.AvroJob;
import org.apache.avro.mapred.Pair;
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
 * It uses AvroMapper and AvroReducer to implement the Map and Reduce steps, respectively.  The output will be stored in
 * Snappy-compressed Avro format.
 * <p/>
 * When you run this class you must supplied it with two parameters: first, the path to the input data; second, the path
 * where to store the output data.
 */
public class TweetCount extends Configured implements Tool {

    private static final Logger LOG = Logger.getLogger(TweetCount.class);
    private static final String SNAPPY_CODEC = "snappy";

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

        // AvroJob#setInputSchema() and AvroJob#setOutputSchema() set relevant config options such as input/output
        // format, map output classes, and output key class.
        AvroJob.setInputSchema(conf, Tweet.getClassSchema());
        AvroJob.setOutputSchema(conf, Pair.getPairSchema(Schema.create(Type.STRING), Schema.create(Type.INT)));

        AvroJob.setMapperClass(conf, TweetCountMapper.class);
        AvroJob.setReducerClass(conf, TweetCountReducer.class);

        AvroJob.setOutputCodec(conf, SNAPPY_CODEC);

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
