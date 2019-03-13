package io.microconfig.configs.provider;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.io.tree.ComponentTree;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.microconfig.configs.io.tree.ConfigFileFilters.*;
import static io.microconfig.environments.Component.byType;

@RequiredArgsConstructor
public class FileBasedConfigProvider implements ConfigProvider {
    private final ComponentTree componentTree;
    private final Set<String> configExtensions;
    private final ComponentParser componentParser;

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return collectComponentProperties(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> collectComponentProperties(Component component, String env, Set<Include> processedIncludes) {
        Map<String, Property> propertyByKey = new HashMap<>();
        Consumer<Predicate<File>> collectProperties = filter -> findProperties(filter, component, env, processedIncludes, propertyByKey);

        collectProperties.accept(defaultFilter(configExtensions));
        collectProperties.accept(envSharedFilter(configExtensions, env));
        collectProperties.accept(envSpecificFilter(configExtensions, env));

        return propertyByKey;
    }

    private void findProperties(Predicate<File> filter, Component component, String env,
                                Set<Include> processedIncludes, Map<String, Property> propertyByKey) {
        componentTree.getConfigFiles(component.getType(), filter)
                .map(file -> componentParser.parse(file, env))
                .forEach(c -> processComponent(c, propertyByKey, processedIncludes));
    }

    private void processComponent(ParsedComponent parsedComponent, Map<String, Property> destination, Set<Include> processedIncludes) {
        parsedComponent.getIncludes()
                .stream()
                .filter(include -> !processedIncludes.add(include))
                .map(include -> collectComponentProperties(byType(include.getComponent()), include.getEnv(), processedIncludes))
                .forEach(destination::putAll);

        destination.putAll(parsedComponent.getPropertiesAsMas());
    }
}