package CLHMiner;

import java.io.*;

public class Main {
	public static void main(String[] args) throws IOException {

		String Taxonomy = "E:\\NCKH\\3.Code\\CLH-Miner\\CLHMiner\\taxonomy_CLHMiner.txt";
		String input = "E:\\NCKH\\3.Code\\CLH-Miner\\CLHMiner\\input.txt";
		String output = "E:\\NCKH\\3.Code\\CLH-Miner\\CLHMiner\\output.txt";
		
		int minimumUtility = 40;
		
		AlgoCLHMiner cl = new AlgoCLHMiner();
		
		cl.runAlgorithm(minimumUtility, input, output, Taxonomy);
		// cl.saveResultsToFile(output);
		// cl.saveTaxonomyToFile(Taxonomy);
		cl.printStats();
	}

}
