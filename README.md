# invesdwin-maven-plugin

This plugin provides auto configuration for eclipse projects using the invesdwin platform standards. It automatically sets up [Checkstyle](https://marketplace.eclipse.org/content/checkstyle-plug), [SpotBugs](http://marketplace.eclipse.org/content/spotbugs-eclipse-plugin), ~~[Spring Tools](https://marketplace.eclipse.org/content/spring-tools-4-aka-spring-tool-suite-4)~~, [MoreUnit](https://marketplace.eclipse.org/content/moreunit), [m2e-apt](https://marketplace.eclipse.org/content/m2e-apt), save actions, code formatter and other things.

When using the included checkstyle config, please make sure you have the [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin) installed in eclipse or else you will get exceptions thrown during checks.

## Maven

Releases and snapshots are deployed to this maven repository:
```
https://invesdwin.de/repo/invesdwin-oss-remote/
```

Dependency declaration:
```xml
<plugin>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-maven-plugin</artifactId>
	<version>1.0.13</version><!---project.version.invesdwin-maven-plugin-parent-->
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

* Git setup:
```
# On windows open terminal as administrator
git config --global core.longpaths true
git config --global core.autocrlf false
# On linux if thre is no credential manager available
git config --global  credential.helper store
```
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
* To build private invesdwin projects you have to use this protected [<USER_HOME>/.m2/settings.xml](https://github.com/subes/invesdwin-continuous-integration/blob/master/settings.xml)
	* For connecting to private invesdwin services (e.g. financial data) you have to download this protected [<USER_HOME>/.invesdwin/system.properties](https://github.com/subes/invesdwin-continuous-integration/blob/master/system.properties)
	* There is also a protected [ansible project](https://github.com/subes/invesdwin-continuous-integration/tree/master/invesdwin-setup/src/ansible) that automates some of these steps
	* Otherwise consider setting network timeouts (at least 30000 ms) in your custom `settings.xml` to prevent hanging downloads: https://stackoverflow.com/a/27015320/67492 

## Eclipse Tips
* Download Eclipse JEE package and install similar to: [installEclipse.sh](https://github.com/subes/invesdwin-maven-plugin/blob/master/installEclipse.sh)
	* on Ubuntu you can create a shortcut via [Menulibre](https://github.com/bluesabre/menulibre): `apt install menulibre`
	* using the created `/usr/local/bin/eclipse` launcher script and `<ECLIPSE_HOME>/eclipse48.png`
* Install Plugins
	* [Checkstyle](https://marketplace.eclipse.org/content/checkstyle-plug)
		* also install [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin) via dropins
	* [SpotBugs](http://marketplace.eclipse.org/content/spotbugs-eclipse-plugin)
	* [MoreUnit](https://marketplace.eclipse.org/content/moreunit)
	* [ECD++ - Fork of Enhanced Class Decompiler](https://marketplace.eclipse.org/content/ecd-fork-enhanced-class-decompiler)
 		* Desktop alternative: [jd-gui-duo](https://github.com/nbauma109/jd-gui-duo)
	* [WindowBuilder](https://marketplace.eclipse.org/content/windowbuilder) (use stable/release, not nightly)
	* [Web Developer Tools](https://marketplace.eclipse.org/content/eclipse-web-developer-tools-0) (optional, if you used Eclipse Java, already contained in Eclipse JEE)
 		* Alternative: [Wild Web Developer](https://marketplace.eclipse.org/content/wild-web-developer-html-css-javascript-typescript-nodejs-angular-json-yaml-kubernetes-xml)
	* ~~[Spring Tools 4](https://marketplace.eclipse.org/content/spring-tools-4-aka-spring-tool-suite-4)~~ (optional, slows down eclipse with the spring language server)
	* ~~[TeXlipse](https://marketplace.eclipse.org/content/eclipse-texlipse)~~ (optional, for writing e.g. thesis documents)
* Install m2eclipse connectors:
	* Window -> Preferences -> Maven -> Discovery -> Open Catalog -> Select "buildhelper" -> Finish
		* If this does not work, install manually using the update site: https://github.com/tesla/m2eclipse-buildhelper/releases/download/latest/
		* This plugin will ensure that generated sources are added to the project build path.
		* Manual Workaround: Right Click on the Project -> Propertries -> Java Build Path -> Source -> Add Folder… -> Select `target/generated-sources/apt`
  	* Window -> Preferences -> Maven -> Java Configurator -> JRE System Library Version -> Use Workspace Default
  		* This ensures that maven-surefire-plugin settings are not applied to eclipse JUnit launchers and that these launchers don't mess up the default JDK version when creating new launchers.
* Prefer Java Perspective over JEE Perspective (top right buttons)
* In Package Explorer configure (three dots)
	* **Top Level Elemements -> Working Sets**
	* **Package Presentation -> Hierarchical**
	* **Filters and Customization... -> .\* resources -> Uncheck -> OK**
	* or use Project Explorer instead
* In Problems View configure (three dots)
	* Show -> Show All (default in Java Perspective)
* Change some general settings
	* **Window -> Preferences -> General -> Workspace -> Refresh using native hooks or polling -> Check**
	* **Window -> Preferences -> General -> Show heap status -> Check**
	* Window -> Preferences -> Java -> Editor -> Content Assist -> Completion inserts -> Select
	* Window -> Preferences -> Run/Debug -> Console -> Limit console output -> Check
	* **Window -> Preferences -> Run/Debug -> Console -> Console buffer size (characters) -> 8000000**
* Remove annoying completions
	* Window -> Preferences -> Java -> Appearance -> Type Filters -> Add
		* `ch.qos.logback.core.recovery.ResilientSyslogOutputStream`
		* `org.assertj.*.Arrays`
		* `org.assertj.*.Assertions`
* Assign the File Associations for .class files without sources to enhanced class decompiler
	* Window -> Preferences -> Java -> Debug -> Use advanced source lookup (JRE 1.5 and higher) -> [Uncheck](https://github.com/ecd-plugin/ecd/issues/58#issuecomment-580295517)
	* Window -> Preferences -> General -> Editors -> *.class without source -> Class Decompiler Viewer -> Default
	* Window -> Preferences -> General -> Editors -> *.class -> Class Decompiler Viewer -> Default
 * Make it easier to debug decompiler classes
	* Window -> Preferences -> Java -> Decompiler -> Output original line numbers as comments -> Check
 	* Window -> Preferences -> Java -> Decompiler -> Align code for debugging -> Check
    * (make sure a Default Class Decompiler is selected at the top, otherwise saving the dialog might not work)
* Import each project into a separate Working Set
	* with that it becomes easy to commit individual projects using Git Staging View by selecting the Working Sets
	* except invesdwin-oss requires each project to be selected individually since Git Submodules need to be committed separately
	* or use `mr commit` or direct git commands per project/repository on the console
* You can configure IntelliJ Keymap if desired using [this plugin](https://github.com/IntelliJIdeaKeymap4Eclipse/IntelliJIdeaKeymap4Eclipse)
	* with Eclipse Keymap on macOS `CTRL+SPACE` might require remapping so that code completion becomes usable (macOS might occupy that shortcut): Window -> Preferences -> Keys -> Content Assist
* Some modules do not support getting resolved from inside Eclipse, you can resolve them from the maven repository instead by closing or removing the project in eclipse. An example is the protected module `invesdwin-trading-jforex-runtime-bundle`. Though normally the pom.xml contains hints/tips like this for specific modules that require workarounds like this.
* Removing JPA Change Events which can slow down the IDE: https://stackoverflow.com/questions/19649847/eclipse-jpa-project-change-event-handler-waiting
* Disable Spring Boot validations that can slow down the IDE:
	* Window -> Preferences -> Language Servers -> Spring Language Servers -> Boot 2.x Validation -> Enablement -> Off
	* Window -> Preferences -> Language Servers -> Spring Language Servers -> Boot 3.x Validation -> Enablement -> Off
* To prevent deadlocks when importing projects in Eclipse 2022-09 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=579076):
	* Window -> Prefernences -> Maven -> Annotation Processing -> Select Annotation Processing Mode -> Do not automatically configure/execute annotation processing from pom.xml
* To prevent the debugger always stopping on uncaught exceptions:
    * Window -> Preferences -> Java -> Debug -> Suspend execution on uncaught exceptions -> Off

## Eclipse Troubleshooting

* In 2023 eclipse-m2e had a bug where resources were not copied correctly. So if you start a process and it complains about files being missing, you might be affected by [this bug](https://github.com/eclipse-m2e/m2e-core/issues/1511). Try upgrade eclipse-m2e. Alternatively copy the resources again via maven: `mvn process-resources process-test-resources -T1C`
* When eclipse is not closed properly it might crash on startup due to corrupt snap files or it might fail to find jvm internal classes because of corrupt indexes. To solve this, the workspace needs to be repaired:
```sh
cd <ECLIPSE_WORKSPACE_FOLDER>
rm -rf .metadata/.plugins/org.eclipse.jdt.core
rm -rf .metadata/.plugins/org.eclipse.core.resources/*.snap
eclipse -clean
```

## IntelliJ Tips

* Import all projects as Maven using a parent-pom that lists projects as modules (see above for an example pom.xml)
	* IMPORANT: Make sure not to use paths that contain symlinks, as IntelliJ has issues with that which are noticeable when trying to execute tests
* Install plugins:
	* [Eclipse-Code-Formatter](https://github.com/krasa/EclipseCodeFormatter#instructions)
		* use "Resolve project specific config" (should be equivalent to "Eclipse [built-in]")/invesdwin-maven-plugin-parent/invesdwin-maven-plugin/src/main/java/invesdwin-eclipse-settings/.settings/org.eclipse.jdt.ui.prefs)
	* [Save Actions plugin](https://plugins.jetbrains.com/plugin/7642-save-actions) and configure:
		* File -> Preferences -> Editor -> Code Style -> Java -> Imports -> Packages to Use Import with '*' -> Remove All
		* File -> Preferences -> Editor -> Code Style -> Java -> Imports -> Class count to use import with '*' -> 99
		* File -> Preferences -> Editor -> Code Style -> Java -> Imports -> Names count to use static import with '*' -> 99
		* File -> Preferences -> Other Settings -> Save Actions -> Activate save actions on save -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Optimize imports -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Reformat file -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Add final modifier to field -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Add final modifier to local variable or parameter except if it is implicit -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Add missing @Override annotations -> Check
		* File -> Preferences -> Other Settings -> Save Actions -> Add blocks to if/while/for statements -> Check
	* [Spotbugs](https://plugins.jetbrains.com/plugin/14014-spotbugs) and configure
		* File -> Preferences -> Tools -> SpotBugs -> General -> Analyze affected files after compile -> Check
		* File -> Preferences -> Tools -> SpotBugs -> General -> Analyze affected files after auto make -> Check
		* File -> Preferences -> Tools -> SpotBugs -> Analysis Effort -> Minimal
	* [Checkstyle](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea) and configure
		* File -> Preferences -> Tools -> Checkstyle -> Third-Party Checks -> [invesdwin-checkstyle-plugin](https://github.com/subes/invesdwin-checkstyle-plugin).jar
		* Download [checkstyle.config.suppression.xml](https://github.com/subes/invesdwin-maven-plugin/blob/master/invesdwin-maven-plugin-parent/invesdwin-maven-plugin/src/main/java/invesdwin-eclipse-settings/.settings/checkstyle.config.suppression.xml) and [checkstyle.config.xml](https://github.com/subes/invesdwin-maven-plugin/blob/master/invesdwin-maven-plugin-parent/invesdwin-maven-plugin/src/main/java/invesdwin-eclipse-settings/.settings/checkstyle.config.xml)
		* File -> Preferences -> Tools -> Checkstyle -> Configuration File -> Select the downloaded `checkstyle.config.xml`
		* set `config_loc` property to the path where `checkstyle.config.suppression.xml` resides
		* activate the new configuration file (checkbox column)
* To prevent import errors for `sun.misc.Unsafe` 
	* File -> Settings -> Build, Execution, Deployment -> Compiler -> Java Compiler -> Use '--release' option for cross compilation (Java 9 and later) -> Uncheck
* You can configure Eclipse Keymap if desired via: 
	* File -> Settings -> Keymap -> Eclipse
* To enable automatic builds configure
	* File -> Preferences -> Build, Execution, Deployment -> Compiler -> Build project automatically -> Check
	* File -> Preferences -> Build, Execution, Deployment -> Compiler -> Compile independent modules in parallel -> Automatic
* Some modules do not support getting resolved from inside IntelliJ. An example is the protected module `invesdwin-trading-jforex-runtime-bundle`. You can resolve them from the maven repository instead by ignoring the project in IntelliJ:
	* Right Click Project -> Maven -> Ignore Project -> Yes
        * then Right Click Parent Project -> Maven -> Reload Project

### IntelliJ Annotation Processing Workarounds

Configure annotation processing properly:
* Use Eclipse compiler (more robust against errors): 
	* File -> Preferences -> Build, Execution, Deployment -> Compiler -> Java Compiler -> Use Compiler -> Eclipse
* Prevent generated classes from being deleted right after getting generated:
	* File -> Preferences -> Build, Execution, Deployment -> Compiler -> Clear output directory on rebuild (otherwise generated classes from maven will be deleted)  -> Uncheck
* Make sure annotation processing generates files before trying to compile the source code:
	* File -> Preferences -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Run processors in a separate step before compiling java (-proc:only mode) -> Check
* For more information: 
	* https://youtrack.jetbrains.com/issue/IDEA-253719
	* https://youtrack.jetbrains.com/issue/IDEA-253720

**Obsolete Workaround 1:** If annotation processing causes errors, a workaround seems to be using maven for generating classes and disabling annotation processing completely:
* Go to: File -> Preferences -> Build, Execution, Deployment -> Compiler -> Annotation Processors
	* Add a new profile (+ icon) with name "Disabled" and uncheck: "Enable annotation processing" then move all modules into disabled profile (-> icon).
	* OR just uncheck "Enable annotation processing" for all profiles individually.
	* Sadly this setting is not persistent in IntelliJ and needs to be reapplied after any maven related changes that cause a reload of the maven modules. This can be prevented by: 
		* File -> Preferences -> Build, Execution, Deployment -> Build Tools -> Reload project after changes in build scripts -> Uncheck
* Run `mvn clean generate-sources` from command line or do it in IntelliJ via: 
	* Right Click Root Project -> Maven -> Generate Sources and Update Folders

**Obsolete Workaround 2:** Alternatively you can use Maven for building in IntelliJ by checking:
* File -> Preferences -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner -> Delegate IDE build/run actions to maven -> Check
* File -> Preferences -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner -> Skip Tests -> Check
* Then speed up build by enabling parallel maven builds
	* File -> Preferences -> Build, Execution, Deployment -> Build Tools -> Maven -> Thread count -> 1C
		* "1C" stands for one thread per available cpu core

## Visual Studio Code Tips

* Install extensions:
	* [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
 	* [Eclipse Keymap](https://marketplace.visualstudio.com/items?itemName=alphabotsec.vscode-eclipse-keybindings)
* Import all projects as Maven by opening the respective workspace root folder, then navigating to the Java Projects view and telling it to import everything as Maven (if it is confused by Gradle files)
	* IMPORANT: Make sure not to use paths that contain symlinks, as VS-Code has issues with that which are noticeable when trying to execute tests (https://stackoverflow.com/a/76516730/67492)
* Change java version and redirect JavaSE-1.8 to a reject JDK (though with a correct path to a recent JDK installation):
	* File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Startup -> `Java › Jdt › Ls › Java: Home` -> Edit in settings.json -> Put `"java.jdt.ls.java.home": "/usr/lib/jvm/default",`
    * File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Startup -> `Java › Jdt › Ls: Vmargs` -> Edit in settings.json -> Put `"java.jdt.ls.vmargs": "-Xmx5g",`
    * File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Installed JDKs -> `Java › Configuration: Runtimes` -> Edit in settings.json -> Uncheck -> Put `"java.configuration.runtimes": [ { "name": "JavaSE-1.8", "path": "/usr/lib/jvm/default" }, ],`
* In Java Projects configure (three dots)
	* Hierarchical View -> Click 
 * Configure settings:
	* File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Maven -> `Java › Maven: Download Sources` -> Check
 	* File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Other -> `Java › Eclipse: Download Sources` -> Check
	* File -> Preferences -> Settings -> Extensions -> Language Support for Java(TM) by Red Hat -> Other -> `Java › Editor: Reload Changed Sources` -> Auto

```json
{
    "workbench.colorTheme": "Default Light Modern",
    "redhat.telemetry.enabled": false,
    "java.configuration.runtimes": [ { "name": "JavaSE-1.8", "path": "/usr/lib/jvm/default" }, ],
    "workbench.startupEditor": "none",
    "java.jdt.ls.java.home": "/usr/lib/jvm/default",
    "java.maven.downloadSources": true,
    "java.eclipse.downloadSources": true,
    "java.jdt.ls.vmargs": "-Xmx5g",
}
```

## AI Copilot/Assistant Tips

* IMPORTANT:
	* If you use AI plugins in your IDE, make sure **Optional Telemetry** is disabled or **Privacy Mode** is enabled in your personal account to achieve **Zero Data Retention (ZDR)**
	* When using **online/web prompts**, the information is generally shared and used for training the models. So be careful to **share no sensitive information**
	* Be careful when using CLI/IDE **AI tools that can directly read and modify files** while being able to invoke arbitrary commands, code, or web requests. The AI may have access to the whole computer, or at least everything visible from within the IDE and could leak data via **prompt injection**. Consider using a **sandbox** like a virtual machine and **share no sensitive information**.
* For Visual Studio Code:
	* Separate distributions where you can add extensions and configure just like any other Visual Studo Code installation:
 		* [Cursor](https://cursor.com/home)
   		* [Windsurf](https://windsurf.com/)
     	* [Kiro](https://kiro.dev/)
 	* AI extensions for vanilla Visual Studio Code installations:
  		* [Roo Code](https://roocode.com/)
   		* [Cline](https://cline.bot/)
     	* [Kilo Code](https://kilo.ai/)
      	* [Codex](https://openai.com/codex/)
* For IntelliJ:
	* [JetBrains AI](https://www.jetbrains.com/ai/) and [Junie](https://www.jetbrains.com/junie/)
 	* [GitHub Copilot](https://plugins.jetbrains.com/plugin/17718-github-copilot--your-ai-pair-programmer)
  	* [Windsurf](https://plugins.jetbrains.com/plugin/20540-windsurf-plugin-for-python-js-java-go--)
  	* [Kilo Code](https://plugins.jetbrains.com/plugin/28350-kilo-code)
* For Eclipse:
	* [GitHub Copilot](https://marketplace.eclipse.org/content/github-copilot)

## Support

If you need further assistance or have some ideas for improvements and don't want to create an issue here on github, feel free to start a discussion in our [invesdwin-platform](https://groups.google.com/forum/#!forum/invesdwin-platform) mailing list.
