# Build container for bitdreamit-astm-e1381-transmission
# Compiles the shared module and runs unit tests in a clean environment.
#
# The server and client modules require Mirth Connect jars that are not
# available in any public Maven repo. Mount them at /mirth-libs/ to
# compile the full project.
#
#   docker build -t bitdreamit-astm-e1381-transmission:build .
#   docker run --rm -v $HOME/mirth-libs:/mirth-libs \
#       -v $PWD/out:/out bitdreamit-astm-e1381-transmission:build
#
FROM eclipse-temurin:17-jdk-jammy

LABEL org.opencontainers.image.title="bitdreamit-astm-e1381-transmission-builder" \
      org.opencontainers.image.description="Build & test container for the ASTM E1381-02 Mirth Connect plugin" \
      org.opencontainers.image.source="https://github.com/bitdreamit/bitdreamit-astm-e1381-transmission" \
      org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /workspace

# Copy the project source
COPY . /workspace/

# Default command: compile shared module + tests, run tests
# Full server/client compile requires Mirth jars mounted at /mirth-libs/
CMD ["bash", "-c", "\
    set -e; \
    mkdir -p /out/shared /out/test; \
    echo '[1/4] Compiling shared module (no external deps)...'; \
    javac -d /out/shared -sourcepath /workspace/shared/src $(find /workspace/shared/src -name '*.java'); \
    echo '[2/4] Compiling tests (needs shared + junit)...'; \
    if [ -d /mirth-libs ]; then \
        TEST_CP=/out/shared:/mirth-libs/test/junit-4.13.2.jar:/mirth-libs/test/hamcrest-core-1.3.jar; \
        javac -cp \"$TEST_CP\" -d /out/test -sourcepath /workspace/test/src $(find /workspace/test/src -name '*.java'); \
        echo '[3/4] Running JUnit tests...'; \
        java -cp \"$TEST_CP:/out/test\" org.junit.runner.JUnitCore \
            com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameTest \
            com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381RetryMetricsTest \
            com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameCorruptionTest \
            com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381RoundTripTest; \
        echo '[4/4] Building full jars (server + client)...'; \
        cd /workspace/distribution && ./build.sh; \
        cp -r /workspace/out /out/; \
    else \
        echo '[3/4] Skipped (no /mirth-libs mounted)'; \
        echo '[4/4] Shared module compiled successfully.'; \
    fi"]
