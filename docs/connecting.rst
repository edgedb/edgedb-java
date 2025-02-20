.. _gel_java_connecting:

=====================
Connection Parameters
=====================

The ``GelClientPool`` constructor can optionally consume a ``GelConnection``
object containing connection arguments for the client.

Most of the time, the connection arguments are implicitly resolved via 
:ref:`projects <ref_intro_projects>`. In other cases, the ``GelConnection``
class exposes ways to construct connection arguments.

Connection builder
------------------

You can use a provided builder by calling the ``builder()`` method on 
``GelConnection``

.. code-block:: java

    var builder = GelConnection.builder();
    var connection = builder.build();

The builder accepts several `parameters <https://docs.edgedb.com/database/reference/connection>`_
which are used to construct the final ``GelConnection``.

If no parameters are provided, the default behavior is to search for the
project's ``gel.toml`` file.

The builder has the following methods:

+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| Name                                | Type                     | Description                                                             |
+=====================================+==========================+=========================================================================+
| ``withInstance``                    | String                   | The name of the gel instance to connect to.                             |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withDsn``                         | String                   | The DSN to connect to. See: `here <https://www.edgedb.com/docs/reference/dsn>`_ for more information. |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withCredentials``                 | String                   | A json representation of the connection arguments.                      |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withCredentialsFile``             | Path                     | A file to read as the credentials.                                      |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withHost``                        | String                   | The hostname to connect as.                                             |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withPort``                        | int                      | The port of the database.                                               |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withBranch``                      | String                   | The name of the branch to use.                                          |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withDatabase``                    | String                   | The name of the database to use. (Deprecated in favor of withBranch)    |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withUser``                        | String                   | The username to connect as.                                             |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withPassword``                    | String                   | The password used to authenticate.                                      |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withSecretKey``                   | String                   | The secret key used to use for cloud connections.                       |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withTLSCertificateAuthority``     | String                   | The TLS certificate authority, used to verifiy the server certificate.  |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withTLSCertificateAuthorityFile`` | Path                     | A file to read as the TLS certificate authority.                        |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withTlsSecurity``                 | TLSSecurityMode          | The TLS security policy.                                                |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withTLSServerName``               | String                   | The TLS server name to use. Overrides the hostname.                     |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withWaitUntilAvailable``          | WaitTime                 | The time to wait for a connection to the server to be established.      |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
| ``withServerSettings``              | HashMap<String, String>  | Additional settings for the server connection. Currently has no effect. |
+-------------------------------------+--------------------------+-------------------------------------------------------------------------+
