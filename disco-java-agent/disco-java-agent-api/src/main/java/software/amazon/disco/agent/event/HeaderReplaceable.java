package software.amazon.disco.agent.event;

/**
 * A composable interface that Disco Events can implement if consumers of those events should be able to manipulate
 * the headers of the request associated with the event.
 */
public interface HeaderReplaceable {

    /**
     * Creates the provided header if it does not exist yet, or replaces the header if it does already exist.
     * This should be overridden in a concrete Event class if the functionality is available.
     *
     * @param key - key of the header to create or replace
     * @param value - value of the header
     * @return true if the header is successfully replaced
     */
    boolean replaceHeader(String key, String value);
}
