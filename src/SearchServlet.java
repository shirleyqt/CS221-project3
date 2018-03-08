

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import opennlp.tools.tokenize.SimpleTokenizer;

/**
 * Servlet implementation class SearchServlet
 */
@WebServlet("/search")
public class SearchServlet extends HttpServlet {
		
	public class ID_URL {
		public String docID;
		public String url;
		
		public ID_URL(String docID, String url) {
			this.docID = docID;
			this.url = url;
		}
	}
	
	
	private static final long serialVersionUID = 1L;
	
	public static HashMap<String, String> urlIDMap = new HashMap<>();
	private HashMap<String, List<Payload>> invertIndex = new HashMap<>();
	private HashMap<String, Double> tfIdf = new HashMap<>();

       

    public SearchServlet() throws JsonParseException, JsonMappingException, IOException {
        super();
        
    		Path indexRootPath = Paths.get("/Users/zuozhiw/eclipse-workspace/SearchEngine/WEBPAGES_CLEAN").toRealPath();
    		System.out.println("index root path is: " + indexRootPath);
    	
    	
    		System.out.println("reading index files");
    		
    		urlIDMap = new ObjectMapper().readValue(
    				indexRootPath.resolve("bookkeeping.json").toFile(), new TypeReference<HashMap<String, String>>(){});

    		invertIndex = new ObjectMapper().readValue(
    				indexRootPath.resolve("index_file.json").toFile(), 
    				new TypeReference<HashMap<String, List<Payload>>>(){});
    		
    		tfIdf = new ObjectMapper().readValue(
    				indexRootPath.resolve("tf_idf_file.json").toFile(), 
    				new TypeReference<HashMap<String, Double>>(){});
    		
    		System.out.println(invertIndex.get("conference"));
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String query = request.getParameter("query");
		if (query == null || query.trim().isEmpty()) {
			response.getWriter().write(new ObjectMapper().writeValueAsString(new ArrayList<>()));
			return;
		}
		
		SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		query = query.toLowerCase();
		List<String> tokens = Arrays.asList(tokenizer.tokenize(query));
		
		
		
		List<List<String>> tokenDocIDs = new ArrayList<>();
		
		for (String t: tokens) {
			String token = t.trim().toLowerCase();
			System.out.println(token);
			
			if (token.isEmpty()) {
				continue;
			}
			if (! invertIndex.containsKey(token)) {
				continue;
			}
			
			List<String> docIDs = invertIndex.get(token).stream()
					.map(p -> p.docID)
					.collect(Collectors.toList());
			tokenDocIDs.add(docIDs);
		}
		
		System.out.println(tokenDocIDs);

		if (tokenDocIDs.isEmpty()) {
			response.getWriter().write(new ObjectMapper().writeValueAsString(new ArrayList<>()));
			return;
		}
		
		List<String> finalIDs = tokenDocIDs.get(0);
		for (int i = 1; i < tokenDocIDs.size(); i++) {
			finalIDs.retainAll(tokenDocIDs.get(i));
		}
		
		Set<ID_URL> finalResults = finalIDs.stream()
				.map(id -> new ID_URL(id, urlIDMap.get(id)))
				.collect(Collectors.toSet());
		
		response.getWriter().write(new ObjectMapper().writeValueAsString(finalResults));
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
