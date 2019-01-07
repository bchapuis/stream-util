package ch.bertil.stream;

import de.bytefish.pgbulkinsert.PgBulkInsert;
import de.bytefish.pgbulkinsert.mapping.AbstractMapping;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.postgresql.PGConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class Main {

    static class PgBulkInsertConsumer<TEntity> implements Consumer<Collection<TEntity>> {

        private final PgBulkInsert<TEntity> bulkInsert;

        private final DataSource dataSource;

        public PgBulkInsertConsumer(PgBulkInsert<TEntity> bulkInsert, PoolingDataSource dataSource) {
            this.bulkInsert = bulkInsert;
            this.dataSource = dataSource;
        }

        @Override
        public void accept(Collection<TEntity> entities) {
            try (Connection connection = dataSource.getConnection()) {
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                bulkInsert.saveAll(pgConnection, entities);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    static class IntegerMapping extends AbstractMapping<Integer> {

        public IntegerMapping() {
            super("public", "integers");
            mapInteger("i", Integer::intValue);
        }

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int num = 10000000;
        int partitionSize = 1000;
        int batchSize = 10;

        // create table integers (i int);
        PoolingDataSource dataSource = dataSource("jdbc:postgresql://localhost:5432/osm?user=osm&password=osm");
        PgBulkInsert<Integer> bulkInsert = new PgBulkInsert<>(new IntegerMapping());

        ForkJoinPool forkJoinPool = new ForkJoinPool(10);
        forkJoinPool.submit(() ->  StreamUtil
                .partition(IntStream.range(0, num).mapToObj(i -> i).parallel(), partitionSize, batchSize)
                .forEach(new PgBulkInsertConsumer<Integer>(bulkInsert, dataSource))).get();
    }

    public static PoolingDataSource dataSource(String conn) {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(conn, null);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(connectionPool);
        return dataSource;
    }

}
