import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

import opennlp.tools.tokenize.SimpleTokenizer;

public class BuildIndex {
	
	
	public static HashMap<String, String> urlIDMap = new HashMap<>();
	
	public static HashMap<String, String> contentIDMap = new HashMap<>();
	
	public static HashMap<String, PriorityQueue<Payload>> invertIndex = new HashMap<>();
	public static HashMap<String, Integer> tokenFrequency = new HashMap<>();
	public static HashMap<String, HashSet<String>> tokenInDocs = new HashMap<>();

	
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("reading files");
		readFiles(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN"));
		
		System.out.println("building in memory index");
		populateInvertIndex();
		
		System.out.println("write index to file");
		writeIndexToFile(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN").resolve("index_file.json"));
		
		System.out.println("write to tfidf file");
		writeTfIdfFile(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN").resolve("tf_idf_file.json"));
		
		System.out.println("done");
	}
	
	
	/**
	 * Read the HTML files from the WEBPAGES_CLEAN folder
	 * 	according to the bookkeeping.json file, 
	 *  and populate the urlIDMap and contentIDMap
	 * 
	 * @param rootPath, the path of WEBPAGES_CLEAN folder
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void readFiles(Path rootPath) throws IOException {
		// read the bookkeeping json into the urlIDMap
		urlIDMap = new ObjectMapper().readValue(rootPath.resolve("bookkeeping.json").toFile(), HashMap.class);
		
		// read the actual files and populate the contentIDMap
		int counter = 0;
		
		for (String key : urlIDMap.keySet()) {
			String content = new String(Files.readAllBytes(rootPath.resolve(key)));
			contentIDMap.put(key, content);
			
//			if (counter > 50) {
//				break;
//			}
			counter++;
		}
	}
	
	/**
	 * Populate the in memory invert index.
	 * 
	 * Iterate through all the documents, tokenize each document into tokens 
	 *   and then add doc ID and position into the Priority Queue, 
	 *   so that the payload is sorted by docID first then position
	 */
	public static void populateInvertIndex() {
		
		for (String docID : contentIDMap.keySet()) {
			
			String content = contentIDMap.get(docID);
			SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
			List<String> tokens = Arrays.asList(tokenizer.tokenize(content));
			
			for (int pos = 0; pos < tokens.size(); pos++) {
				String token = tokens.get(pos).trim().toLowerCase();
				if (token.trim().isEmpty()) {
					continue;
				}
				if (token.length() < 3) {
					continue;
				}
				// add the term into invert index
				PriorityQueue<Payload> currentSet = invertIndex.getOrDefault(token, 
						new PriorityQueue<>((p1, p2) -> p1.compareTo(p2)));
				currentSet.add(new Payload(docID, pos));
				invertIndex.put(token, currentSet);
				
				// add frequency
				tokenFrequency.put(token, tokenFrequency.getOrDefault(token, 0) + 1);
				HashSet<String> docSet = tokenInDocs.getOrDefault(token, new HashSet<>());
				docSet.add(docID);
				tokenInDocs.put(token, docSet);
			}
			
		}
			
	}
	
	public static void writeIndexToFile(Path indexFilePath) throws IOException {
		Files.deleteIfExists(indexFilePath);
		Files.createFile(indexFilePath);
		
		new ObjectMapper().writeValue(indexFilePath.toFile(), invertIndex);
	}
	
	
	public static void writeTfIdfFile(Path tfIdfFilePath) throws IOException {
		// calculate tf-idf
		HashMap<String, Double> tfIdfMap = new HashMap<>();
		
		for (String token : tokenFrequency.keySet()) {
			// tf-idf = tf * idf = (1 + log(freq(t)) * log (1 + N / N_t)
			
			Integer tf = tokenFrequency.get(token);			
			Double idf = new Double(urlIDMap.size()) / new Double(tokenInDocs.get(token).size());
			
			Double tfIdf = (1 + Math.log(tf)) * Math.log(1 + idf);
			
			tfIdfMap.put(token, tfIdf);
		}
		
		Files.deleteIfExists(tfIdfFilePath);
		Files.createFile(tfIdfFilePath);
		
		new ObjectMapper().writeValue(tfIdfFilePath.toFile(), tfIdfMap);

	}
	
	
}
