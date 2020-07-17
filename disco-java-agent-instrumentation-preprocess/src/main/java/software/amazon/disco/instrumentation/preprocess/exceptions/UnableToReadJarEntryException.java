package software.amazon.disco.instrumentation.preprocess.exceptions;

import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

/**
 * Exception thrown when the {@link ModuleExportStrategy exporter}
 * fails to read an exiting entry from the original Jar file.
 */
public class UnableToReadJarEntryException extends RuntimeException {
    /**
     * Constructor
     *
     * @param entryName {@link java.util.jar.JarEntry} that failed to be copied
     * @param cause     {@link Throwable cause} of the failure for tracing the root cause.
     */
    public UnableToReadJarEntryException(String entryName, Throwable cause) {
        super(PreprocessConstants.MESSAGE_PREFIX + "Failed to read Jar entry: " + entryName, cause);
    }
}
