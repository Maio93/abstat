package it.unimib.disco.summarization.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation class Upload
 */
@WebServlet("/upload")
@MultipartConfig
public class Upload extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Upload() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().write("doGet");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		Part dataset = request.getPart("dataset");
		Part ontology = request.getPart("ontology");
		String email = request.getParameter("email");

		copyFile("./data/triples", getFilename(dataset), dataset);
		copyFile("./data/ontology", getFilename(ontology), ontology);

		response.getWriter().write("Upload riuscito, email: " + email);
	}

	private static String getFilename(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
				return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE
																													// fix.
			}
		}

		return null;
	}

	private void copyFile(String path, String name, Part filePart) throws IOException {
		new File(path).mkdirs();
		File output = new File(path + "/" + name);
		output.createNewFile();
		BufferedReader reader = new BufferedReader(new InputStreamReader(filePart.getInputStream()));
		PrintStream writer = new PrintStream(new FileOutputStream(output));
		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.print(line + "\n");
		}

		writer.close();
		reader.close();
	}
}
