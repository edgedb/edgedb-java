.. _gel_java_connecting:

=====================
Connection Parameters
=====================

The ``GelClientPool`` constructor can consume an ``GelConnection`` class 
containing connection arguments for the client.

Most of the time, the connection arguments are implicitly resolved via 
:ref:`projects <ref_intro_projects>`. In other cases, the ``GelConnection``
class exposes ways to construct connection arguments.

Connection builder
------------------

You can use a provided builder by calling the ``builder()`` method on 
``GelConnection``

.. code-block:: java

    var builder = GelConnection.builder();

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

``GelConnection`` also exposes static methods used to parse connection 
arguments from different sources.

fromDSN
^^^^^^^

This method parses a :ref:`DSN <ref_dsn>` string into an ``GelConnection``.

.. code-block:: java

    var connection = GelConnection
        .fromDSN("gel://user:pass@host:port/db");

fromProjectFile
^^^^^^^^^^^^^^^

This method resolves connection arguments from a ``gel.toml`` 
:ref:`project file <ref_intro_projects>`.

.. code-block:: java

    var connection = GelConnection
        .fromProjectFile("~/myproject/gel.toml");

fromInstanceName
^^^^^^^^^^^^^^^^

This method resolves the connection arguments for a given instance name.

.. code-block:: java

    var connection = GelConnection
        .fromInstanceName("my_instance_name");

resolveEdgeDBTOML
^^^^^^^^^^^^^^^^^

This method is the default behaviour, it scans the current directory for
a ``gel.toml`` project file, if none is found, the parent directory is 
scanned recursivly until a project file is found; if none is found, a 
``FileNotFoundException`` is raised.

.. code-block:: java

    var connection = GelConnection
        .resolveEdgeDBTOML();

parse
^^^^^

The parse method will resolve the given arguments as well as apply
environment variables to the connection, following the 
:ref:`priority levels <ref_reference_connection_priority>` of arguments.

.. code-block:: java

    var connection = GelConnection
        .parse("my_instance");

