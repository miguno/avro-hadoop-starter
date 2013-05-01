package com.miguno.avro.util;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class AvroDataComparer {

    private static final Logger LOG = Logger.getLogger(AvroDataComparer.class);

    public static boolean haveIdenticalContents(Path first, Path second) throws IOException {
        File firstFile = new File(first.toUri().getPath());
        DatumReader<GenericRecord> firstDatumReader = new GenericDatumReader<>();
        DataFileReader<GenericRecord> firstDataFileReader = new DataFileReader<>(firstFile, firstDatumReader);

        File secondFile = new File(second.toUri().getPath());
        DatumReader<GenericRecord> secondDatumReader = new GenericDatumReader<>();
        DataFileReader<GenericRecord> secondDataFileReader = new DataFileReader<>(secondFile, secondDatumReader);

        GenericRecord recordFromFirst = null;
        GenericRecord recordFromSecond = null;
        while (firstDataFileReader.hasNext()) {
            if (!secondDataFileReader.hasNext()) {
                LOG.error("XXX Second file has less records than first file");
                return false;
            }
            // Reuse record objects by passing it to next(). This saves us from allocating and garbage collecting many
            // objects for files with many items.
            recordFromFirst = firstDataFileReader.next(recordFromFirst);
            recordFromSecond = secondDataFileReader.next(recordFromSecond);
            if (!recordFromFirst.equals(recordFromSecond)) {
                LOG.error("XXX First record != second record (" + recordFromFirst + " // " + recordFromSecond + ")");
                return false;
            }
        }
        try {
            firstDataFileReader.close();
        }
        catch (IOException e) {
            LOG.warn("Could not close data file reader for " + first);
        }
        try {
            secondDataFileReader.close();
        }
        catch (IOException e) {
            LOG.warn("Could not close data file reader for " + second);
        }
        return true;
    }
}
