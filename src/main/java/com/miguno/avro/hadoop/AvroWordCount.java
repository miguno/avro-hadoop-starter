/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miguno.avro.hadoop;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.mapred.AvroJob;
import org.apache.avro.mapred.AvroWrapper;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * The classic WordCount example modified to output Avro Pair<CharSequence, Integer> records instead of text.
 */
public class AvroWordCount extends Configured implements Tool {

    private static final Logger LOG = Logger.getLogger(Map.class);

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {

        private static final IntWritable ONE = new IntWritable(1);

        private Text word = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter)
                throws IOException {
            String line = value.toString();
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                word.set(tokenizer.nextToken());
                output.collect(word, ONE);
            }
        }
    }

    public static class Reduce extends MapReduceBase
            implements Reducer<Text, IntWritable, AvroWrapper<Pair<CharSequence, Integer>>, NullWritable> {

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

    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            LOG.error(String.format("Usage: %s <input path> <output path>", getClass().getSimpleName()));
            return ExitCode.ERROR_ILLEGAL_CLI_ARGUMENTS.getCode();
        }

        JobConf conf = new JobConf(AvroWordCount.class);
        conf.setJobName("avro-wordcount");

        // We call setOutputSchema first so we can override the configuration parameters it sets
        AvroJob.setOutputSchema(conf, Pair.getPairSchema(Schema.create(Type.STRING), Schema.create(Type.INT)));

        conf.setMapperClass(Map.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(TextInputFormat.class);

        conf.setMapOutputKeyClass(Text.class);
        conf.setMapOutputValueClass(IntWritable.class);
        conf.setOutputKeyComparatorClass(Text.Comparator.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        RunningJob job = JobClient.runJob(conf);
        job.waitForCompletion();
        if (job.isSuccessful()) {
            return ExitCode.SUCCESS.getCode();
        }
        else {
            return ExitCode.ERROR_JOB_FAILED.getCode();
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new AvroWordCount(), args);
        System.exit(exitCode);
    }
}
