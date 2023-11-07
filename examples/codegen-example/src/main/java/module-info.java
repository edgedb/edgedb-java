module com.edgedb.examples.codegen {
    requires com.edgedb.driver;
    requires org.jetbrains.annotations;
    exports com.edgedb.examples.codegen.generated.results to com.edgedb.driver;
    exports com.edgedb.examples.codegen.generated.interfaces to com.edgedb.driver;
}