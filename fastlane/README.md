fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android bump_version

```sh
[bundle exec] fastlane android bump_version
```

Bump version and commit changes

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build release bundle

### android deploy_production

```sh
[bundle exec] fastlane android deploy_production
```

Deploy to Google Play Store

### android deploy_internal

```sh
[bundle exec] fastlane android deploy_internal
```

Deploy to internal track

### android deploy_beta

```sh
[bundle exec] fastlane android deploy_beta
```

Deploy to beta track

### android deploy_android

```sh
[bundle exec] fastlane android deploy_android
```

Full deployment pipeline

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
