.. _edgedb_java_datamodeling:

=============
Data modeling
=============

The Java driver allows you to structure your query results as classes.

Basic representation
--------------------

You can simply create a class that matches the schema types' properties
by following the :ref:`scalar type map <edgedb_java_datatypes>`.

.. tabs::

    .. code-tab:: java
        :caption: Java

        @EdgeDBType
        public class Person {
            public String name;
            public int Age;
        }

    .. code-tab:: sdl
        :caption: Schema

        module default {
            type Person {
                property name -> str;
                property age -> int32;
            }
        }

There are a few requirements with the class representation:

* All classes that represent data need to be marked with the 
  ``@EdgeDBType`` annotation.

* Any multi-link property (collection) needs to be marked with the 
  ``@EdgeDBLinkType`` annotation.

* A field must be public *or* have a valid setter if
  ``useFieldSetters`` is ``true`` in the client configuration.

If a field cannot be mapped from a value within a result, it is simply ignored;
this allows the same Java type to be used for queries with different shapes.

Naming strategies
-----------------

Naming strategies control the map between Java names and schema names. By 
default, no mutation is applied to the field names of the java class; this means
``myFieldName`` is directly mapped to ``myFieldName``.

Default implementations of ``NamingStrategy`` are available as static methods
under the interface, for example:

.. code-block:: java
    
    var config = EdgeDBClientConfig.builder()
        .withNamingStrategy(NamingStrategy.snakeCase())
        .build();
        
chooses the ``snake_case`` naming strategy, which converts any given name to 
'snake_case'.

Using field setters
-------------------

You can configure whether or not the binding will attempt to use field setters
if present with the ``useFieldSetters`` configuration option. When this is 
``true``, the binding will attempt to find methods in your class that meet
the following requirements:

* Prefixed with ``set`` followed by the field name in ``PascalCase``;

* Contain one parameter with the same type of the field;

* The method is public and non-static;

For example, creating a bean that represents the ``Person`` schema type:

.. tabs::

    .. code-tab:: java
        :caption: Java

        @EdgeDBType
        public class Person {
            private String name;
            private int age;

            public void setName(String name) {
                this.name = name;
            }

            public void setAge(int age) {
                this.age = age;
            }

            public String getName() {
                return this.name;
            }

            public int getAge() {
                return this.age;
            }
        }

    .. code-tab:: sdl
        :caption: Schema

        module default {
            type Person {
                property name -> str;
                property age -> int32;
            }
        }

The driver will give priority to ``setName`` and ``setAge`` rather than 
reflection to set the field values.

Multi-link properties
---------------------

The JVM doesn't retain generic information for collection generics. To get 
around this, you must specify the type of the collection with the 
``@EdgeDBLinkType`` annotation.

.. tabs::

    .. code-tab:: java
        :caption: Java

        @EdgeDBType
        public class Person {
            public String name;
            public int age;

            @EdgeDBLinkType(Person.class)
            public List<Person> friends;
        }

    .. code-tab:: sdl
        :caption: Schema

        module default {
            type Person {
                property name -> str;
                property age -> int32;
                multi link friends -> Person;
            }
        }

The binding accepts any collection type that is: an array, a ``List<?>``, 
assignable from a ``List<?>``, or a ``HashSet<?>``.

Custom deserializers
--------------------

You can specify a constructor as a target for deserialization with the
``@EdgeDBDeserializer`` annotation. A deserializer has 2 valid modes of 
operation: enumeration consumers, and value consumers.

Enumerator consumer
^^^^^^^^^^^^^^^^^^^

An enumerator consumer takes only one parameter, an ``ObjectEnumerator``, 
which provides a direct handle to the deserialization pipeline. Calling
``next()`` preforms the deserialization step for one element and returns
an ``ObjectEnumerator.ObjectElement``, containing the name, type, and 
value.

.. code-block:: java

    @EdgeDBType
    public class Person {
        private String name;
        private int age;

        public Person(ObjectEnumerator enumerator) {
            try {
                ObjectEnumerator.ObjectElement element;
                while(enumerator.hasRemaining() && (element = enumerator.next()) != null) {
                    switch(element.getName()) {
                        case "name":
                            assert element.getType() == String.class;
                            this.name = (String)element.getValue();
                            break;
                        case "age":
                            assert element.getType() == Integer.class;
                            this.age = (int)element.getValue();
                            break;

                    }
                }
            } catch(EdgeDBException err) { // deserialization error
            
            } catch(OperationNotSupportedException err) { // read/IO error

            }
        }
    }

This approach isn't viable for large data structure maps. Instead, it is useful 
for other data type representations, like tuples:

.. code-block:: java

        @EdgeDBDeserializer
        public SimpleTuple(ObjectEnumerator enumerator) 
        throws EdgeDBException, OperationNotSupportedException {
            elements = new ArrayList<>();

            while(enumerator.hasRemaining()) {
                var enumerationElement = enumerator.next();

                assert enumerationElement != null;

                elements.add(new Element(
                    enumerationElement.getType(), 
                    enumerationElement.getValue()
                ));
            }
        }

Value consumers
^^^^^^^^^^^^^^^

Value consumers take in the fields' values in the constructor, mapped by a 
``@EdgeDBName`` annotation:

.. tabs::

    .. code-tab:: java
        :caption: Java

        @EdgeDBType
        public class Person {
            private final String name;
            private final int age;

            @EdgeDBDeserializer
            public Person(
                @EdgeDBName("name") String name,
                @EdgeDBName("age") int age
            ) {
                this.name = name;
                this.age = age;
            }
        }

    .. code-tab:: sdl
        :caption: Schema

        module default {
            type Person {
                property name -> str;
                property age -> int32;
                multi link friends -> Person;
            }
        }


Polymorphic types
-----------------

The binding supports polymorphic types, allowing you to reflect your abstract 
schema types in code. For example:

.. tabs::

    .. code-tab:: java
        :caption: Java

        @EdgeDBType
        public abstract class Media {
            public String title;
        }

        @EdgeDBType
        public class Show extends Media {
            public Long seasons;
        }

        @EdgeDBType
        public class Movie extends Media {
            public Long release_year;
        }

    .. code-tab:: sdl
        :caption: Schema

        module default {
            abstract type Media {
                required property title -> str {
                    constraint exclusive;
                }
            }
        
            type Movie extending Media {
                required property release_year -> int64;
            }
        
            type Show extending Media {
                required property seasons -> int64;
            }
        }

You can then specify ``Media`` as a result of a query; the binding will
then discover any subclasses of ``Media`` and deserialize the subclasses as a 
result.

.. code-block:: java

    client.query(Media.class, "SELECT Media { title, [IS Movie].release_year, [IS Show].seasons }")
        .thenAccept(result -> {
            for(var media : result) {
                if(media instanceof Show) {
                    var show = (Show)media;
                    System.out.println(String.format("Got show: %s, %d", show.title, show.seasons));
                } else if (media instanceof Movie) {
                    var movie = (Movie)media;
                    System.out.println(String.format("Got movie: %s, %d", movie.title, movie.release_year));
                }
            }
        });

