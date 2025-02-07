.. _edgedb_java_transactions:

============
Transactions
============

Transactions are a robust concept to ensure your queries are executed,
even if network errors occur. To do this, simply use the ``transaction``
method on ``EdgeDBClient``.

.. code-block:: java

    client.transaction(tx -> 
        tx.execute("INSERT Person { name := $name, age := $age}", new HashMap<>(){{
            put("name", "Example Name");
            put("age", 1234);
        }}).thenCompose(v -> 
            tx.querySingle(Long.class, "SELECT count((SELECT Person))");
        );
    ).thenAccept(count -> {
        System.out.println(String.format("There are %d people in the database", count));
    })

It is important to note that you must use the ``tx`` parameter of the
transaction to perform queries, otherwise you won't get the benefits
of transactions.
