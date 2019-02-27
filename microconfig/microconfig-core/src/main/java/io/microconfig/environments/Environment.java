package io.microconfig.environments;

import lombok.Getter;

import java.util.*;

import static io.microconfig.utils.CollectionUtils.singleValue;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Getter
public class Environment {
    private final String name;
    private final List<ComponentGroup> componentGroups;

    private final Optional<String> ip;
    private final Optional<Integer> portOffset;
    private final Optional<EnvInclude> include;

    public Environment(String name,
                       List<ComponentGroup> componentGroups,
                       Optional<String> ip, Optional<Integer> portOffset,
                       Optional<EnvInclude> include) {
        this.name = requireNonNull(name);
        this.componentGroups = unmodifiableList(requireNonNull(componentGroups));
        this.ip = requireNonNull(ip);
        this.portOffset = requireNonNull(portOffset);
        this.include = requireNonNull(include);
    }

    public List<ComponentGroup> getGroupByIp(String serverIp) {
        return componentGroups.stream()
                .filter(g -> of(serverIp).equals(g.getIp()))
                .collect(toList());
    }

    public ComponentGroup getGroupByName(String groupName) {
        List<ComponentGroup> collect = componentGroups.stream()
                .filter(g -> g.getName().equals(groupName))
                .collect(toList());

        if (collect.isEmpty()) {
            throw new IllegalArgumentException("Can't find group with name [" + groupName + "] in env [" + name + "]");
        }

        return singleValue(collect);
    }

    public Optional<ComponentGroup> getGroupByComponentName(String componentName) {
        List<ComponentGroup> groups = componentGroups.stream()
                .filter(g -> g.getComponents().stream().anyMatch(c -> c.getName().equals(componentName)))
                .collect(toList());

        return groups.isEmpty() ? empty() : of(singleValue(groups));
    }

    public List<Component> getComponentsByGroup(String group) {
        return getGroupByName(group).getComponents();
    }

    public List<Component> getAllComponents() {
        return getComponentGroups()
                .stream()
                .flatMap(group -> group.getComponents().stream())
                .collect(toList());
    }

    public Optional<Component> getComponentByName(String componentName) {
        return componentGroups.stream()
                .map(componentGroup -> componentGroup.getComponentByName(componentName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public Environment verifyUniqueComponentNames() {
        Set<String> unique = new HashSet<>();
        componentGroups.stream()
                .map(ComponentGroup::getComponents)
                .flatMap(Collection::stream)
                .filter(c -> !unique.add(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Env [" + name + "] contains several definitions of [" + c.getName() + "] component");
                });

        return this;
    }

    public Environment verifyIpsPresent() {
        componentGroups.stream()
                .filter(g -> !ip.isPresent() && !g.getIp().isPresent())
                .findFirst()
                .ifPresent(g -> {
                    throw new IllegalArgumentException("Env [" + name + "] doesn't have ip for [" + g.getName() + "] componentGroup");
                });

        return this;
    }

    public Environment processInclude(EnvironmentProvider environmentProvider) {
        return include.isPresent() ? include.get().includeTo(this, environmentProvider) : this;
    }

    @Override
    public String toString() {
        return name;
    }
}