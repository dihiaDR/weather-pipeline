package io.bench.weather;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import javax.naming.Context;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

public class FlinkPipeline {

	private static final String BOOTSTRAP = "kafka:9092";
	private static final String BUCKET = "weather_processed";
	private static final String TOKEN = "adminadmin";

	public static void main(String[] args) throws Exception {

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// 1) Source Kafka (simple string)
		KafkaSource<String> source = KafkaSource.<String>builder()
				.setBootstrapServers(BOOTSTRAP)
				.setTopics("weather.raw")
				.setGroupId("flink-weather")
				.setStartingOffsets(OffsetsInitializer.earliest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		DataStream<String> lines = env
				.fromSource(source, org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(),
						"csv-source")
				.filter(line -> line.split(",").length >= 8); // sécurise

		// 2) Fenêtre tumbling 1 min en processing-time
		DataStream<String> aggregates = lines
				.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(10)))
				.aggregate(new CsvAggregator());

		// 3) Sink InfluxDB
		aggregates.addSink(new InfluxSink());

		env.execute("CSV → Flink → Influx");
	}

	/* -------- Agrégation (temp_avg · wind_max · count) -------- */
	public static class CsvAggregator implements AggregateFunction<String, double[], String> {
		@Override
		public double[] createAccumulator() {
			return new double[] { 0, -Double.MAX_VALUE, 0 };
		}

		@Override
		public double[] add(String v, double[] acc) {
			String[] f = v.split(",");
			double t = Double.parseDouble(f[2]);
			double w = Double.parseDouble(f[5]);
			acc[0] += t;
			acc[1] = Math.max(acc[1], w);
			acc[2]++;
			return acc;
		}

		@Override
		public String getResult(double[] acc) {
			long ts = System.currentTimeMillis() * 1_000_000; // ns
			return "weather temp_avg=" + (acc[0] / acc[2]) +
					",wind_max=" + acc[1] +
					",count=" + (long) acc[2] +
					" " + ts;
		}

		@Override
		public double[] merge(double[] a, double[] b) {
			return a;
		}
	}

	/* ----------------- Sink InfluxDB ----------------- */
	public static class InfluxSink extends RichSinkFunction<String> {
		private transient WriteApiBlocking write;

		@Override
		public void open(org.apache.flink.configuration.Configuration cfg) {
			InfluxDBClient cli = InfluxDBClientFactory.create(
					"http://influxdb:8086", TOKEN.toCharArray(), "weather", BUCKET);
			write = cli.getWriteApiBlocking();
		}

		@Override
		public void invoke(String lineProtocol, Context ctx) {
			write.writeRecord(WritePrecision.NS, lineProtocol);
		}
	}
}
