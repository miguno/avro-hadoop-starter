package com.miguno.avro.hadoop;

/**
 * We are using this enum to manage shell exit codes.
 */
public enum ExitCode {
    /**
     * Indicates success of the MapReduce job.
     */
    SUCCESS(0),

    /**
     * Indicates that the MapReduce job was submitted but subsequently failed.
     */
    ERROR_JOB_FAILED(1),

    /**
     * Indicates that the user did not supply CLI options correctly.
     */
    ERROR_ILLEGAL_CLI_ARGUMENTS(1),

    /**
     * Indicates a failure of which we do not know more about.
     */
    ERROR_UNKNOWN_FAILURE(99);

    /**
     * The exit code that should be returned to the shell.
     */
    private int code;

    /**
     * @param code the shell exit code
     */
    ExitCode(int code) {
        this.code = code;
    }

    /**
     * Returns the exit code to be returned to the shell.
     * @return the exit code
     */
    public int intValue() {
      return this.code;
    }
}