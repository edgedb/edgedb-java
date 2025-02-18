.. _edgedb-java-configuration:

=============
Configuration
=============

The driver can be configured by passing an ``EdgeDBClientConfig`` object 
into the ``GelClientPool`` constructor. The client config contains immutable 
settings for client behavior. You can construct a new ``EdgeDBClientConfig``
with the builder subclass like so:

.. code-block:: java

    var builder = EdgeDBClientConfig.builder();

    ..

    var config = builder.build();

These are the following methods exposed on the configuration builder:

+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| Name                         | Parameters              | Description                                                                                 |
+==============================+=========================+=============================================================================================+
| ``withPoolSize``             | ``int``                 | The maximum number of concurrent clients to allow in the client pool.                       |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withRetryMode``            | ``ConnectionRetryMode`` | The connection retry mode for new clients.                                                  |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withMaxConnectionRetries`` | ``int``                 | The maximum number of connection attempts to make before throwing.                          |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withConnectionTimeout``    | ``long``, ``TimeUnit``  | The amount of time to wait for a connection to be established.                              |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withMessageTimeout``       | ``long, TimeUnit``      | The amount of time to wait for an expected response from EdgeDB.                            |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withExplicitObjectIds``    | ``boolean``             | Whether or not the ``id`` property of objects need to be explicitly included in shapes.     |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withImplicitLimit``        | ``long``                | Set an implicit result limit.                                                               |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withImplicitTypeIds``      | ``boolean``             | Whether or not ``__tid__`` will be included implicitly on all objects.                      |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withNamingStrategy``       | ``NamingStrategy``      | The naming strategy used to correlate field names to schema field names.                    |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``useFieldSetters``          | ``boolean``             | Whether or not to try to use a setter method for a field being deserialized.                |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withClientType``           | ``ClientType``          | The client type of the pool.                                                                |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withClientAvailability``   | ``int``                 | The number of clients to keep instansiated within the pool, regardless of connection state. |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+
| ``withClientMaxAge``         | ``Duration``            | The max age of an inactive client within the client pool.                                   |
+------------------------------+-------------------------+---------------------------------------------------------------------------------------------+

This configuration object can then be passed into the constructor of 
a ``GelClientPool``.

In addition to client-level configuration, the driver offers session-level
configuration. This type of configuration is controlled using methods prefixed
by ``with``. These methods return a new client instance which shares the same
connection, pool, and client configuration.

.. code-block:: java

    var clientPool = new GelClientPool();

    var appliedGlobalClient = clientPool.withGlobals(new HashMap<>(){{
        put("current_user_id", ...);
    }});