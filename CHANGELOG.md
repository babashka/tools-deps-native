# Changelog

## v0.1.7

- Fix [#30](https://github.com/babashka/tools-deps-native/issues/30): pod won't run on newer versions of macOS

## v0.1.6

- Revert tools.deps to `v0.18.1354` which tools.build uses

## v0.1.5

- Fix NPE when `deps.edn` doesn't exist

## v0.1.4

- Fix linux amd64 (got mixed up with aarch64)

## v0.1.3

- Include linux aarch64 binary ([@TimoKramer](https://github.com/TimoKramer))
- Bump deps

## v0.1.2

- Upgrade libraries, including tools.deps
- Provide binary for macos aarch64

## v0.1.1

- Fix reflection issues with certain dependencies (see https://github.com/babashka/tools.bbuild/issues/10)

## v0.1.0

- Bump to latest tools-deps (0.16.1264)
