module com.edgedb.examples {
    requires com.edgedb.driver;
    requires com.edgedb.core;
    requires org.slf4j;
    requires org.jooq.joou;
    requires org.reflections;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports com.edgedb.examples;
}