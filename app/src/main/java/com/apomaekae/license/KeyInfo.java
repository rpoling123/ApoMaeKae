package com.apomaekae.license;

public class KeyInfo {

    public final boolean valid;
    public final boolean versionValid;
    public final String key;
    public final String expires;
    public final String message;

    public KeyInfo(
            boolean valid,
            boolean versionValid,
            String key,
            String expires,
            String message
    ) {
        this.valid = valid;
        this.versionValid = versionValid;
        this.key = key;
        this.expires = expires;
        this.message = message;
    }
}
