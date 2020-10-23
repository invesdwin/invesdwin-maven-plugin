# invesdwin-maven-plugin

This plugin provides auto configuration for eclipse projects using the invesdwin platform standards. It automatically sets up [checkstyle](https://marketplace.eclipse.org/content/checkstyle-plug), [spotbugs](http://marketplace.eclipse.org/content/spotbugs-eclipse-plugin), [spring tool suite](https://marketplace.eclipse.org/content/spring-tool-suite-sts-eclipse), [moreunit](https://marketplace.eclipse.org/content/moreunit), save actions, code formatter and other things.

When using the included checkstyle config, please make sure you have the [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin) installed in eclipse or else you will get exceptions thrown in during checks.

## Maven

Releases and snapshots are deployed to this maven repository:
```
https://invesdwin.de/artifactory/invesdwin-oss-remote
```

Dependency declaration:
```xml
<plugin>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-maven-plugin</artifactId>
	<version>1.0.9</version><!---project.version.invesdwin-maven-plugin-parent-->
	<executions>
		<execution>
			<goals>
				<goal>initialize</goal>
				<goal>generate-sources</goal>
				<goal>compile</goal>
			</goals>
			<configuration>
				<useInvesdwinEclipseSettings>true</useInvesdwinEclipseSettings>
				<codeComplianceLevel>1.8</codeComplianceLevel>
			</configuration>
		</execution>
	</executions>
</plugin>
```
## Goals

The plugins has the following goals which you can include/exclude:

* `initialize`: this extracts the eclipse settings and sets up the git/svn ignores properly for the module
  - the configuration parameter `useInvesdwinEclipseSettings` can be used to disable extraction of the eclipse settings
  - the configuration parameter `codeComplianceLevel` can be used to set the java code compliance level for the eclipse project
* `generate-sources`: this generates a helper class with which the JAXB context finds out about XSD files used by the invesdwin platform
* `compile`: this sets up checkstyle and findbugs builder and nature for this eclipse project

## Handling Multiple Projects/Repositories
* Check out individual git repos and create a parent pom to build them all in one go:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0                              http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-build-all</artifactId>
	<version>0.0.0-SNAPSHOT</version>
	<packaging>pom</packaging>

	<modules>
		<!-- list all projects here -->
		<module>invesdwin-oss</module>
        	<module>invesdwin-trading/invesdwin-trading-parent</module>
        	<module>invesdwin-trading-test/invesdwin-trading-backtest-test</module>
        	<module>invesdwin-trading-jforex/invesdwin-trading-jforex-parent</module>
        	<module>invesdwin-trading-fxcm/invesdwin-trading-fxcm-parent</module>
        	<module>invesdwin-trading-metatrader/invesdwin-trading-metatrader-parent</module>
        	<module>orbox/orbox-parent</module>
        	<module>irisalpha/irisalpha-parent</module>
  	</modules>

</project>
```
* Install [myrepos](https://myrepos.branchable.com/): `apt install myrepos`
	* run `mr register <PROJECT_FOLDER>` for each repository
	* with that you can use `mr update` from any parent directory to pull all projects (nested symlinks are not supported)
	* edit `.mrconfig` as below
```sh
# use special update scripts for invesdwin-oss that properly handle git submodules
[/ABSOLUTE/PATH/TO/invesdwin/invesdwin-oss]
update = bash pull.sh
push = bash push.sh
commit = bash commit.sh
checkout = git clone 'https://github.com/subes/invesdwin-oss.git' 'invesdwin-oss'
# private repos might require credentials in the URL to allow checkout (otherwise 404 might be returned)
[/ABSOLUTE/PATH/TO/invesdwin-trading]
checkout = git clone 'https://<USERNAME>:<PASSWORD>@github.com/subes/invesdwin-trading.git' 'invesdwin-trading'
```

* Alternatively on Windows
	* install [Cygwin](https://www.cygwin.com/) and [add it to the PATH](https://www.howtogeek.com/howto/41382/how-to-use-linux-commands-in-windows-with-cygwin/) or run scripts directly from `<CYGWIN_HOME>/bin/bash.exe`
	* use a pull script like this:
```sh
#! /bin/bash

for dir in *
do
  test "dependencies" = "$dir" && continue
  test -d "$dir" || continue
  echo -- $dir
  cd $dir

  git pull

  cd ..
done

cd invesdwin-oss
./pull.sh
```

## Eclipse Tips
* In Package Explorer configure (three dots):
	* Top Level Elemements -> Working Sets
	* Package Presentation -> Hierarchical
	* or use Project Explorer instead
* Prefer Java Perspective over JEE Perspective (top right buttons)
* In Problems View configure (three dots):
	* Show -> Show All (default in Java Perspective)
* Install Eclipse Plugins that are mentioned at the top
	* also install [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin)
* Import each project into a separate Working Set
	* with that it becomes easy to commit individual projects using Git Staging View by selecting the Working Sets
	* except invesdwin-oss requires each project to be selected individually since Git Submodules need to be committed separately
	* or use `mr commit` or direct git commands per project/repository on the console
* You can configure IntelliJ Keymap if desired using [this plugin](https://github.com/IntelliJIdeaKeymap4Eclipse/IntelliJIdeaKeymap4Eclipse)
	* with Eclipse Keymap on macOS `CTRL+SPACE` might require remapping so that code completion becomes usable (macOS might occupy that shortcut): Window -> Preferences -> Keys -> Content Assist

## IntelliJ Tips

* Import all projects as Maven using a parent-pom that lists projects as modules (see above for an example pom.xml)
* Install Eclipse-Code-Formatter and [follow instructions](https://github.com/krasa/EclipseCodeFormatter#instructions):
	* use "Resolve project specific config" (should be equivalent to "Eclipse [built-in]")
	* use "Optimize Imports from File" as [<SOME_PROJECT>/.settings/org.eclipse.jdt.ui.prefs](https://github.com/subes/invesdwin-maven-plugin/blob/master/invesdwin-maven-plugin-parent/invesdwin-maven-plugin/src/main/java/invesdwin-eclipse-settings/.settings/org.eclipse.jdt.ui.prefs)
* Install [Save Actions plugin](https://plugins.jetbrains.com/plugin/7642-save-actions) and configure:
	* Use external Eclipse configuration file (.epf): [eclipse_settings.epf](https://github.com/subes/invesdwin-maven-plugin/blob/master/eclipse_settings.epf)
	* this imports the configuration
* You can configure Eclipse Keymap if desired via: File -> Settings -> Keymap -> Eclipse

## Support

If you need further assistance or have some ideas for improvements and don't want to create an issue here on github, feel free to start a discussion in our [invesdwin-platform](https://groups.google.com/forum/#!forum/invesdwin-platform) mailing list.
