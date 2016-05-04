package edu.uw.feedlack;

public final class OutputExplanation {

	private final Likelihood likelihood;
	private final String name;
	private final String reason;
	
	public OutputExplanation(Likelihood likelihood, String name, String reason) {
		
		this.likelihood = likelihood;
		this.name = name;
		this.reason = reason;
		
	}
	
	public Likelihood getLikelihood() { return likelihood; }
	public String getName() { return name; }
	public String getReason() { return reason; }
	
}
