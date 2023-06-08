.. _edgedb_java_connecting:

=====================
Connection Parameters
=====================

The ``EdgeDBClient`` constructor can consume an ``EdgeDBConnection`` class 
containing connection arguments for the client.

Most of the time, the connection arguments are implicitly resolved via 
:ref:`projects <ref_intro_projects>`. In other cases, the ``EdgeDBConnection``
class exposes ways to construct connection arguments.

Connection builder
------------------

You can use a provided builder by calling the ``builder()`` method on 
``EdgeDBConnection``

.. code-block:: java

    var builder = EdgeDBConnection.builder();

The builder has the following methods:

+---------------------+-----------------+-----------------------------------------------------------------------+
| Name                | Type            | Description                                                           |
+=====================+=================+=======================================================================+
| ``withUser``        | String          | The username to connect as.                                           |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withPassword``    | String          | The password used to authenticate.                                    |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withDatabase``    | String          | The name of the database to use.                                      |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withHostname``    | String          | The hostname of the database.                                         |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withPort``        | int             | The port of the database.                                             |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withTlsca``       | String          | The TLS certificate authority, used to verify the server certificate. |
+---------------------+-----------------+-----------------------------------------------------------------------+
| ``withTlsSecurity`` | TLSSecurityMode | The TLS security policy.                                              |
+---------------------+-----------------+-----------------------------------------------------------------------+


Parse & constructor methods
---------------------------

``EdgeDBConnection`` also exposes static methods used to parse connection 
arguments from different sources.

fromDSN
^^^^^^^

This method parses a :ref:`DSN <ref_dsn>` string into an ``EdgeDBConnection``.

.. code-block:: java

    var connection = EdgeDBConnection
        .fromDSN("edgedb://user:pass@host:port/db");

fromProjectFile
^^^^^^^^^^^^^^^

This method resolves connection arguments from a ``edgedb.toml`` 
:ref:`project file <ref_intro_projects>`.

.. code-block:: java

    var connection = EdgeDBConnection
        .fromProjectFile("~/myproject/edgedb.toml");

fromInstanceName
^^^^^^^^^^^^^^^^

This method resolves the connection arguments for a given instance name.

.. code-block:: java

    var connection = EdgeDBConnection
        .fromInstanceName("my_instance_name");

resolveEdgeDBTOML
^^^^^^^^^^^^^^^^^

This method is the default behaviour, it scans the current directory for
a ``edgedb.toml`` project file, if none is found, the parent directory is 
scanned recursivly until a project file is found; if none is found, a 
``FileNotFoundException`` is raised.

.. code-block:: java

    var connection = EdgeDBConnection
        .resolveEdgeDBTOML();

parse
^^^^^

The parse method will resolve the given arguments as well as apply
environment variables to the connection, following the 
:ref:`priority levels <ref_reference_connection_priority>` of arguments.

.. code-block:: java

    var connection = EdgeDBConnection
        .parse("my_instance");

