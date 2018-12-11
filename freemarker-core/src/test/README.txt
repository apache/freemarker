The tests were moved to the freemarker-core-test and freemarker-core-test-java8 projects.
(This was necessary to avoid dependency loops. Some utility classes that are useful for testing
the core are also useful for testing the other modules. This those classes had to be moved into
a separate module, freemarker-test-utils, which however depends on freemarker-core to provide
template testing facilities and such. Hence, freemarker-core can't depend on freemarker-test-utils,
yet the classes in it are needed for testing freemarker-core, thus, yet another project had to be
added.)