package assignHuffman;

import java.io.File;

/**
 * @author Chad Miller
 * 
 * Class tests HuffmanTree through compression and decompression of file.
 * 
 */

public class CompressionDemo {

  //compress file.
  public static void compressFile(String infile, String outfile) {
    HuffmanTree t = new HuffmanTree();
    t.compressFile(new File(infile), new File(outfile));  
    t.huffmanToDot("huffman_tree.dot");
  }
  //decompress file.
  public static void decompressFile(String infile, String outfile) {
    HuffmanTree t = new HuffmanTree();
    t.decompressFile(new File(infile), new File(outfile));
  }
  //run tests through main.
  public static void main(String[] args) {
    compressFile("A9_example/original.txt", "compressed.txt");  
    decompressFile("compressed.txt", "decompressed.txt");
  }
}
