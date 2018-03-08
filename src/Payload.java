import com.fasterxml.jackson.annotation.JsonProperty;

public class Payload implements Comparable<Payload> {
	
	@JsonProperty("d")
	public String docID;
	@JsonProperty("p")
	public Integer position;
	
	public Payload() {}
	
	public Payload(String docID, Integer position) {
		this.docID = docID;
		this.position = position;
	}
	
	@Override
	public String toString() {
		return "<docID: " + docID + ", position: " + position + ">";
	}

	@Override
	public int compareTo(Payload o) {
		if (this.docID != o.docID) {
			return this.docID.compareTo(o.docID);
		}
		return this.position.compareTo(o.position);
	}

}
