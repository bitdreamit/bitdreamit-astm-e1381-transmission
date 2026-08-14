# Contributing to bitdreamit-astm-e1381-transmission

Thanks for your interest in improving this project! This document describes
how to set up a development environment and what the code-review expectations are.

## Development environment

1. **JDK 8 or later** (OpenJDK 17 recommended; OpenJDK 21 also tested).
2. **Mirth Connect 4.5+ runtime jars** - extract them to `../mirth-libs/`:
   ```
   mirth-libs/
     server/mirth-server.jar
     server/donkey-server.jar
     client/mirth-client.jar
     client/mirth-client-core.jar      # contains TransmissionModeProperties
     test/junit-4.13.2.jar
     test/hamcrest-core-1.3.jar
   ```
3. **IntelliJ IDEA 2023+** (Community Edition is fine) or any JDK-aware editor.

## Code style

- 4-space indentation; no tabs.
- `package` and `import` statements grouped: `java.*`, `javax.*`, then
  third-party (`org.apache.log4j`, `com.mirth.*`), then
  `com.bitdreamit.*`.
- Class-level Javadoc on every `public` class; method-level Javadoc on
  every `public` method that is non-trivial.
- No checked exceptions in new code unless the caller can reasonably
  recover (use `ASTME1381FrameException` for protocol errors).
- Production logging via `org.apache.log4j.Logger`; never `System.out`.

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
fix: bound ENQ establishment wait by configured timeout

The original implementation called in.read() with no timeout, which
would block forever if the peer never sent ENQ. Use readByteWithTimeout
with the configured establishmentTimeout (default 15s).

Closes #42.
```

## Pull-request checklist

- [ ] Branch is rebased on the latest `main`.
- [ ] All tests pass: `cd distribution && ./build.sh test`
- [ ] If you added a new property, it is exposed in
      `getPropertyDescriptors()`, has a getter AND setter, and is
      included in `getPurgedProperties()` if it affects runtime
      behaviour (not just UI).
- [ ] If you added a new public class, it has a Javadoc class-level
      comment explaining its purpose and thread-safety.
- [ ] No `System.out` / `printStackTrace` calls.
- [ ] `transmissionmode.xml` and the `plugin.xml` files are updated if
      class names or jar names change.
- [ ] `CHANGELOG.md` has a new entry under `[Unreleased]` describing
      the change.

## Releasing

1. Bump `ASTME1381Constants.PLUGIN_VERSION` and the `<pluginVersion>` in
   both `plugin.xml` files.
2. Update `CHANGELOG.md`: move items from `[Unreleased]` to a new
   `[X.Y.Z] - YYYY-MM-DD` section.
3. Tag: `git tag -s v1.X.Y -m "Release 1.X.Y"`.
4. Push tags: `git push --tags`.
5. CI will build the jars and attach them to the GitHub Release.

## Reporting security issues

**Do not open a public GitHub issue for security problems.** Email
security@bitdreamit.com with a description and (if possible) a
reproducer. We will respond within 72 hours.
