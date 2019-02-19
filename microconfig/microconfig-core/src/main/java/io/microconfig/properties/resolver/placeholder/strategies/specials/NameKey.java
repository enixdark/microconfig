package io.microconfig.properties.resolver.placeholder.strategies.specials;

import io.microconfig.environments.Component;
import io.microconfig.environments.Environment;
import io.microconfig.properties.resolver.placeholder.strategies.SpecialResolverStrategy.SpecialKey;

import java.util.Optional;

import static java.util.Optional.of;

public class NameKey implements SpecialKey {
    @Override
    public String key() {
        return "name";
    }

    @Override
    public Optional<String> value(Component component, Environment environment) {
        return of(component.getName());
    }
}