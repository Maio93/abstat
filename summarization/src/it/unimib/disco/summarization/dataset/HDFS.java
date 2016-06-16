package it.unimib.disco.summarization.dataset;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HDFS {

	private InputFile file;
	private String hadoopPath;
	private Configuration conf;
	private FileSystem fs;

	public HDFS(InputFile file) {
		this.file = file;
		if (file.name().startsWith("hdfs"))
			hadoopPath = file.name();
		else
			hadoopPath = "hdfs://master:54310" + file.name();
		conf = new Configuration();
	}

	public String createHadoopCopy() throws IOException, URISyntaxException {
		System.out.println("Creazione file: " + file.name());
		fs = FileSystem.get(new URI("hdfs://master:54310"), conf);
		fs.createNewFile(new Path(hadoopPath));
		fs.copyFromLocalFile(new Path(file.name()), new Path(hadoopPath));
		return hadoopPath;
	}

	@SuppressWarnings("deprecation")
	public void deleteHadoopCopy() throws IOException {
		System.out.println("Eliminazione file: " + file.name());
		fs.delete(new Path(hadoopPath));
		fs.close();
	}
}
