module com.edgedb.examples.codegen {
    requires com.edgedb.driver;
    requires org.jetbrains.annotations;
    exports com.edgedb.examples.codegen.generated.results to com.edgedb.driver;
}