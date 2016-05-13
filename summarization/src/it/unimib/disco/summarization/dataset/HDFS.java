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

	public HDFS (InputFile file) {
		this.file = file;
		hadoopPath = "hdfs://master:54310/tmp";
		conf = new Configuration();
	}
	
	public String createHadoopCopy() throws IOException, URISyntaxException {
		fs = FileSystem.get(new URI("hdfs://master:54310"), conf);
		fs.createNewFile(new Path(hadoopPath));
		fs.copyFromLocalFile(new Path(file.name()), new Path(hadoopPath));
		return hadoopPath;
	}
	
	@SuppressWarnings("deprecation")
	public void deleteHadoopCopy() throws IOException {
		fs.delete(new Path(hadoopPath));
		fs.close();
	}
}
