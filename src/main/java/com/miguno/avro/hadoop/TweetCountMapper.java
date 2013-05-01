package com.miguno.avro.hadoop;

import com.miguno.avro.Tweet;
import org.apache.avro.mapred.AvroCollector;
import org.apache.avro.mapred.AvroMapper;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;

public class TweetCountMapper extends AvroMapper<Tweet, Pair<CharSequence, Integer>> {

    private static final Integer ONE = Integer.valueOf(1);

    private CharSequence username;

    @Override
    public void map(Tweet tweet, AvroCollector<Pair<CharSequence, Integer>> collector, Reporter reporter)
            throws IOException {
        username = tweet.getUsername();
        collector.collect(new Pair<CharSequence, Integer>(username, ONE));
    }

}
