module com.edgedb.testgen {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.edgedb.driver;

    exports com.edgedb.testgen to com.fasterxml.jackson.databind;
}