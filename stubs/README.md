# stubs/ - Compile-time fallbacks for missing Mirth classes

This directory contains **stub interfaces** for Mirth Connect classes that
are sometimes missing from partial Mirth installations. They exist so the
project can still be compiled when the user's classpath is incomplete.

## When to use these stubs

You should ONLY add this directory to your compile source roots if your
build fails with one of the following errors:

```
java: cannot access com.mirth.connect.donkey.util.purge.Purgable
  class file for com.mirth.connect.donkey.util.purge.Purgable not found
```

```
java: cannot find symbol
  symbol:   method getPluginPointName()
  location: class com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
```

These errors mean your compile classpath is missing `donkey-server.jar`
(which contains `com.mirth.connect.donkey.util.purge.Purgable`).

## The proper fix (preferred)

**Add `donkey-server.jar` to your compile classpath.** That jar ships
with every Mirth Connect 4.x server installation, typically at:

```
$MIRTH_HOME/lib/donkey-server.jar
```

-or, in newer versions-

```
$MIRTH_HOME/lib/extensions/server/donkey-server.jar
```

Once you've located it, copy it to:

```
../mirth-libs/server/donkey-server.jar
```

Then update the IntelliJ project library `mirth-server` to include it
(File → Project Structure → Libraries → mirth-server → + donkey-server.jar),
or - if you are using Maven - the parent `pom.xml` already declares it
as a `system`-scope dependency pointing at `${mirth.libs}/server/donkey-server.jar`.

After that, you do NOT need the stubs in this directory.

## Using the stubs (fallback only)

If you genuinely cannot locate `donkey-server.jar`, you can compile
against the stub interfaces instead:

### IntelliJ IDEA

1. File → Project Structure → Modules
2. Select the `shared` module → Sources tab
3. Click `+ Add Content Root` and select this `stubs/` directory
4. Mark it as a source root (right-click → Mark Directory As → Sources Root)
5. Rebuild

### Maven

Add this to the `shared/pom.xml` `<build>` section:

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <compilerArgument>-Xlint:none</compilerArgument>
    </configuration>
    <executions>
        <execution>
            <id>default-compile</id>
            <configuration>
                <additionalSourceRoots>
                    <additionalSourceRoot>${project.basedir}/../stubs</additionalSourceRoot>
                </additionalSourceRoots>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Command-line (javac)

```bash
javac -cp ../mirth-libs/client/mirth-client-core.jar \
      -sourcepath shared/src:stubs \
      -d out/shared \
      shared/src/com/bitdreamit/connect/plugins/transmission/astm/shared/*.java
```

## What NOT to do

- **DO NOT package the stub classes into the final jar.** They would
  clash with the real `Purgable` interface at runtime. The `distribution/build.sh`
  script does NOT include this directory in the produced jars.
- **DO NOT rely on the stubs at runtime.** They are compile-time-only
  placeholders. The real Mirth Connect server provides the real classes.
- **DO NOT use the stubs as a substitute for proper Mirth jar management.**
  Always try to obtain the real `donkey-server.jar` first.

## Currently provided stubs

| Class | Why it's needed |
|-------|-----------------|
| `com.mirth.connect.donkey.util.purge.Purgable` | The parent class `TransmissionModeProperties` (in `mirth-client-core.jar`) implements `Purgable`. Without `Purgable.class` on the classpath, the parent class fails to load and the entire `shared` module produces cascading compile errors. |

If you encounter other missing-class errors, please report them so we can
add additional stubs or - better - fix the classpath setup documentation.
