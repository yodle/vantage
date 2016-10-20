package com.yodle.vantage.component.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.yodle.vantage.component.domain.VersionId;

@Component
public class VersionPurifier {
    public static final String UNKNOWN_VERSION = "unknown";
    public static final String LATEST_VERSION = "latest";
    public static final String UNDEFINED_VERSION = "undefined";
    public static final Set<String> RESERVED_VERSIONS = Sets.newHashSet(UNDEFINED_VERSION, UNKNOWN_VERSION, LATEST_VERSION);
    private static final Pattern REAL_VERSION_PATTERN = Pattern.compile("^[\\-.0-9a-zA-Z_]+$");

    public VersionId requireRealVersion(VersionId version) {

        if (RESERVED_VERSIONS.contains(version.getVersion())) {
            throw new RuntimeException(
                    String.format("[%s] is a reserved version ([%s]) for component %s", version.getVersion(), RESERVED_VERSIONS, version.getComponent())
            );
        }

        if (isRealVersion(version.getVersion())) {
            return version;
        }

        throw new RuntimeException(
                String.format("You cannot create a dynamic version [%s] for component [%s]", version.getVersion(), version.getComponent())
        );
    }

    public VersionId purifyVersion(VersionId version) {
        if (isLatestVersion(version.getVersion()) || isRealVersion(version.getVersion())) {
            return version;
        }

        return new VersionId(version.getComponent(), UNKNOWN_VERSION);
    }

    public boolean isRealVersion(String version) {
        return version != null && !RESERVED_VERSIONS.contains(version) && REAL_VERSION_PATTERN.matcher(version).matches();
    }

    public boolean isLatestVersion(String version) {
        return version != null && version.toLowerCase().equals(LATEST_VERSION);
    }
}
