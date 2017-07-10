# invesdwin-maven-plugin

This plugin provides auto configuration for eclipse projects using the invesdwin platform standards. It automatically sets up [checkstyle](https://marketplace.eclipse.org/content/checkstyle-plug), [findbugs](https://marketplace.eclipse.org/content/findbugs-eclipse-plugin), [spring tool suite](https://marketplace.eclipse.org/content/spring-tool-suite-sts-eclipse), [moreunit](https://marketplace.eclipse.org/content/moreunit), save actions, code formatter and other things.

When using the included checkstyle config, please make sure you have the [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin) installed in eclipse or else you will get exceptions thrown in during checks.

## Maven

Releases and snapshots are deployed to this maven repository:
```
http://invesdwin.de/artifactory/invesdwin-oss-remote
```

Dependency declaration:
```xml
<plugin>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-maven-plugin</artifactId>
	<version>1.0.0-SNAPSHOT</version>
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

## Support

If you need further assistance or have some ideas for improvements and don't want to create an issue here on github, feel free to ask a question in our [invesdwin-platform googlegroup](https://groups.google.com/forum/#!forum/invesdwin-platform).
