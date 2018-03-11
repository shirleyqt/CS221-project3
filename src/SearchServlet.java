

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private HashMap<String, HashMap<String, Double>> tfIdfEachDoc = new HashMap<>();

       

    public SearchServlet() throws Exception {
        super();
        
        loadIndex();
    
    }
    
    private void loadIndex() throws Exception {
		Path indexRootPath = Paths.get("/Users/zuozhiw/eclipse-workspace/SearchEngine/WEBPAGES_CLEAN").toRealPath();

		System.out.println("index root path is: " + indexRootPath);

		System.out.println("reading index files");
		
		urlIDMap = new ObjectMapper().readValue(
				indexRootPath.resolve("bookkeeping.json").toFile(), new TypeReference<HashMap<String, String>>(){});

		invertIndex = new ObjectMapper().readValue(
				indexRootPath.resolve("index_file.json").toFile(), 
				new TypeReference<HashMap<String, List<Payload>>>(){});
		
		tfIdfEachDoc = new ObjectMapper().readValue(
				indexRootPath.resolve("tf_idf_file.json").toFile(), 
				new TypeReference<HashMap<String, HashMap<String, Double>>>(){});
		
		System.out.println("finish loading index");
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String query = request.getParameter("query");
		if (query == null || query.trim().isEmpty()) {
			response.getWriter().write(new ObjectMapper().writeValueAsString(new HashSet<>()));
			return;
		}
		
		List<String> queryTokens = new ArrayList<>(BuildIndex.tokenizeText(query).values());
		Set<String> resultDocIDs = queryIndex(queryTokens);
		
		HashMap<String, List<Double>> tfIdfEachMatchedDoc = new HashMap<>();
		
		for (String docID: resultDocIDs) {
			List<Double> tfIdf = queryTokens.stream()
					.map(token -> tfIdfEachDoc.get(docID).get(token))
					.collect(Collectors.toList());
			tfIdfEachMatchedDoc.put(docID, tfIdf);
		}
		
		HashMap<String, Double> tfIdfQueryMap = new HashMap<>();
		BuildIndex.calculateTermFrequency(queryTokens)
			.forEach((term, termFreq) -> tfIdfQueryMap.put(
				term, BuildIndex.calculateTfIdf(termFreq, 1, 1)));
		
		List<Double> queryTfIdf = queryTokens.stream().map(token -> tfIdfQueryMap.get(token)).collect(Collectors.toList());
		
		HashMap<String, Double> docCosineSimilarityMap = new HashMap<>();
		
		tfIdfEachMatchedDoc.forEach((docID, docTfIdf) -> docCosineSimilarityMap.put(docID, cosineSimilarity(docTfIdf, queryTfIdf)));
		
		List<String> sortedDocIDs = resultDocIDs.stream()
				.sorted((d1, d2) -> docCosineSimilarityMap.get(d1).compareTo(docCosineSimilarityMap.get(d2)))
				.collect(Collectors.toList());
		
		
		List<ID_URL> finalResults = sortedDocIDs.stream()
				.map(id -> new ID_URL(id, urlIDMap.get(id)))
				.collect(Collectors.toList());
		
		response.getWriter().write(new ObjectMapper().writeValueAsString(finalResults));
		
	}
	
	public static double cosineSimilarity(List<Double> docTfIdf, List<Double> queryTfIdf) {
		
		// calculate dot product of document TF-IDF and query TF-IDF 
		double dotProduct = IntStream.range(0, queryTfIdf.size())
			.mapToDouble(i -> docTfIdf.get(i) * queryTfIdf.get(i))
			.reduce(0.0, Double::sum);
		
		// calculate norm product: ||Doc|| * ||Query||
		//   where ||Doc|| = sqrt(a^2 + b^2 + c^2 ...)
		double normProduct = 
				Math.sqrt(docTfIdf.stream().map(d -> d * d).reduce(0.0, Double::sum)) *
				Math.sqrt(queryTfIdf.stream().map(d -> d * d).reduce(0.0, Double::sum));
		
		return dotProduct / normProduct;
	}
	
	private Set<String> queryIndex(Collection<String> queryTokens) {
		
		Set<String> resultDocIDs = queryTokens.stream()
			// get the invert index payload list for each token
			.map(token -> invertIndex.getOrDefault(token, new ArrayList<>()))
			// convert payload list to a set of doc ID for each token
			.map(list -> list.stream().map(payload -> payload.docID).collect(Collectors.toSet()))
			// intersection of all sets
			.reduce((l1, l2) -> new HashSet<>(CollectionUtils.intersection(l1, l2)))
			.orElse(new HashSet<>());
		
		return resultDocIDs;
	}
	
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
