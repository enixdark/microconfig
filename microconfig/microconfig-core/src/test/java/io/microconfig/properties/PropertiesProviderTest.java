package io.microconfig.properties;

import io.microconfig.properties.resolver.PropertyResolveException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.environments.Component.byNameAndType;
import static io.microconfig.environments.Component.byType;
import static io.microconfig.utils.MicronconfigTestFactory.getPropertyProvider;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class PropertiesProviderTest {
    private final PropertiesProvider provider = getPropertyProvider();

    @Test
    void testLoadsProperties() {
        Map<String, Property> props = provider.getProperties(byType("th-client"), "uat");
        assertEquals(25, props.size());
        assertEquals("th-common-value", props.get("th-client.property.common").getValue());
        assertEquals("th-common-value", props.get("th-client.defaultValue").getValue());
        assertEquals("th-uat-value", props.get("th-client.property.from.uat").getValue());
        assertEquals("uat-override", props.get("th-client.property.override").getValue());
        assertEquals("uat-overridev2", props.get("th-client.several-placeholders").getValue());
        assertEquals("uat-overrideuat-overrideuat-override", props.get("th-client.several-placeholders2").getValue());
        assertEquals("110", props.get("th-client.some.int.property1").getValue());
        assertEquals("200", props.get("th-client.spel").getValue());
        assertEquals("172.30.162.3", props.get("th-client.th-server-ip").getValue());
        assertEquals("100.10.20.1", props.get("th-client.th-server-demo-ip").getValue());
    }

    @Test
    void testVar() {
        Map<String, Property> props = provider.getProperties(byType("var"), "var");
        assertEquals(1, props.values().stream()
                .filter(p -> !p.getSource().isSystem())
                .filter(p -> !p.isTemp())
                .count());

        assertEquals("3", props.get("c").getValue());
    }

    @Test
    void testIpParam() {
        Map<String, Property> properties = provider.getProperties(byType("ip1"), "uat");
        assertEquals("1.1.1.1", properties.get("ip1.some-ip").getValue());
    }

    @Test
    void testOrderParam() {
        doTestOrder("ip1", 1);
        doTestOrder("ip2", 2);
        doTestOrder("th-client", 4);
    }

    private void doTestOrder(String compName, int order) {
        Map<String, Property> properties = provider.getProperties(byType(compName), "uat");
        assertEquals(String.valueOf(order), properties.get("order").getValue());
    }

    @Test
    void testComponentReceivesThisIpPropertyFromEnv() {
        Map<String, Property> properties = provider.getProperties(byType("th-cache-node3"), "uat");
        assertEquals("172.30.162.3", properties.get("ip").getValue());
    }

    @Test
    void testComponentOverridesThisIpPropertyFromEnv() {
        Map<String, Property> properties = provider.getProperties(byType("ip3"), "uat");
        assertEquals("1.1.1.1", properties.get("ip").getValue());
    }

    @Test
    void testCyclicDetect() {
        assertThrows(PropertyResolveException.class, () -> provider.getProperties(byType("cyclicDetectTest"), "uat"));

    }

    @Test
    void testSimpleInclude() {
        Map<String, Property> properties = new TreeMap<>(provider.getProperties(byType("i1"), "uat"));
        assertEquals(asList("configDir", "env", "folder", "i1.prop", "i2.prop", "i3.prop", "name", "portOffset", "userHome"), new ArrayList<>(properties.keySet()));
    }

    @Test
    void testIncludeWithEnvChange() {
        Map<String, Property> props = provider.getProperties(byType("ic1"), "dev");
        assertEquals(14, props.size());
        assertEquals("dev", props.get("env").getValue());
        assertEquals("ic1-dev", props.get("ic1.prop").getValue());
        assertEquals("ic2-dev", props.get("ic2.prop").getValue());
        assertEquals("ic3-dev2", props.get("ic3.prop").getValue());
        assertEquals("ic4-dev2", props.get("ic3.placeholder").getValue());
        assertEquals("ic4-dev2", props.get("ic4.prop").getValue());
        assertEquals("4.4.4.4", props.get("ic3.ic4-ip").getValue());
        assertEquals("ic2", props.get("ic3.placeholderToSelf").getValue());
    }

    @Test
    void testPlaceholderToIncludeWithEnvChange() {
        Map<String, Property> props = provider.getProperties(byType("ic5"), "dev");
        assertEquals("ic2", props.get("v").getValue());
    }

    @Test
    void testIncludeWithoutKeyword() {
        Map<String, Property> props = provider.getProperties(byType("without1"), "dev");
        assertEquals(9, props.size());
        assertEquals("p1", props.get("p1").getValue());
        assertEquals("p2", props.get("p2").getValue());
        assertEquals("w2", props.get("w2.include").getValue());
        assertFalse(props.containsKey("w2.exclude"));
    }

    @Test
    void testPortOffset() {
        assertEquals("1001", provider.getProperties(byType("portOffsetTest"), "dev").get("port").getValue());
        assertEquals("1002", provider.getProperties(byType("portOffsetTest"), "dev2").get("port").getValue());
    }

    @Test
    void testPlaceholderOverride() {
        assertEquals("scomp1 20", provider.getProperties(byType("scomp2"), "dev").get("compositeValue").getValue());
        assertEquals("scomp1 2", provider.getProperties(byType("scomp1"), "dev").get("compositeValue").getValue());

        assertEquals("3", provider.getProperties(byType("scomp1"), "dev").get("compositeValue2").getValue());
        assertEquals("21", provider.getProperties(byType("scomp2"), "dev").get("compositeValue2").getValue());
    }

    @Test
    void testThisOverride() {
        assertEquals("2.2.2.2", provider.getProperties(byType("tov1"), "uat").get("value").getValue());
        assertEquals("3.3.3.3", provider.getProperties(byType("tov2"), "uat").get("value").getValue());
    }

    @Test
    void testEnvProp() {
        assertEquals("uat", provider.getProperties(byType("envPropTest"), "uat").get("env.env").getValue());
        assertEquals("dev value", provider.getProperties(byType("envPropTest"), "dev").get("env.value").getValue());
    }

    @Test
    void nestedExpTest() {
        assertEquals("tcp://:5822", provider.getProperties(byType("pts"), "dev").get("test.mq.address").getValue());
        assertEquals("tcp://:5822", provider.getProperties(byType("pts"), "dev").get("test.mq.address2").getValue());
    }

    @Test
    void spelWrap() {
        assertEquals("hello world3", provider.getProperties(byType("spelWrap"), "dev").get("v1").getValue());
    }

    @Test
    void testEnvPropAliases() {
        doTestAliases("node1", "172.30.162.4");
        doTestAliases("node2", "172.30.162.4");
        doTestAliases("node3", "172.30.162.5");
        doTestAliases("node", "172.30.162.5");
    }

    @Test
    void testReferenceEnvWithSimilarName() {
        assertEquals("value-from-dev", provider.getProperties(byType("main"), "dev").get("value").getValue());
    }

    @Test
    void testPlaceholderToAliases() {
        Map<String, Property> properties = provider.getProperties(byType("placeholderToAlias"), "aliases");
        assertEquals("172.30.162.4 172.30.162.5", properties.get("ips").getValue());
        assertEquals("v1 v1", properties.get("properties").getValue());
    }

    private void doTestAliases(String componentName, String ip) {
        Map<String, Property> properties = provider.getProperties(byNameAndType(componentName, "node"), "aliases");
        assertEquals(componentName, properties.get("name").getValue());
        assertEquals(ip, properties.get("ip").getValue());
    }

}