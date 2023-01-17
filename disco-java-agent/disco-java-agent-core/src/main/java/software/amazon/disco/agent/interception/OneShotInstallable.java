package software.amazon.disco.agent.interception;

/**
 * Some of our core installables are "one shot". They each intercept a particular class (not "any subclass of"),
 * and furthermore these are JDK classes and so can only be loaded a maximum of once, into the bootstrap classloader,
 * therefore we know that once they have been applied, they are dead weight. This interface is internal to Agent Core
 * because using it outside of Core would be dangerous, and generally wrong for any non-bootstrap class. Even when an
 * interceptor appears to only type match one specific class, that class could be loaded multiple times into multiple
 * loaders.
 */
public interface OneShotInstallable extends Installable {
    /**
     * This method is called by {@link InterceptionInstaller} before the {@link java.lang.instrument.ClassFileTransformer}
     * created from this {@link Installable} is removed. In this method the installable should try to load any classes
     * it's intercepting, so that they are transformed. This will be the last chance to do so, before the ClassFileTransformer
     * is removed.
     */
    void beforeDisposal();

    /**
     * Standard recipe for forcing the loading of a class that OneShotInstallers should use in beforeDisposal().
     * @param clazz Class object representing the class to load.
     */
    static void forceClassLoad(Class<?> clazz) {
        clazz.getClassLoader();
    }
}
