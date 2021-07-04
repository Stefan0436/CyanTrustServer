#!/bin/bash

#
# CyanTrustServer AutoRelease Build Script, installs the module update on the remote server.
# Only works on AutoRelease enabled repository services.
#
# Also, it needs the autorelease.allow.install file on the server.
#

function prepare() {
	destination "/etc/connective-http/modules"
	buildOutput "build/libs"
}

function build() {
    chmod +x gradlew
    ./gradlew jar
}

function install() {
    rm -f "$DEST/CyanTrustServer-"*.jar
    cp -rfv "$BUILDDIR/." "$DEST"
    systemctl restart connective-http
}
