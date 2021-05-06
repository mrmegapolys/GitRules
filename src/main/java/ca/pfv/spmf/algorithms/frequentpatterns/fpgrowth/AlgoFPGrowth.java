package ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth;
 
 /* This file is copyright (c) 2008-2015 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */


 import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
 import ca.pfv.spmf.tools.MemoryLogger;
 import com.megapolys.gitrules.Commit;
 import com.megapolys.gitrules.spmf.Itemset;
 import org.jetbrains.annotations.NotNull;

 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 /**
  * This is an implementation of the FPGROWTH algorithm (Han et al., 2004).
  * FPGrowth is described here:
  * <br/><br/>
  *
  * Han, J., Pei, J., & Yin, Y. (2000, May). Mining frequent patterns without candidate generation. In ACM SIGMOD Record (Vol. 29, No. 2, pp. 1-12). ACM
  * <br/><br/>
  *
  * This is an optimized version that saves the result to a file
  * or keep it into memory if no output path is provided
  * by the user to the runAlgorithm method().
  *
  * @see FPTree
  * @see Itemset
  * @see Itemsets
  * @author Philippe Fournier-Viger
  */
 public class AlgoFPGrowth {

     // for statistics
     private long startTimestamp; // start time of the latest execution
     private long endTime; // end time of the latest execution
     private int transactionCount = 0; // transaction count in the database
     private int itemsetCount; // number of freq. itemsets found

     // parameter
     public int minSupport;// the relative minimum support

     // The  patterns that are found
     // (if the user wants to keep them into memory)
     protected Itemsets patterns = null;

     // This variable is used to determine the size of buffers to store itemsets.
     // A value of 50 is enough because it allows up to 2^50 patterns!
     final int BUFFERS_SIZE = 2000;

     // buffer for storing the current itemset that is mined when performing mining
     // the idea is to always reuse the same buffer to reduce memory usage.
     private String[] itemsetBuffer = null;
     // another buffer for storing fpnodes in a single path of the tree
     private FPNode[] fpNodeTempBuffer = null;

     /** maximum pattern length */
     private final int maxPatternLength = 1000;

     /** minimum pattern length */
     private final int minPatternLength = 0;



     /**
      * Constructor
      */
     public AlgoFPGrowth() {

     }

     /**
      * Method to run the FPGRowth algorithm.
      * @param minSupp the minimum support threshold.
      * @return the result if no output file path is provided.
      */
     public Itemsets runAlgorithm(Collection<Commit> commits, int minSupp) {
         // record start time
         startTimestamp = System.currentTimeMillis();
         // number of itemsets found
         itemsetCount = 0;

         //initialize tool to record memory usage
         MemoryLogger.getInstance().reset();
         MemoryLogger.getInstance().checkMemory();

         // if the user want to keep the result into memory
         patterns =  new Itemsets();

         // (1) PREPROCESSING: Initial database scan to determine the frequency of each item
         // The frequency is stored in a map:
         //    key: item   value: support
         final Map<String, Integer> mapSupport = scanDatabaseToDetermineFrequencyOfSingleItems(commits);

         // convert the minimum support as percentage to a
         // relative minimum support
         this.minSupport = minSupp;

         // (2) Scan the database again to build the initial FP-Tree
         // Before inserting a transaction in the FPTree, we sort the items
         // by descending order of support.  We ignore items that
         // do not have the minimum support.
         FPTree tree = getFpTree(commits, mapSupport);

         // We create the header table for the tree using the calculated support of single items
         tree.createHeaderList(mapSupport);

         // (5) We start to mine the FP-Tree by calling the recursive method.
         // Initially, the prefix alpha is empty.
         // if at least an item is frequent
         if(tree.headerList.size() > 0) {
             // initialize the buffer for storing the current itemset
             itemsetBuffer = new String[BUFFERS_SIZE];
             // and another buffer
             fpNodeTempBuffer = new FPNode[BUFFERS_SIZE];
             // recursively generate frequent itemsets using the fp-tree
             // Note: we assume that the initial FP-Tree has more than one path
             // which should generally be the case.
             fpgrowth(tree, itemsetBuffer, 0, transactionCount, mapSupport);
         }

         // record the execution end time
         endTime= System.currentTimeMillis();

         // check the memory usage
         MemoryLogger.getInstance().checkMemory();

         // return the result (if saved to memory)
         return patterns;
     }

     @NotNull
     private FPTree getFpTree(Collection<Commit> commits, Map<String, Integer> mapSupport) {
         FPTree tree = new FPTree();

         for (Commit commit : commits) {
             List<String> transaction = new ArrayList<>();

             // for each item in the transaction
             for(String item : commit.getFiles()){
                 if(mapSupport.get(item) >= minSupport){
                     transaction.add(item);
                 }
             }
             // sort item in the transaction by descending order of support
             transaction.sort((item1, item2) -> {
                 // compare the frequency
                 int compare = mapSupport.get(item2) - mapSupport.get(item1);
                 // if the same frequency, we check the lexical ordering!
                 if (compare == 0) {
                     return item1.compareTo(item2);
                 }
                 // otherwise, just use the frequency
                 return compare;
             });
             // add the sorted transaction to the fptree.
             tree.addTransaction(transaction);
         }
         return tree;
     }


     /**
      * Mine an FP-Tree having more than one path.
      * @param tree  the FP-tree
      * @param prefix  the current prefix, named "alpha"
      * @param mapSupport the frequency of items in the FP-Tree
      */
     private void fpgrowth(FPTree tree, String[] prefix, int prefixLength, int prefixSupport, Map<String, Integer> mapSupport) {
         if(prefixLength == maxPatternLength){
             return;
         }

         // We will check if the FPtree contains a single path
         boolean singlePath = true;
         // This variable is used to count the number of items in the single path
         // if there is one
         int position = 0;
         // if the root has more than one child, than it is not a single path
         if(tree.root.children.size() > 1) {
             singlePath = false;
         }else {

             // Otherwise,
             // if the root has exactly one child, we need to recursively check childs
             // of the child to see if they also have one child
             FPNode currentNode = tree.root.children.get(0);
             while(true){
                 // if the current child has more than one child, it is not a single path!
                 if(currentNode.children.size() > 1) {
                     singlePath = false;
                     break;
                 }
                 // otherwise, we copy the current item in the buffer and move to the child
                 // the buffer will be used to store all items in the path
                 fpNodeTempBuffer[position] = currentNode;

                 position++;
                 // if this node has no child, that means that this is the end of this path
                 // and it is a single path, so we break
                 if(currentNode.children.size() == 0) {
                     break;
                 }
                 currentNode = currentNode.children.get(0);
             }
         }

         // Case 1: the FPtree contains a single path
         if(singlePath){
             // We save the path, because it is a maximal itemset
             saveAllCombinationsOfPrefixPath(fpNodeTempBuffer, position, prefix, prefixLength);
         }else {
             // For each frequent item in the header table list of the tree in reverse order.
             for(int i = tree.headerList.size()-1; i>=0; i--){
                 // get the item
                 String item = tree.headerList.get(i);

                 // get the item support
                 int support = mapSupport.get(item);

                 // Create Beta by concatening prefix Alpha by adding the current item to alpha
                 prefix[prefixLength] = item;

                 // calculate the support of the new prefix beta
                 int betaSupport = Math.min(prefixSupport, support);

                 // save beta to the output file
                 saveItemset(prefix, prefixLength+1, betaSupport);

                 if(prefixLength+1 < maxPatternLength){

                     // === (A) Construct beta's conditional pattern base ===
                     // It is a subdatabase which consists of the set of prefix paths
                     // in the FP-tree co-occuring with the prefix pattern.
                     List<List<FPNode>> prefixPaths = new ArrayList<>();
                     FPNode path = tree.mapItemNodes.get(item);

                     // Map to count the support of items in the conditional prefix tree
                     // Key: item   Value: support
                     Map<String, Integer> mapSupportBeta = new HashMap<>();

                     while(path != null){
                         // if the path is not just the root node
                         if(path.parent.itemID != null){
                             // create the prefixpath
                             List<FPNode> prefixPath = new ArrayList<>();
                             // add this node.
                             prefixPath.add(path);   // NOTE: we add it just to keep its support,
                             // actually it should not be part of the prefixPath

                             // ####
                             int pathCount = path.counter;

                             //Recursively add all the parents of this node.
                             FPNode parent = path.parent;
                             while(parent.itemID != null){
                                 prefixPath.add(parent);

                                 // FOR EACH PATTERN WE ALSO UPDATE THE ITEM SUPPORT AT THE SAME TIME
                                 // if the first time we see that node id
                                 if(mapSupportBeta.get(parent.itemID) == null){
                                     // just add the path count
                                     mapSupportBeta.put(parent.itemID, pathCount);
                                 }else{
                                     // otherwise, make the sum with the value already stored
                                     mapSupportBeta.put(parent.itemID, mapSupportBeta.get(parent.itemID) + pathCount);
                                 }
                                 parent = parent.parent;
                             }
                             // add the path to the list of prefixpaths
                             prefixPaths.add(prefixPath);
                         }
                         // We will look for the next prefixpath
                         path = path.nodeLink;
                     }

                     // (B) Construct beta's conditional FP-Tree
                     // Create the tree.
                     FPTree treeBeta = new FPTree();
                     // Add each prefixpath in the FP-tree.
                     for(List<FPNode> prefixPath : prefixPaths){
                         treeBeta.addPrefixPath(prefixPath, mapSupportBeta, minSupport);
                     }

                     // Mine recursively the Beta tree if the root has child(s)
                     if(treeBeta.root.children.size() > 0){

                         // Create the header list.
                         treeBeta.createHeaderList(mapSupportBeta);
                         // recursive call
                         fpgrowth(treeBeta, prefix, prefixLength+1, betaSupport, mapSupportBeta);
                     }
                 }
             }
         }

     }


     /**
      * This method saves all combinations of a prefix path if it has enough support
      * @param prefix the current prefix
      * @param prefixLength the current prefix length
      */
     private void saveAllCombinationsOfPrefixPath(FPNode[] fpNodeTempBuffer, int position,
             String[] prefix, int prefixLength) {

         int support = 0;
         // Generate all subsets of the prefixPath except the empty set
         // and output them
         // We use bits to generate all subsets.
 loop1:	for (long i = 1, max = 1L << position; i < max; i++) {

             // we create a new subset
             int newPrefixLength = prefixLength;

             // for each bit
             for (int j = 0; j < position; j++) {
                 // check if the j bit is set to 1
                 int isSet = (int) i & (1 << j);
                 // if yes, add the bit position as an item to the new subset
                 if (isSet > 0) {
                     if(newPrefixLength == maxPatternLength){
                         continue loop1;
                     }

                     prefix[newPrefixLength++] = fpNodeTempBuffer[j].itemID;
                     // 2018-03-18: REMOVED THE FOLLOWING "IF" to fix
                     // support counting error.
 //					if(support == 0) {
                         support = fpNodeTempBuffer[j].counter;
 //					}
                 }
             }
             // save the itemset
             saveItemset(prefix, newPrefixLength, support);
         }
     }


     /**
      * This method scans the input database to calculate the support of single items
      * @return a map for storing the support of each item (key: item, value: support)
      */
     private Map<String, Integer> scanDatabaseToDetermineFrequencyOfSingleItems(Collection<Commit> commits) {
         Map<String, Integer> mapSupport = new HashMap<>();

         for (Commit commit : commits) {
             for (String file : commit.getFiles()) {
                 Integer count = mapSupport.get(file);
                 if(count == null){
                     mapSupport.put(file, 1);
                 }else{
                     mapSupport.put(file, ++count);
                 }
             }
             transactionCount++;
         }
         return mapSupport;
     }


     /**
      * Write a frequent itemset that is found to the output file or
      * keep into memory if the user prefer that the result be saved into memory.
      */
     private void saveItemset(String [] itemset, int itemsetLength, int support) {
         if(itemsetLength < minPatternLength) {
             return;
         }

         // increase the number of itemsets found for statistics purpose
         itemsetCount++;

         // if the result should be saved to a file
         // create an object Itemset and add it to the set of patterns
         // found.
         String[] itemsetArray = new String[itemsetLength];
         System.arraycopy(itemset, 0, itemsetArray, 0, itemsetLength);

         // sort the itemset so that it is sorted according to lexical ordering before we show it to the user
         Arrays.sort(itemsetArray);

         Itemset itemsetObj = new Itemset(Arrays.asList(itemsetArray), support);
         patterns.addItemset(itemsetObj, itemsetLength);
     }

     /**
      * Print statistics about the algorithm execution to System.out.
      */
     public void printStats() {
         System.out.println("=============  FP-GROWTH 2.42 - STATS =============");
         long temps = endTime - startTimestamp;
         System.out.println(" Transactions count from database : " + transactionCount);
         System.out.print(" Max memory usage: " + MemoryLogger.getInstance().getMaxMemory() + " mb \n");
         System.out.println(" Frequent itemsets count : " + itemsetCount);
         System.out.println(" Total time ~ " + temps + " ms");
         System.out.println("===================================================");
     }
 }