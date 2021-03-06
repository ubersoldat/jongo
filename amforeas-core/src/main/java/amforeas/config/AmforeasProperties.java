/**
 * Copyright (C) Alejandro Ayuso
 *
 * This file is part of Amforeas. Amforeas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Amforeas is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Amforeas. If not, see <http://www.gnu.org/licenses/>.
 */

package amforeas.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import amforeas.acl.ACLRule;

public class AmforeasProperties {

    private static final Logger l = LoggerFactory.getLogger(AmforeasProperties.class);

    /**
     * Map of properties for this instance
     */
    private final Map<String, AmforeasProperty> properties = new ConcurrentHashMap<>();

    /**
     * Wrapper of {@link System} to override properties during {@link AmforeasProperties.load}
     */
    private SystemWrapper system = new SystemWrapper();

    /* Server */
    public static final String SERVER_ROOT = "server.root";
    public static final String SERVER_HOST = "server.host";
    public static final String SERVER_PORT = "server.http.port";
    public static final String SERVER_THREADS_MIN = "server.threads.min";
    public static final String SERVER_THREADS_MAX = "server.threads.max";
    public static final String SERVER_PAGE_SIZE = "server.page.size";
    public static final String SERVER_PAGE_SIZE_MAX = "server.page.size.max";

    /* SSL */
    public static final String SERVER_SECURE_PORT = "server.https.port";
    public static final String SERVER_SECURE_FILE = "server.https.jks";
    public static final String SERVER_SECURE_FILE_PASSWORD = "server.https.jks.password";

    /* Database */
    public static final String ALIAS_LIST = "alias.list";
    public static final String DB_DRIVER = "%s.jdbc.driver";
    public static final String DB_USERNAME = "%s.jdbc.username";
    public static final String DB_PASSWORD = "%s.jdbc.password";
    public static final String DB_DATABASE = "%s.jdbc.database";
    public static final String DB_HOST = "%s.jdbc.host";
    public static final String DB_PORT = "%s.jdbc.port";
    public static final String DB_READONLY = "%s.jdbc.readonly";
    public static final String DB_MAX_CONNECTIONS = "%s.jdbc.max.connections";
    public static final String DB_URL = "%s.jdbc.url";

    /* ACLs */
    public static final String DB_ACL_ALLOW_RULE = "%s.acl.allow";
    public static final String DB_ALIAS_ALLOW_RULE = "%s.acl.rules.%s.allow";

    /**
     * Instantiates a new instance of {@link AmforeasProperties} and loads the properties
     * from the provided {@link Properties}
     * @param javaProperties {@link Properties} - the {@link Properties} from where to load
     * @return an instance of {@link AmforeasProperties}
     */
    public static AmforeasProperties of (final Properties javaProperties) {
        AmforeasProperties instance = new AmforeasProperties();
        instance.load(javaProperties);
        return instance;
    }

    public AmforeasProperties() {
        /* Required */
        this.addProperty(SERVER_ROOT, true);
        this.addProperty(SERVER_HOST, true);
        this.addProperty(SERVER_PORT, true);
        this.addProperty(SERVER_THREADS_MIN, "5", true);
        this.addProperty(SERVER_THREADS_MAX, "10", true);
        this.addProperty(ALIAS_LIST, true);
        this.addProperty(SERVER_PAGE_SIZE, "25", true);
        this.addProperty(SERVER_PAGE_SIZE_MAX, "500", true);

        /* Optionals */
        this.addProperty(SERVER_SECURE_PORT, false);
        this.addProperty(SERVER_SECURE_FILE, false);
        this.addProperty(SERVER_SECURE_FILE_PASSWORD, false);
    }

    /**
     * Adds the given {@link AmforeasProperty} to this instance properties
     * @param property - {@link AmforeasProperty} to add
     */
    public void addProperty (AmforeasProperty property) {
        this.properties.put(property.getKey(), property);
    }

    /**
     * Create a new {@link AmforeasProperty} from the given parameters and adds it to this instance properties
     * @param key - the key of the property
     * @param required - if the property is required to have a value (not null, or empty)
     */
    private void addProperty (String key, Boolean required) {
        final AmforeasProperty p = new AmforeasProperty(key, required);
        this.addProperty(p);
    }

    /**
     * Create a new {@link AmforeasProperty} from the given parameters and adds it to this instance properties
     * @param key - the key of the property
     * @param key - the value of the property
     * @param required - if the property is required to have a value (not null, or empty)
     */
    private void addProperty (String key, String value, Boolean required) {
        final AmforeasProperty p = new AmforeasProperty(key, required, value);
        this.addProperty(p);
    }

    /**
     * Create a new {@link AmforeasProperty} from the given parameters and adds it to this instance properties.
     * If the value is set on {@link System} it's overwritten.
     * @param key - the key of the property
     * @param alias - the database alias this property belongs to
     * @param javaProperties - {@link Properties} with the alias values
     */
    private void addAliasProperty (String key, String alias, Properties javaProperties) {
        final String formatted = String.format(key, alias);
        final String withPrefix = AmforeasProperty.PREFIX + formatted;
        final String fromFile = javaProperties.getProperty(withPrefix);
        l.debug("Adding property {} to alias {} with value: {}", formatted, alias, fromFile);
        this.addProperty(formatted, system.get(withPrefix).orElse(fromFile), false);
    }

    private void addResourceRule (String key, String alias, String resource, Properties javaProperties) {
        final String formatted = String.format(key, alias, resource);
        final String withPrefix = AmforeasProperty.PREFIX + formatted;
        final String fromFile = javaProperties.getProperty(withPrefix);
        final String finalValue = system.get(withPrefix).orElse(fromFile);

        l.debug("Adding ACL rule {} to alias {} resource {} with value: {}", formatted, alias, resource, finalValue);
        this.addProperty(formatted, finalValue, false);
    }

    /**
     * Obtains the property value for the given key
     * @param key - the key of the property
     * @return the value for the given key
     * @throws IllegalArgumentException - if the key is not valid or we don't have a value for it
     */
    public String get (String key) {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("A configuration key is required");
        }

        key = AmforeasProperty.PREFIX + key;

        l.debug("Getting configuration for key {}", key);
        Optional<AmforeasProperty> p = this.getProperty(key);

        if (p.isEmpty()) {
            throw new IllegalArgumentException("The provided configuration key " + key + " is not valid");
        }

        return p.get().getValue();
    }

    /**
     * Obtains the property value for the given key and alias
     * @param key - the key of the property
     * @param alias - the database alias this property belongs to
     * @return the value for the given key
     * @throws IllegalArgumentException - if the key is not valid or we don't have a value for it
     */
    public String get (String key, String alias) {
        if (!key.contains("%s")) {
            throw new IllegalArgumentException("The provided configuration key " + key + " is not formattable");
        }

        if (StringUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("The provided configuration key " + key + " requires an alias");
        }

        return this.get(String.format(key, alias));
    }

    public String rule (String key, String alias, String resource) {
        if (!key.contains("%s")) {
            throw new IllegalArgumentException("The provided rule key " + key + " is not formattable");
        }

        if (StringUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("The provided rule key " + key + " requires an alias");
        }

        if (StringUtils.isEmpty(resource)) {
            throw new IllegalArgumentException("The provided rule key " + key + " requires a resource");
        }

        return this.get(String.format(key, alias, resource));
    }

    private Optional<AmforeasProperty> getProperty (String key) {
        return Optional.ofNullable(this.properties.get(key));
    }

    /**
     * Loads the values from the provided {@link Properties} to to this instance properties.
     * @param javaProperties {@link Properties} - the {@link Properties} from where to load data from
     */
    public void load (final Properties javaProperties) {
        // load the fixed ones
        javaProperties.entrySet().forEach(entry -> {
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            this.getProperty(key).ifPresent(prop -> prop.setValue(val));
        });

        // load from system
        this.properties.entrySet().forEach(entry -> this.system.get(entry.getKey()).ifPresent(val -> entry.getValue().setValue(val)));

        // load db aliases
        this.getAliases().forEach(alias -> this.loadAliases(alias, javaProperties));
    }

    private void loadAliases (final String alias, final Properties javaProperties) {
        this.addAliasProperty(DB_DRIVER, alias, javaProperties);
        this.addAliasProperty(DB_USERNAME, alias, javaProperties);
        this.addAliasProperty(DB_PASSWORD, alias, javaProperties);
        this.addAliasProperty(DB_DATABASE, alias, javaProperties);
        this.addAliasProperty(DB_HOST, alias, javaProperties);
        this.addAliasProperty(DB_PORT, alias, javaProperties);
        this.addAliasProperty(DB_READONLY, alias, javaProperties);
        this.addAliasProperty(DB_MAX_CONNECTIONS, alias, javaProperties);
        this.addAliasProperty(DB_URL, alias, javaProperties);
        this.addAliasProperty(DB_ACL_ALLOW_RULE, alias, javaProperties);
        this.loadRules(alias, javaProperties);
    }

    private void loadRules (final String alias, final Properties javaProperties) {
        final String prefix = AmforeasProperty.PREFIX + alias + ".acl.rules";

        for (Entry<Object, Object> entry : javaProperties.entrySet()) {
            final String key = (String) entry.getKey();

            if (!key.startsWith(prefix)) {
                continue;
            }

            final String[] split = key.split("\\.");
            try {
                String resource = split[4];
                this.addResourceRule(DB_ALIAS_ALLOW_RULE, alias, resource, javaProperties);
            } catch (ArrayIndexOutOfBoundsException e) {
                l.warn("Invalid ACL rule {}", key);
            }
        }
    }

    public List<String> getAliases () {
        String aliases = this.get(ALIAS_LIST);

        if (StringUtils.isEmpty(aliases)) {
            return new ArrayList<>(0);
        }

        return Arrays.asList(aliases.split(",")).stream().map(alias -> alias.trim()).collect(Collectors.toList());
    }

    public List<ACLRule> getAliasRules (final String alias) {
        final String aliasPrefix = AmforeasProperty.PREFIX + alias + ".acl.allow";
        final String resourcePrefix = AmforeasProperty.PREFIX + alias + ".acl.rules";

        final List<ACLRule> rules = new ArrayList<>();
        for (Entry<String, AmforeasProperty> entry : this.properties.entrySet()) {
            final String key = entry.getKey();

            if (key.startsWith(aliasPrefix)) {
                rules.add(ACLRule.of(alias, entry.getValue().getValue()));
                continue;
            }

            if (key.startsWith(resourcePrefix)) {
                final String[] split = key.split("\\.");
                final String resource = split[4];
                rules.add(ACLRule.of(alias, resource, entry.getValue().getValue()));
            }
        }

        return rules;
    }

    /**
     * Checks if the data we currently have is valid.
     * If any {@link AmforeasProperty} marked as required has no value, it fails
     * If any database alias doesn't provide a jdbc.driver, it fails
     * @return true if the configuration is valid.
     */
    public Boolean isValid () {
        for (Entry<String, AmforeasProperty> entry : this.properties.entrySet()) {
            if (entry.getValue().isRequired() && StringUtils.isEmpty(entry.getValue().getValue())) {
                l.warn("Invalid configuration. Property {} is marked as required but has invalid value {}", entry.getKey(), entry.getValue().getValue());
                return false;
            }
        }

        for (String alias : this.getAliases()) {
            // Check that at least we have the driver & database name. In the future we might do more complex checks
            if (StringUtils.isEmpty(this.get(DB_DRIVER, alias))) {
                l.warn("Invalid configuration. Database property {} is required for alias {}", DB_DRIVER, alias);
                return false;
            }

            if (StringUtils.isEmpty(this.get(DB_DATABASE, alias))) {
                l.warn("Invalid configuration. Database property {} is required for alias {}", DB_DATABASE, alias);
                return false;
            }
        }

        return true;
    }

    public void setSystem (SystemWrapper system) {
        this.system = system;
    }

    public SystemWrapper getSystem () {
        return this.system;
    }
}
