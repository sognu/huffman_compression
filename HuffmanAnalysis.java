package assignHuffman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * @author Chad Miller
 *
 *  Class tests performance of HuffmanTree compression
 */

public class HuffmanAnalysis {
	
    //uniform distribution of characters
	private static final int UNIFORM = 0;
	//non-uniform distribution of characters
	private static final int NONUNIFORM = 1;
	
	
	public static void main(String[] args) {
		HuffmanCompressionRatio(UNIFORM);
		HuffmanCompressionRatio(NONUNIFORM);
	}
	/**
	 *   Runs compression ratio for a file increasing by size 'n' characters -- 
	 *   includes unique characters starting from 2 and increasing to 128.
	 * @param dist
	 */
	private static void HuffmanCompressionRatio(int dist) {
		
		if (dist == UNIFORM)
			System.out.println("Uniform distribution for symbols." + "\n");
		else
			System.out.println("Nonuniform distribution for symbols." + "\n");
		
		for (int i = 2; i<129; i+=21) {
			System.out.println("Compression ratio for " + i +" different characters: ");
			System.out.println("size\t" + "ratio");
		
			for (int n= 1000; n<=10000; n+=1000) {
				generateRandomFile(n, i, dist);
				CompressionDemo.compressFile("test_file.txt", "compressed.txt");
				File orig = new File("test_file.txt");
				File compressed = new File("compressed.txt");
				double ratio = ((double)compressed.length()) / orig.length();
				System.out.println(n + "\t" + ratio);
			}
			System.out.println();
		}
		System.out.println();
	}
	
	/**
	 * Generates a random file of uniformly or non-uniformly distributed characters.
	 * @param size
	 * @param numChars
	 * @param dist
	 */
	private static void generateRandomFile (int size, int numChars, int dist) {
		
		char[] chars = new char[size];
		char[] source = new char[numChars];
		
		for (int i=0; i< source.length; i++) {
			source[i] = (char)(('a' + i)%128);
		}
		
		Random rng = new Random();
		
		for (int i=0; i<chars.length; i++) {
			if (dist == UNIFORM)
				chars[i] = source[ rng.nextInt(source.length) ];//generate an array of uniform characters.
			else {
				int index = ( (int) Math.abs( rng.nextGaussian()*(source.length/6) ) ) //controls distribution
						%source.length;
				chars[i] = source[index]; //generate an array of non-uniform characters.
			}
		}

		try {
			FileOutputStream out = new FileOutputStream(new File("test_file.txt"));
			for (char c : chars)
				out.write(c);
		} catch (IOException e) {
			System.err.println(e);
		}
		
	}

}
