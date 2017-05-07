The current gradle build is work in progress, so use the Ant build, as
described in README.md!

To build the project, go to the project home directory, and issue:

    ./gradlew jar test
  
On Windows this won't work if you are using an Apache source release (as
opposed to checking the project out from Git), as due to Apache policy
restricton `gradle\wrapper\gradle-wrapper.jar` is missing from that. So you
have to download that very common artifact from somewhere manually. On
UN*X-like systems (and from under Cygwin shell) you don't need that jar, as
our custom `gradlew` shell script does everything itself.
