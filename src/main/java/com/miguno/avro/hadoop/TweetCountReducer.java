package com.miguno.avro.hadoop;

import org.apache.avro.mapred.AvroCollector;
import org.apache.avro.mapred.AvroReducer;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;

public class TweetCountReducer extends AvroReducer<CharSequence, Integer, Pair<CharSequence, Integer>> {

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
