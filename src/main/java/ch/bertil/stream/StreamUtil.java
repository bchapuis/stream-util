package ch.bertil.stream;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {

    public static <T> Stream<Stream<T>> partition(Stream<T> stream, int partitionSize, int batchSize) {
        Spliterator<Stream<T>> spliterator = new PartitioningSpliterator<T>(stream.spliterator(), partitionSize, batchSize);
        return StreamSupport.stream(spliterator, true);
    }

}
