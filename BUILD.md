# Building Secure Agents Lib
**Repository:** `secure-agents-lib`  
**Description:** `Used for building the project`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->
Assuming you have the [requirements](#requirements) installed you can build via Maven i.e.

```
$ mvn clean install
```

## Requirements

This directory and its subdirectories are a multimodule Maven project, therefore you will require the following software
to develop and build this code:

- JDK 21 (or higher)
- [Apache Maven](https://maven.apache.org) (3.8.1 or higher)
- Docker for container based tests
    - Can be opted out of by disabling the `docker` profile e.g. by adding `-P-docker` to the Maven arguments

**NB** These minimum requirements are enforced as part of the Maven build using the Maven Enforcer plugin.

## Development

It is recommended to use an IDE of your choice that supports Maven projects, most such IDEs have the option to import an
existing Maven project which you should use with this directory.

### Test Coverage

The build is set up with the [JaCoCo](https://www.eclemma.org/jacoco/trunk/index.html) Maven plugin to calculate test
coverage as part of all builds. It is also set up to enforce minimum levels of code coverage. You can add the following
`<plugin>` definition to the `pom.xml` of any module where you want to enable this enforcement:

```xml

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${plugin.jacoco}</version>
</plugin>
```

There's no need for specific configuration since that is handled by the `<pluginManagement>` section of the top level
`pom.xml` in this directory. By default, the plugin is configured to require 80% code coverage.

If you want to increase/decrease the desired minimum coverage level you can add a `coverage.minimum` property to the
`<properties>` section of your module and set the minimum coverage level e.g. `0.9` would increase required coverage to
90%.

### SNAPSHOT Builds

Maven Snapshots are only published from `main`, or when a developer manually runs `mvn deploy` from their machine.

### Release Builds

Release builds should be carried out using the normal Maven release process from a developers local machine.

Firstly make sure `main` is up-to-date and then create a new branch from there for the release:

```bash
$ git checkout main
$ git pull
$ git checkout -B release/<version>
$ git push -u origin release/<version>
```

Then you can prepare a Maven release, following the prompts to supply the desired git tag and next development version:

```bash
$ mvn release:clean release:prepare -DreleaseVersion=<version>
```

Make sure you have pushed your release preparation to the remote repository:

```bash
$ git push
$ git push --tags
```

The tag created by the `mvn release:prepare` will trigger an automatic GitHub Actions build that includes releasing the
libraries to Maven Central.  Please check the Actions tab for a build with the tag you just created and ensure that the
build passes.

At this point edit the `README.md` to update the version (listed earlier in [Depending on these
Libraries](README.md#depending-on-these-libraries)), commit and push that change:

```bash
$ vim README.md
$ git add README.md
$ git commit -m "Note latest version is now <version>"
$ git push
```

Finally, please go to GitHub and open a PR to merge the release branch you created back into `main`.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.