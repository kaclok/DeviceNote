package com.smlj.singledevice_note.core.o;

import lombok.Data;

@Data
public class Version implements Comparable<Version> {
    private final int major;
    private final int minor;
    private final int patch;
    private final int build;

    public Version(String version) {
        String[] parts = version.split("\\.");
        this.major = Integer.parseInt(parts[0]);
        this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        this.patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        this.build = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
    }

    @Override
    public int compareTo(Version other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        if (patch != other.patch) return Integer.compare(patch, other.patch);
        return Integer.compare(build, other.build);
    }
}
