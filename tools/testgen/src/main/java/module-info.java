module com.gel.testgen {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.gel.driver;

    exports com.gel.testgen to com.fasterxml.jackson.databind;
}