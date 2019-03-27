package io.microconfig.commands.buildconfig.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.microconfig.commands.buildconfig.factory.ConfigType.byNameAndTypes;
import static io.microconfig.commands.buildconfig.factory.ConfigType.extensionAsName;

@Getter
@RequiredArgsConstructor
public enum StandardConfigType {
    APPLICATION(byNameAndTypes("application", ".properties", ".yaml")),
    PROCESS(byNameAndTypes("process", ".process", ".proc")),
    DEPLOY(byNameAndTypes("deploy", ".deploy")),
    ENV(extensionAsName("env")),
    SECRET(extensionAsName("secret")),
    LOG4j(extensionAsName("log4j")),
    LOG4J2(extensionAsName("log4j2"));

    private final ConfigType configType;

    public ConfigType type() {
        return configType;
    }
}