import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import opennlp.tools.tokenize.SimpleTokenizer;

public class BuildIndex {
	
	
	public static HashMap<String, String> urlIDMap = new HashMap<>();
	
	public static HashMap<String, String> contentIDMap = new HashMap<>();
	
//	public static HashMap<String, PriorityQueue<Payload>> invertIndex = new HashMap<>();
	public static HashMap<String, HashSet<String>> invertIndex = new HashMap<>();

	public static HashMap<String, Set<String>> titleInvertIndex = new HashMap<>();
	
	public static HashMap<String, HashMap<String, Integer>> termFrequencyEachDoc = new HashMap<>();
	public static HashMap<String, Integer> documentFrequency = new HashMap<>();

	
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("reading files");
		readFiles(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN"));
		
		System.out.println("building in memory index");
		populateInvertIndex();
		
		System.out.println("write index to file");
		writeIndexToFile(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN").resolve("index_file.json"));
		
		System.out.println("write title index to file");
		writeTitleIndexToFile(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN").resolve("title_file.json"));
		
		System.out.println("write to tf-idf file");
		writeTfIdfFile(Paths.get(".").toRealPath().resolve("WEBPAGES_CLEAN").resolve("tf_idf_file.json"));
		
		System.out.println("done");
		
		// Print out statics
		System.out.println("# of documents: " + urlIDMap.size());
		System.out.println("# of unique words: " + invertIndex.size());
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
			
//			if (counter > 2) {
//				break;
//			}
			
			counter++;
		}
	}
	
	public static HashMap<Integer, String> tokenizeText(String text) {
		HashMap<Integer, String> result = new HashMap<>();
		
		if (text == null || text.trim().isEmpty()) {
			return result;
		}
		
		SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		List<String> tokens = Arrays.asList(tokenizer.tokenize(text));
		
		for (int pos = 0; pos < tokens.size(); pos++) {
			String token = tokens.get(pos).trim().toLowerCase();

			if (token.isEmpty() || token.length() < 3) {
				continue;
			}
			
			result.put(pos, token);
		}
		return result;
	}
	
	public static Set<String> findTitleTokens(HashMap<Integer, String> document) {
		List<Integer> bodyPositions = document.keySet().stream()
			.filter(pos -> document.get(pos).equalsIgnoreCase("body"))
			.sorted()
			.collect(Collectors.toList());
				
		HashSet<String> resultTokens = new HashSet<>();
		
		if (bodyPositions.size() < 2) {
			return resultTokens;
		}
		
		Integer firstBodyPos = bodyPositions.get(0);
				
		for (int i = 0; i < firstBodyPos; i++) {
			if (document.containsKey(i)) {
				resultTokens.add(document.get(i));
			}
		}
		
		return resultTokens;
	}
	
	public static HashMap<String, Integer> calculateTermFrequency(Iterable<String> docVector) {
		HashMap<String, Integer> tf = new HashMap<>();
		docVector.forEach(term -> tf.put(term, tf.getOrDefault(term, 0) + 1));
		return tf;
	}
	
	public static double calculateTfIdf(int termFreq, int corpusSize, int docFreq) {
		// tf-idf = tf * idf = (1 + log(freq(t)) * log (1 + N / N_t)
		return (1 + Math.log(termFreq)) * Math.log(1 + (new Double(corpusSize) / new Double(docFreq)));
	}
	
	public static HashMap<String, Double> getDocTfIdf(HashMap<String, Integer> docTermFreq) {
		HashMap<String, Double> tfIdfMap = new HashMap<>();
		docTermFreq.forEach((term, termFreq) -> tfIdfMap.put(
				term, calculateTfIdf(termFreq, urlIDMap.size(), documentFrequency.get(term))));
		return tfIdfMap;
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
			
			// tokenize text into a hashmap of position -> token
			// all token are in lowercase
			HashMap<Integer, String> positionTokenMap = tokenizeText(contentIDMap.get(docID));
			
			// add the payload(docID and position) to the invert index
//			positionTokenMap.forEach((pos, token) -> {
//				if (!invertIndex.containsKey(token)) 
//					invertIndex.put(token, new PriorityQueue<>((p1, p2) -> p1.compareTo(p2)));
//				invertIndex.get(token).add(new Payload(docID, pos));
//				});
			
			positionTokenMap.forEach((pos, token) -> {
				if (!invertIndex.containsKey(token)) 
					invertIndex.put(token, new HashSet<String>());
				invertIndex.get(token).add(docID);
				});
			
			// add the title invert index
			Set<String> titleTokens = findTitleTokens(positionTokenMap);
			for (String t: titleTokens) {
				if (! titleInvertIndex.containsKey(t)) {
					if (t == null) {
					}
					titleInvertIndex.put(t, new HashSet<String>());
				}
				titleInvertIndex.get(t).add(docID);
			}
			
			
			// calculate the term frequency and put it to the term frequency map
			termFrequencyEachDoc.put(docID, calculateTermFrequency(positionTokenMap.values()));
			
			// convert the document to a set and update the document frequency map
			new HashSet<>(positionTokenMap.values()).forEach(
					token -> documentFrequency.put(token, documentFrequency.getOrDefault(token, 0) + 1));
						
		}
			
	}
	
	public static void writeIndexToFile(Path indexFilePath) throws IOException {
		Files.deleteIfExists(indexFilePath);
		Files.createFile(indexFilePath);
		
		new ObjectMapper().writeValue(indexFilePath.toFile(), invertIndex);
	}

	public static void writeTitleIndexToFile(Path indexFilePath) throws IOException {
		Files.deleteIfExists(indexFilePath);
		Files.createFile(indexFilePath);
				
		new ObjectMapper().writeValue(indexFilePath.toFile(), titleInvertIndex);
	}
		
	
	public static void writeTfIdfFile(Path tfIdfFilePath) throws IOException {
		// calculate tf-idf
		HashMap<String, HashMap<String, Double>> tfIdfMapEachDoc = new HashMap<>();
		
		termFrequencyEachDoc.forEach((docID, docTf) -> tfIdfMapEachDoc.put(docID, getDocTfIdf(docTf)));
		
		Files.deleteIfExists(tfIdfFilePath);
		Files.createFile(tfIdfFilePath);
		
		new ObjectMapper().writeValue(tfIdfFilePath.toFile(), tfIdfMapEachDoc);

	}
	
	
}
