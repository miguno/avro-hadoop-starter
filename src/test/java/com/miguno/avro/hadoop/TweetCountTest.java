package com.miguno.avro.hadoop;

import com.google.common.io.Resources;
import com.miguno.avro.Tweet;
import com.miguno.avro.util.AvroDataComparer;
import org.apache.avro.Schema;
import org.apache.avro.mapred.AvroJob;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.fest.assertions.api.Assertions.assertThat;

public class TweetCountTest extends ClusterMapReduceTestCase {

    private static final Logger LOG = Logger.getLogger(TweetCountTest.class);
    private static final boolean KEEP_SRC_FILE = false;
    private static final boolean OVERWRITE_EXISTING_DST_FILE = true;
    private static final boolean DO_REFORMAT_HDFS = true;
    private static final String SNAPPY_CODEC = "snappy";

    @Override
    protected void setUp() throws Exception {
        // Workaround that fixes NPE when trying to start MiniMRCluster;
        // for more information see http://grepalex.com/2012/10/20/hadoop-unit-testing-with-minimrcluster/
        System.setProperty("hadoop.log.dir", System.getProperty("java.io.tmpdir") + "/minimrcluster-logs");
        super.startCluster(DO_REFORMAT_HDFS, null);
    }

    public void testTweetCount() throws IOException {
        // given
        Path inputPath = new Path("testing/tweetcount/input");
        Path outputPath = new Path("testing/tweetcount/output");

        JobConf conf = createJobConf();
        updateJobConfiguration(conf, inputPath, outputPath);
        upload("avro/input.avro", inputPath);

        // when
        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion();

        // then
        assertThat(job.isSuccessful()).isTrue();

        Path[] outputFiles = FileUtil.stat2Paths(getFileSystem().listStatus(outputPath,
                new Utils.OutputFileUtils.OutputFilesFilter()));
        assertThat(outputFiles.length).isEqualTo(1);

        Path outputFile = outputFiles[0];
        assertThatAvroOutputIsIdentical("avro/output.avro", outputFile);
    }

    private void updateJobConfiguration(JobConf conf, Path inputPath, Path outputPath) {
        conf.setJobName("test-tweetcount");

        AvroJob.setInputSchema(conf, Tweet.getClassSchema());
        AvroJob.setOutputSchema(conf, Pair.getPairSchema(Schema.create(Schema.Type.STRING), Schema.create(
                Schema.Type.INT)));

        AvroJob.setMapperClass(conf, TweetCountMapper.class);
        AvroJob.setReducerClass(conf, TweetCountReducer.class);

        AvroJob.setOutputCodec(conf, SNAPPY_CODEC);

        FileInputFormat.setInputPaths(conf, inputPath);
        FileOutputFormat.setOutputPath(conf, outputPath);
    }

    private void upload(String resourceFile, Path dstPath) throws IOException {
        LOG.debug("Uploading " + resourceFile + " to " + dstPath);
        Path originalInputFile = new Path(Resources.getResource(resourceFile).getPath());
        Path testInputFile = new Path(dstPath, fileNameOf(resourceFile));
        getFileSystem().copyFromLocalFile(KEEP_SRC_FILE, OVERWRITE_EXISTING_DST_FILE, originalInputFile, testInputFile);
    }

    private String fileNameOf(String resourceFile) {
        return new Path(resourceFile).getName();
    }

    private void assertThatAvroOutputIsIdentical(String expectedOutputResourceFile, Path outputFile)
            throws IOException {
        LOG.debug("Comparing contents of " + expectedOutputResourceFile + " and " + outputFile);
        Path expectedOutput = new Path(Resources.getResource(expectedOutputResourceFile).getPath());
        Path tmpLocalOutput = createTempLocalPath();
        getFileSystem().copyToLocalFile(outputFile, tmpLocalOutput);
        assertThat(AvroDataComparer.haveIdenticalContents(expectedOutput, tmpLocalOutput)).isTrue();
        delete(tmpLocalOutput);
    }

    private Path createTempLocalPath() throws IOException {
        java.nio.file.Path path = Files.createTempFile("test-tweetcount-actual-output-", ".avro");
        // delete the temp file immediately -- we are just interested in the generated filename
        path.toFile().delete();
        return new Path(path.toAbsolutePath().toString());
    }

    private void delete(Path path) throws IOException {
        new File(path.toString()).delete();
    }

}
