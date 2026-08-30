package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>minecraft</b> with <b>com.mojang:minecraft</b> coordinates and
     * with version reference <b>minecraft</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getMinecraft() {
        return create("minecraft");
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class VersionAccessors extends VersionFactory  {

        private final JavaVersionAccessors vaccForJavaVersionAccessors = new JavaVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>loom</b> with value <b>1.16.+</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLoom() { return getVersion("loom"); }

        /**
         * Version alias <b>minecraft</b> with value <b>1.21.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMinecraft() { return getVersion("minecraft"); }

        /**
         * Group of versions at <b>versions.java</b>
         */
        public JavaVersionAccessors getJava() {
            return vaccForJavaVersionAccessors;
        }

    }

    public static class JavaVersionAccessors extends VersionFactory  {

        public JavaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>java.version</b> with value <b>21</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getVersion() { return getVersion("java.version"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {
        private final ConfigurePluginAccessors paccForConfigurePluginAccessors = new ConfigurePluginAccessors(providers, config);
        private final ConsumePluginAccessors paccForConsumePluginAccessors = new ConsumePluginAccessors(providers, config);
        private final LoomPluginAccessors paccForLoomPluginAccessors = new LoomPluginAccessors(providers, config);
        private final ProvidePluginAccessors paccForProvidePluginAccessors = new ProvidePluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of plugins at <b>plugins.configure</b>
         */
        public ConfigurePluginAccessors getConfigure() {
            return paccForConfigurePluginAccessors;
        }

        /**
         * Group of plugins at <b>plugins.consume</b>
         */
        public ConsumePluginAccessors getConsume() {
            return paccForConsumePluginAccessors;
        }

        /**
         * Group of plugins at <b>plugins.loom</b>
         */
        public LoomPluginAccessors getLoom() {
            return paccForLoomPluginAccessors;
        }

        /**
         * Group of plugins at <b>plugins.provide</b>
         */
        public ProvidePluginAccessors getProvide() {
            return paccForProvidePluginAccessors;
        }

    }

    public static class ConfigurePluginAccessors extends PluginFactory {

        public ConfigurePluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>configure.base</b> with plugin id <b>configure-base</b> and
         * with <b>no version specified</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getBase() { return createPlugin("configure.base"); }

    }

    public static class ConsumePluginAccessors extends PluginFactory {

        public ConsumePluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>consume.common</b> with plugin id <b>consume-common</b> and
         * with <b>no version specified</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getCommon() { return createPlugin("consume.common"); }

    }

    public static class LoomPluginAccessors extends PluginFactory  implements PluginNotationSupplier{

        public LoomPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>loom</b> with plugin id <b>net.fabricmc.fabric-loom</b> and
         * with version reference <b>loom</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> asProvider() { return createPlugin("loom"); }

        /**
         * Plugin provider for <b>loom.remap</b> with plugin id <b>net.fabricmc.fabric-loom-remap</b> and
         * with version reference <b>loom</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getRemap() { return createPlugin("loom.remap"); }

    }

    public static class ProvidePluginAccessors extends PluginFactory {

        public ProvidePluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>provide.common</b> with plugin id <b>provide-common</b> and
         * with <b>no version specified</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getCommon() { return createPlugin("provide.common"); }

    }

}
