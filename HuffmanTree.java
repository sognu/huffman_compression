package assignHuffman;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 *  @author Chad Miller
 * 
 * Implements file compression and decompression using Huffman's algorithm,
 * which encodes and decodes each character of a file using a binary trie.
 *
 */
public class HuffmanTree {

	/**
	 * Represents a single node of a Huffman Tree.
	 */
	private class Node implements Comparable<Node> {

		public int symbol; // character to be encoded/decoded

		public int weight; // number of occurrences of the character

		public Node leftChild, rightChild, parent;

		/**
		 * Constructs a leaf node.
		 */
		public Node(int _symbol, int _weight) {
			symbol = _symbol;
			weight = _weight;
			leftChild = rightChild = parent = null;
		}

		/**
		 * Constructs an internal node. Note that an internal node has a weight (sum
		 * of the weights of its children) but no character.
		 * 
		 * @param left
		 *          - left child of the new node
		 * @param right
		 *          - right child of the new node
		 */
		public Node(Node left, Node right) {
			symbol = INCOMPLETE_CODE;
			weight = left.weight + right.weight;
			leftChild = left;
			rightChild = right;
			parent = null;
		}

		/**
		 * Returns a string containing all of the edges in the tree rooted at "this"
		 * node, in DOT format.
		 */
		public String generateDot() {
			String atNode = Character.toString((char) symbol);
			if (symbol == INCOMPLETE_CODE)
				atNode = " ";
			else if (symbol == EOF)
				atNode = "EOF";
			else if (symbol == '\n')
				atNode = "newline";
			else if (symbol == '\t')
				atNode = "tab";
			else if (symbol == ' ')
				atNode = "space";
			atNode += " " + weight;
			String ret = "\tnode" + hashCode() + " [label = \"<f0> |<f1> " + atNode
					+ "|<f2> \"]\n";
			if (leftChild != null)
				ret += "\tnode" + hashCode() + ":f0 -- node" + leftChild.hashCode()
				+ ":f1\n" + leftChild.generateDot();
			if (rightChild != null)
				ret += "\tnode" + hashCode() + ":f2 -- node" + rightChild.hashCode()
				+ ":f1\n" + rightChild.generateDot();

			return ret;
		}

		/**
		 * Compares two Huffman nodes, using weight.
		 * 
		 * @param rhs
		 *          - right-hand side node
		 * @return a value > 0 if this node is larger than rhs, a value < 0 if this
		 *         node is smaller than rhs, 0 if the nodes are equal
		 */
		public int compareTo(Node rhs) {
			int d = weight - rhs.weight;

			if (d != 0) return d;
			else 
				return leftMost().symbol - rhs.leftMost().symbol;
		}

		private Node leftMost() {

			Node leftMost = this;

			while(leftMost.leftChild != null){
				leftMost = leftMost.leftChild;
			}
			return leftMost;
		}
	}

	private Node root; // root of the Huffman tree

	private Map<Integer, Node> symbols; // set of characters in the file mapped to
	// their corresponding Huffman nodes

	private List<Integer> data; // list of characters in the file

	private final static int ERROR = -3; // used to detect errors in Huffman
	// encoding

	private final static int INCOMPLETE_CODE = -2; // used to detect internal
	// Huffman nodes (i.e., not a
	// leaf node and has no
	// character)

	private final static int EOF = 0; // used to detect the end of a file
	// compressed using Huffman's algorithm

	/**
	 * Generates a compressed version of the input file.
	 * 
	 * @param infile
	 *          - input file of (uncompressed) data
	 * @param outfile
	 *          - output file of compressed data
	 */
	public void compressFile(File infile, File outfile) {
		symbols = new HashMap<Integer, Node>();
		data = new ArrayList<Integer>();

		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outfile));

			// read characters and counts frequency of each
			readData(new FileInputStream(infile));

			// build Huffman tree using frequency information
			createTree();
			
			// write character and frequencies to beginning of compressed file
			writeEncodingInfo(out);

			// for each character in the input file, encodes it using the Huffman tree
			// and writes the bit code
			BitOutputStream bout = new BitOutputStream(out);
			for (int ch : data)
				bout.writeBits(getCode(ch & 0xff));
					bout.close();

		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * Generates a decompressed version of the input file
	 * 
	 * @param infile
	 *          - input file of (compressed) data
	 * @param outfile
	 *          - output file of decompressed data
	 */
	public void decompressFile(File infile, File outfile) {
		symbols = new HashMap<Integer, Node>();
		data = new ArrayList<Integer>();

		try {
			DataInputStream in = new DataInputStream(new FileInputStream(infile));

			// read characters and frequency information
			readEncodingInfo(in);

			// build Huffman tree using frequency information
			createTree();
			
			// read Huffman codes corresponding to each character in original file
			BitInputStream bin = new BitInputStream(in);
			readCompressedData(bin);
			bin.close();

			// use the codes to find each character in the Huffman tree and print the
			// characters
			FileOutputStream out = new FileOutputStream(outfile);
			for (int i : data)
				out.write(i);
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * COMPRESSION
	 * 
	 * Reads all characters, while maintaining a count of the occurrences of each
	 * character (AKA the character's weight).
	 * 
	 * @param in
	 *          - stream for the input file
	 * @throws IOException
	 */
	private void readData(InputStream in) throws IOException {
		int ch;
		// read characters until end of file
		while ((ch = in.read()) != -1) {
			// if character had not yet been seen, put it in the tree (weight = 1)
			if (symbols.get(ch) == null)
				symbols.put(ch, new Node(ch, 1));
			// if character has already been seen, increment weight
			else
				symbols.get(ch).weight++;
			// keep track of all characters (in order)
			data.add(ch);
		}

		// add end-of-file marker to tree and list of characters
		symbols.put(EOF, new Node(EOF, 1));
		data.add(EOF);
	}

	/**
	 * DECOMPRESSION
	 * 
	 * Reads the Huffman codes corresponding to each character in original file.
	 * 
	 * @param in
	 *          - stream for the input file
	 * @throws IOException
	 */
	private void readCompressedData(BitInputStream in) throws IOException {
		String bits = "";
		int bit;
		int decode = 0;

		// read bits until end of file
		while ((bit = in.readBit()) != -1) {
			if (bit == 0)
				bits += "0";
			else
				bits += "1";

			// follow the path in the Huffman tree indicated by the bit code and get
			// the character encoded
			decode = getChar(bits);

			if (decode == INCOMPLETE_CODE) // if the path leads to an internal node,
				// not a complete code; get next bit
				continue;
			else if (decode == ERROR)
				throw new IOException("Decoding error");
			else if (decode == EOF)
				return;

			// keep track of each character decoded
			data.add(decode);
			bits = "";
		}
	}

	/**
	 * COMPRESSION
	 * 
	 * Writes the character and weight information to the output file, so that the
	 * Huffman tree can reconstructed at the time of decompression.
	 * 
	 * @param out
	 *          - stream for the output file
	 * @throws IOException
	 */
	private void writeEncodingInfo(DataOutputStream out) throws IOException {
		for (Map.Entry<Integer, Node> e : symbols.entrySet()) {
			out.writeByte(e.getKey());
			out.writeInt(e.getValue().weight);
		}

		// special code to indicate end of file
		out.writeByte(0);
		out.writeInt(0);
	}

	/**
	 * DECOMPRESSION
	 * 
	 * Reads the character and weight information at the beginning of the
	 * compressed file, so that the Huffman tree can reconstructed.
	 * 
	 * @param in
	 *          - stream for the input file
	 * @throws IOException
	 */
	private void readEncodingInfo(DataInputStream in) throws IOException {
		int ch;
		int num;
		while (true) {
			ch = in.readByte();
			num = in.readInt();
			if (num == 0) // EOF
				return;
			symbols.put(ch, new Node(ch, num));
		}
	}

	/**
	 * COMPRESSION and DECOMPRESSION
	 * 
	 * Constructs a Huffman tree to represent bit codes for each character. (See
	 * algorithm and examples in Lecture 22.)
	 */
	private void createTree() {
		PriorityQueue<Node> pq = new PriorityQueue<Node>();
		pq.addAll(symbols.values());

		while(pq.size() > 1){
			Node left = pq.poll();
			Node right = pq.poll();
			Node temp = new Node(left, right);
			left.parent = temp;
			right.parent = temp;
			pq.offer(temp);
		}
		root = pq.poll();
	}    
	// FILL IN -- use "pq" to construct a Huffman tree rooted at "root"
	// TO VISUALIZE the binary trie, use the huffmanToDot method


	/**
	 * COMPRESSION
	 * 
	 * Returns the bit code for a character, by traversing the path from the
	 * character's leaf node up to the root of the tree. Encountering a left child
	 * causes a 0 to be pre-appended to the bit code, and encountering a right
	 * child causes a 1 to be pre-appended. (See algorithm and examples in Lecture
	 * 22.)
	 * 
	 * @param ch
	 *          - character to be encoded
	 */
	private int[] getCode(int ch) {
		
		Node temp = symbols.get(ch);
		Node parent = temp;
		String code = "";
		while(temp.parent != null){
			parent = temp.parent;
			if(parent.leftChild == temp){
				code = 0 + code;
			}else {
				code = 1 + code;
			}
			temp = temp.parent;
		}
		
		// FILL IN -- do not return null
		// HINT: since you don't know the length of the bit code to start, build a
		//    String of 0s and 1s and then convert to an int array when done
		char[] bits = code.toCharArray();
		int[] result = new int[bits.length];
		for (int i=0; i<bits.length; i++) {
		    
		    result[i] = bits[i] - '0';
		
		}
		return result;
	}

	/**
	 * DECOMPRESSION
	 * 
	 * Returns the character for the bit code, by traversing the path from the
	 * root of the tree to the character's leaf node. A 0 in the code causes the
	 * path to go through a left child, and a 1 in the code causes the path to go
	 * through a right child.
	 * 
	 * @param code
	 *          - the bit code indicating the path from the root
	 * @return the character encoded or ERROR if the path is not valid
	 */
	private int getChar(String code) {
		Node curr = root;

		for (int i = 0; curr != null && i < code.length(); i++)
			if (code.charAt(i) == '0')
				curr = curr.leftChild;
			else
				curr = curr.rightChild;

		if (curr == null)
			return ERROR;

		return curr.symbol;
	}

	/**
	 * Generates a DOT file for visualizing the Huffman tree.
	 * 
	 * @param dotFilename
	 *          - filename of DOT file to generate
	 */
	public void huffmanToDot(String dotFilename) {
		try {
			PrintWriter out = new PrintWriter(dotFilename);
			out.println("graph Tree {\n\tnode [shape=record]\n");

			if (root == null)
				out.println("");
			else
				out.print(root.generateDot());

			out.println("}");
			out.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
