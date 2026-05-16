# freemarker-test-graalvm-natice

Test project for GraalVM support. This is not built by default.

## Quickstart

1. Download and install GraalVM 21+, if you haven't already. If it's installed to a non-standard location, also set the
   `GRAALVM_HOME` environment variable (or the `JAVA_HOME`, if you use GraalVM by default anyway), otherwise the build
   will fail!

2. Build the test project native image: 

   ```shell
   ./gradlew :freemarker-test-graalvm-native:nativeCompile 
   ```

3. Run the native executable it has created:

   ```shell
   ./freemarker-test-graalvm-native/build/native/nativeCompile/freemarker-test-graalvm-native 
   ```

   Note: On Windows, use `\`-s instead of `/`-s in the executable path!
   
   The executable should output should be similar to : 
   
   ```txt
   INFO: name : FreeMarker Native Demo, version : 2.3.35-nightly
   Jan 15, 2025 4:28:19 PM freemarker.log._JULLoggerFactory$JULLogger info
   INFO: result :
   <html>
       <head>
           <title>Hello : FreeMarker GraalVM Native Demo</title>
       </head>
       <body>
           <h1>Hello : FreeMarker GraalVM Native Demo</h1>
           <p>Test template for Apache FreeMarker GraalVM native support (2.3.35-nightly)</p>
       </body>
   </html>
   ```

## CI (GitHub workflow)

GraalVM native test for this module is included in the GitHub  [CI](../.github/workflows/ci.yml) workflow.
