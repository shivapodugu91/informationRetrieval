

import java.io.*;
import java.util.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class IR2 
{
	static int FilesCount=0; 
	static TreeMap<String, Integer>term_df ;
	static TreeMap<Integer, Integer>docLength= new TreeMap<Integer, Integer>();
	static TreeMap<Integer, Integer>max_tf= new TreeMap<Integer, Integer>();
	static TreeMap<Integer, Integer>max_tf2= new TreeMap<Integer, Integer>();
    static TreeMap<String, TreeMap<Integer, Integer>> indexVersion1= new TreeMap<String, TreeMap<Integer, Integer>>();
    static TreeMap<String, TreeMap<Integer, Integer>> indexVersion2= new TreeMap<String, TreeMap<Integer, Integer>>();
    static TreeMap<String, LinkedHashMap<Short, Short>> cmp_Index_version1= new TreeMap<String, LinkedHashMap<Short, Short>>();
    static TreeMap<String, LinkedHashMap<Short, Short>> cmp_Index_version2= new TreeMap<String, LinkedHashMap<Short, Short>>();
    static TreeMap<String, Integer> term_frequency= new TreeMap<String, Integer>();
    static LinkedHashMap<String, List<Object>> Index_BlockVersion1= new LinkedHashMap<String, List<Object>>();
    static LinkedHashMap<String, List<Object>> Index_FrontCoding= new LinkedHashMap<String, List<Object>>();
    static File uncompressedIndexFile, compressedIndexFile;
    static double timeTakenVersion1=0,timeTakenVersion2=0;
    public static StanfordCoreNLP pipeline;
  
    public static  void  StanfordLemmatizer() 
    {
          Properties props;
          props = new Properties();
          props.put("annotators", "tokenize, ssplit, pos, lemma");
          pipeline = new StanfordCoreNLP(props);
    }
   
    public static String lemmatize(String documentText)
    {  
        List<String> lemmas_ = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        pipeline.annotate(document);
        // Iterate over all of the sentences found
        String temp="";
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) 
        {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
            	temp=token.get(LemmaAnnotation.class);
            	    lemmas_.add(temp);
            }
        }
        return lemmas_.get(0).toString();
    }
 
    static void scanFiles(String directoryPath,String stopwordsPath) throws IOException 
    {
        File file = new File(directoryPath);
        File listOfFiles[] = file.listFiles();
        Arrays.sort(listOfFiles);
        FilesCount=listOfFiles.length;
        int documents[][] = new int[FilesCount+1][2];
        TreeMap<String, String> stopwords= fetch_Stopwords(stopwordsPath);
        System.out.println("Version 1 \nCreating index");
        long startTimeVersion1= System.currentTimeMillis();
        HashMap<String, Integer> lemmas,lemmas_without_stopwords;
        lemmas_without_stopwords=null;
        for(int j = 0; j < listOfFiles.length; j++)
        {
        	lemmas = new HashMap<String, Integer>();
        	int docID=0,doclenth=0;
        	File f = listOfFiles[j];
        	if(f.getName().contains("cranfield"))
        	{
        	String str=f.getName();
        	str= str.replaceAll("[^0-9]", "");
        	str=str.replaceFirst("^0+(?!$)", "");
             docID=Integer.parseInt(str);
        	String content=parseFiles(f);
        	StringTokenizer st = new StringTokenizer(content," ");
    		while (st.hasMoreElements()) 
    		{  doclenth++;
    			String lemma="";
    			String word=st.nextElement().toString();
    			lemma=lemmatize(word);
				if (lemmas.containsKey(lemma)!= true)
					lemmas.put(lemma, 1);
                else
                	lemmas.put(word,lemmas.get(lemma)+1);  
				
    		}
            lemmas_without_stopwords= removeStopwords(lemmas, stopwords);
            createIndex(docID, lemmas_without_stopwords, indexVersion1);
        }
        	docLength.put(docID,doclenth);
        	int maxValueInMap=(Collections.max(lemmas_without_stopwords.values())); 
        	max_tf.put(docID,maxValueInMap);
    }
        System.out.println("Creating uncompressed index..");
        writeUncompressedIndex("Index_Version1.uncompressed", indexVersion1);
        System.out.println("Created..\nCompressing index..");
        blockCompression(indexVersion1);
        timeTakenVersion1= (System.currentTimeMillis() - startTimeVersion1)/1000;
        System.out.println("Created..\nWriting compressed index..");
        writeCompressedIndex("Index_Version1.compressed", Index_BlockVersion1);
        System.out.println("Created..");
        System.out.println("-------------------------------------");
        System.out.println("Version 2");
        System.out.println("Creating index");
        long startTimeVersion2= System.currentTimeMillis();
        	 for(int j = 0; j < listOfFiles.length; j++)
             {
             	int docID=0;
             	File f = listOfFiles[j];
        	if(f.getName().contains("cranfield"))
        	{
        	String str=f.getName();
        	str= str.replaceAll("[^0-9]", "");
        	str=str.replaceFirst("^0+(?!$)", "");
             docID=Integer.parseInt(str);
        	}
            HashMap<String, Integer> tokens= addTokens(f);
            HashMap<String, Integer> stems= getStemmedTokens(tokens);
            HashMap<String, Integer> stems_without_stopwords= removeStopwords(tokens, stopwords);
            updateDoclenInfo(documents, docID, tokens, stems_without_stopwords);
            createIndex(docID, stems_without_stopwords, indexVersion2);
        }
        if(indexVersion2.containsKey(""))
            indexVersion2.remove("");

        System.out.println("Writing uncompressed index..");
        writeUncompressedIndex("Index_Version2.uncompressed", indexVersion2);

        System.out.println("Created..\nCompressing index..");
        frontCoding(indexVersion2);
        timeTakenVersion2= (System.currentTimeMillis() - startTimeVersion2)/1000;

        System.out.println("Created..\nWriting compressed index..");
        writeCompressedIndex( "Index_Version2.compressed", Index_FrontCoding);
        System.out.println("Created..");
    
        showData();
        writeDoclenFile("documentsInfo.txt", documents);

    }
    
    public static void updateDoclenInfo(int documents[][], int docID, HashMap<String, Integer> stems, HashMap<String, Integer> stems_without_stopwords)
    {
        int totalFileStems=0, max_tf=0;
        Set<String> fileStems= stems.keySet();
        for(String f:fileStems){
            totalFileStems+=stems.get(f);
        }

        Set<String> normalizedFileStems= stems_without_stopwords.keySet();
        for(String n:normalizedFileStems){
            max_tf=Math.max(max_tf, stems_without_stopwords.get(n));
        }
        documents[docID][0]=totalFileStems;
        documents[docID][1]=max_tf;
    }
    
    public static void writeDoclenFile(String fileName, int documents[][]) throws IOException 
    {
        File doclen= new File(fileName);
        ObjectOutputStream objectOutputStream= new ObjectOutputStream(new FileOutputStream(doclen));
        objectOutputStream.writeObject(documents);
        objectOutputStream.flush();
        objectOutputStream.close();
    }
    
    public static void showData() throws IOException 
    {   String[] terms = { "Reynolds", "NASA", "Prandtl","flow", "pressure","boundary","shock"};
        
        String versionData="";
   
        for(int i=0;i<terms.length;i++)
        {
        	String l=lemmatize(terms[i].toLowerCase());
        	if(i==0||i==4||i==5)
        	{
        		versionData=versionData+terms[i]+"\t" + documentFrequency(l, indexVersion1) +
                        "\t" + term_frequency(l, indexVersion1)+ "\t" + getPostingSize(l, indexVersion1) + " \n";
        	}
        	else
        	versionData=versionData+terms[i]+"\t\t" + documentFrequency(l, indexVersion1) +
                    "\t" + term_frequency(l, indexVersion1) + "\t" + getPostingSize(l, indexVersion1) + " \n";
        }
        System.out.println("Term	\tDF\tTF\tPosting Size");
        System.out.println(versionData);
        
        System.out.println("\nIndex details........");
        System.out.println("Time taken by version 1: " + timeTakenVersion1 + " seconds");
        System.out.println("Size of uncompressed index: " + (new File("Index_Version1.uncompressed").length())/1024 + " KB");
        System.out.println("Size of compressed index: " + (new File("Index_Version1.compressed").length())/1024 + " KB");
        System.out.println("Size of inverted lists in index 1: " + Index_BlockVersion1.size());
        System.out.println("Information of some terms in index 1");
        
        System.out.println("\nIndex details........");
        System.out.println("Time taken by version 2: " + timeTakenVersion2 + " seconds");
        System.out.println("Size of uncompressed index: " + (new File("Index_Version2.uncompressed").length())/1024 + " KB");
        System.out.println("Size of compressed index: " + (new File("Index_Version2.compressed").length())/1024 + " KB");
        System.out.println("Size of inverted lists in index 2: " + Index_FrontCoding.size());
        System.out.println("Information of some terms in index 2");

        System.out.println("Term	\tDF\tTF\tPosting Size");
        Stemmer stemmer= new Stemmer();
         versionData="";
        for(int i=0;i<terms.length;i++)
        {
        	 stemmer.add(terms[i].toLowerCase().toCharArray(), terms[i].length());
             stemmer.stem();
             String s=stemmer.toString();
             if(i==0||i==4||i==5)
         	{
             System.out.println(terms[i]+"\t" + documentFrequency(s, indexVersion2) +
                     "\t" + term_frequency(s, indexVersion2) + "\t" + getPostingSize(s, indexVersion2) + " ");
         	}
             else
            	 System.out.println(terms[i]+"\t\t" + documentFrequency(s, indexVersion2) +
                         "\t" + term_frequency(s, indexVersion2) + "\t" + getPostingSize(s, indexVersion2) + " "); 
        }
        System.out.println("Nasa Results");
        NasaResults();
        System.out.println("\n\nlargest_lowest_df : indexVersion1");
        largest_lowest_df(indexVersion1);
        System.out.println("\n\nlargest_lowest_df : indexVersion2");
        largest_lowest_df(indexVersion2);
        largest_max_tf();
        largest_docLength();
    }
    public static int documentFrequency(String term, TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        TreeMap<Integer, Integer> posting= index.get(term);
        if(posting!=null)
            return posting.size();
        return 0;
    }
    
    public static int term_frequency(String term, TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        TreeMap<Integer, Integer> posting= index.get(term);
        int count=0;
        for(Map.Entry<Integer, Integer> entry: posting.entrySet())
            count+= entry.getValue();
        return count;
    }
    
    public static long getPostingSize(String term, TreeMap<String, TreeMap<Integer, Integer>> index) throws IOException 
    {
        TreeMap<Integer, Integer> posting= index.get(term);
        File tempFile= new File("TempPostingFile");
        ObjectOutputStream objectOutputStream= new ObjectOutputStream(new FileOutputStream(tempFile));
        objectOutputStream.writeObject(posting);
        objectOutputStream.close();
        objectOutputStream.flush();
        return tempFile.length();
    }

    public static void frontCoding(TreeMap<String, TreeMap<Integer, Integer>> index) 
    {
        List<Object> testIndexList= new ArrayList<Object>();
        LinkedHashMap<Integer, Short> termFreqBlock= new LinkedHashMap<Integer, Short>();
        LinkedHashSet<Short> deltaEncodingSet= new LinkedHashSet<Short>();
        Set<String> terms = index.keySet();
        List<String> termsList = new ArrayList<String>();
        String termsArray[] = terms.toArray(new String[terms.size()]);
        int k = 8, currentK = 0, originalGap=0;
        String prefix = "";
        String temp = new String("");
        for (int i = 0; i < termsArray.length; i++) {
            if (currentK < k) {
                termsList.add(currentK, termsArray[i]);
                currentK++;
            }
            if (currentK == k || i + 1 == termsArray.length) {
                if (!(prefix = longestCommonPrefix(termsList)).equals("")) {
                    temp += "[";
                    for (int j = 0; j < termsList.size(); j++) {
                        if (termsList.get(j).startsWith(prefix)) {
                            if (j == 0)
                                temp += termsList.get(j).length() + prefix + "*" + termsList.get(j).substring(prefix.length());
                            if (j > 0) {
                                temp += termsList.get(j).substring(prefix.length()).length() + "|" + termsList.get(j).substring(prefix.length());
                            }
                        } else {
                            if (j == 0)
                                temp += termsList.get(j).length() + prefix + "*" + termsList.get(j).substring(0);
                            if (j > 0) {
                                temp += termsList.get(j).substring(0).length() + "|" + termsList.get(j).substring(0);
                            }

                        }
                        TreeMap<Integer, Integer> postingList= index.get(termsList.get(j));
                        for(Map.Entry<Integer, Integer> entry: postingList.entrySet()) {
                            originalGap = Math.abs(entry.getKey() - originalGap);
                            deltaEncodingSet.add(deltaEncoding(originalGap));
                            originalGap = entry.getKey();
                        }
                        termFreqBlock.put(j, termFrequency(termsList.get(j), index));
                    }

                    temp += "]";
                    testIndexList.add(0, deltaEncodingSet);
                    testIndexList.add(1, termFreqBlock);
                    Index_FrontCoding.put(temp, testIndexList);
                    currentK = 0;testIndexList.clear();termFreqBlock.clear();
                    termsList.clear();temp="";originalGap=0;deltaEncodingSet.clear();
                }
            }
        }
    }
    
    public static String longestCommonPrefix(List<String> strings){
        if(strings.size()==0)
            return "";
        int stringArrayLength=strings.size();
        for(int prefixLength=0; prefixLength<strings.get(0).length(); prefixLength++){
            char c= strings.get(0).charAt(prefixLength);
            for(int i=1; i<stringArrayLength; i++){
                if(prefixLength>=strings.get(i).length() || strings.get(i).charAt(prefixLength)!=c){
                    if(!strings.get(i).substring(0, prefixLength).equals("")){
                        return strings.get(i).substring(0, prefixLength);
                    }
                    else{
                        stringArrayLength--;
                        break;
                    }
                }
            }
        }
        return strings.get(0);
    }

    public static TreeMap<String, String> fetch_Stopwords(String stopwordpath) throws FileNotFoundException 
    {
        TreeMap<String, String> stopwordsMap= new TreeMap<String, String>();
        Scanner read= new Scanner(new File(stopwordpath));
        while(read.hasNext())
        {
            String stopword=read.next();
            stopwordsMap.put(stopword, stopword);
        }
        read.close();
        return stopwordsMap;
    }

    public static HashMap<String, Integer> getStemmedTokens(HashMap<String, Integer> tokens)
    {
        HashMap<String, Integer> stems= new HashMap<String, Integer>();
        Stemmer stemmer= new Stemmer();
        for(String token: tokens.keySet())
        {   
            stemmer.add(token.toCharArray(), token.length());
            stemmer.stem();
            String stemmedToken= stemmer.toString();
            if(!stems.containsKey(stemmedToken))
            {
                stems.put(stemmedToken, 1);
            }
            else
            {
                stems.put(stemmedToken, stems.get(stemmedToken)+1);
            }
        }
        return stems;
    }

    public static String parseFiles(File file)
    {
    	String content=null;
    	Scanner scan;
    	try 
    	{
    		scan = new Scanner(file);
    		scan.useDelimiter("\\Z");  
    		content = scan.next(); 
    		content.toLowerCase();
    		content=content.replaceAll("\\<.*?\\>", "");
    		content=content.replaceAll("\n", " ");
    		content=content.replaceAll("'", "");
    		content=content.replaceAll("[^a-zA-Z0-9' ']", " ");
    		content=content.replaceAll("(\\s+[b-z](?=\\s))"," ");
    		content=content.replaceAll("( )+", " ");
    		content=content.replaceAll("\\d","");
    	}
    	catch (FileNotFoundException e) 
    	{
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}  
    	return content;
    	}
    
    public static HashMap<String, Integer> addTokens(File file)
    {
     
        HashMap<String, Integer> tokens= new HashMap<String, Integer>();
        String content=parseFiles(file);
        StringTokenizer st = new StringTokenizer(content," ");
		while (st.hasMoreElements()) 
		{
			String word=st.nextElement().toString();
			 Stemmer stemmer= new Stemmer();
             char stemChars[] = word.toCharArray();
             stemmer.add(stemChars, stemChars.length);
             stemmer.stem();
			word=stemmer.toString();
			if (tokens.containsKey(word)!= true)
				tokens.put(word, 1);
             else
            	 tokens.put(word,tokens.get(word)+1);
		}
        return tokens;
    }

    public static HashMap<String, Integer> removeStopwords(HashMap<String, Integer> lemmas, TreeMap<String, String> stopwords) throws FileNotFoundException 
    {
        Iterator<Map.Entry<String, Integer>> lemmaIterator= lemmas.entrySet().iterator();
        HashMap<String, Integer> lemmas_without_stopwords= new HashMap<String, Integer>();
        while(lemmaIterator.hasNext())
        {
            Map.Entry<String, Integer> lemmaMap= lemmaIterator.next();
                if(!stopwords.containsKey(lemmaMap.getKey()))
                {
                	if(lemmas_without_stopwords.containsKey(lemmaMap.getKey()))
                		lemmas_without_stopwords.put(lemmaMap.getKey(), lemmaMap.getValue()+1);
                	else
                    lemmas_without_stopwords.put(lemmaMap.getKey(), lemmaMap.getValue());
                }
        }
        return lemmas_without_stopwords;
    }

    public static void createIndex(int docID, HashMap<String, Integer> HashMap_without_stopwords, TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        Iterator<Map.Entry<String, Integer>> iterator= HashMap_without_stopwords.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, Integer> entry=iterator.next();
            insertInIndex(entry.getKey(), docID, entry.getValue(), index);
        }
    }

    public static void insertInIndex(String term, int docID, int termFrequency, TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        TreeMap<Integer, Integer> currentPostingList= index.get(term);
        if(currentPostingList!=null)
        {  
            currentPostingList.put(docID, termFrequency);
            index.put(term, currentPostingList);
        }
        else
        {
            TreeMap<Integer, Integer> newPostingList= new TreeMap<Integer, Integer>();
            newPostingList.put(docID, termFrequency);
            index.put(term, newPostingList);
        }
    }
    
    public static void writeUncompressedIndex( String indexName, TreeMap<String, TreeMap<Integer, Integer>> index) throws IOException
    {
        uncompressedIndexFile= new File(indexName);
        ObjectOutputStream objectOutputStream= new ObjectOutputStream(new FileOutputStream(uncompressedIndexFile));
        objectOutputStream.writeObject(index);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public static void writeCompressedIndex(String indexName, LinkedHashMap<String, List<Object>> compressedIndex) throws IOException {
        compressedIndexFile= new File(indexName);
        ObjectOutputStream objectOutputStream= new ObjectOutputStream(new FileOutputStream(compressedIndexFile));
        objectOutputStream.writeObject(compressedIndex);
        objectOutputStream.flush();
        objectOutputStream.close();
    }
    
    public static void compressIndex(TreeMap<String, TreeMap<Integer, Integer>> index, TreeMap<String, LinkedHashMap<Short, Short>> compressedIndex, String encodingType){
        Set<String> terms= index.keySet();
        for(String term: terms){
            TreeMap<Integer, Integer> postingList= index.get(term);
            LinkedHashMap<Short, Short> postingListWithGaps= new LinkedHashMap<Short, Short>();
            int originalGap=0;
            for(Map.Entry<Integer, Integer> entry: postingList.entrySet()){
                originalGap= entry.getKey()-originalGap;
                if(encodingType.equals("gamma")){
                    postingListWithGaps.put(gammaEncoding(originalGap), termFrequency(term, index));
                }
                else if(encodingType.equals("delta"))
                    postingListWithGaps.put(deltaEncoding(originalGap), deltaEncoding(entry.getValue()));

                originalGap= entry.getKey();
            }
            compressedIndex.put(term, postingListWithGaps);
        }
    }

    public static LinkedHashMap<String, List<Object>> blockCompression(TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        int k=8;int currentK=0;
        LinkedHashMap<Integer, Short> termFreqBlock= new LinkedHashMap<Integer, Short>();
        List<Object> testIndexList= new ArrayList<Object>();
        String dictionaryString=new String("");
        Set<String> terms= index.keySet();
        LinkedHashSet<Short> gammaEncodingSet= new LinkedHashSet<Short>();
        int originalGap=0;
        String termsArray[]= terms.toArray(new String[terms.size()]);
        for(int i=0; i<termsArray.length; i++){
            if(currentK<k){
                dictionaryString+="".concat(termsArray[i].length()+"").concat(termsArray[i]);
                TreeMap<Integer, Integer> postingList= index.get(termsArray[i]);
                LinkedHashMap<Short, Short> postingListWithGaps= new LinkedHashMap<Short, Short>();
                for(Map.Entry<Integer, Integer> entry: postingList.entrySet()){
                    originalGap= Math.abs(entry.getKey()-originalGap);
                    gammaEncodingSet.add(gammaEncoding(originalGap));
                    originalGap= entry.getKey();
                }
                termFreqBlock.put(currentK, termFrequency(termsArray[i], index));
                currentK++;
            }
            if(currentK==k ||  (i+1==termsArray.length)){
                currentK=0;originalGap=0;
                testIndexList.add(0, gammaEncodingSet);
                testIndexList.add(1, termFreqBlock);
                Index_BlockVersion1.put(dictionaryString, testIndexList);
                dictionaryString="";gammaEncodingSet.clear();
                testIndexList.clear();termFreqBlock.clear();
            }
        }
        return Index_BlockVersion1;
    }

    public static short gammaEncoding(int valueToEncode)
    {   
        if(valueToEncode>0)
        {
            int offsetUnaryLength=0;
            int unary=0;
            int valueToEncodeCopy=valueToEncode;
            while(valueToEncodeCopy!=1)
            {
                valueToEncodeCopy/=2;
                offsetUnaryLength++;        
            }
            int offset=1 << offsetUnaryLength; 
            offset-=1;                          
            offset=offset & valueToEncode;  
            int offsetLengthCopy= offsetUnaryLength;   
            while(offsetUnaryLength!=0)
            {
                unary= unary << 1;
                unary= unary | 1;
                offsetUnaryLength--;
            }                                   
            unary= unary << 1;                  
            unary= unary << offsetLengthCopy;   
            offset= offset | unary; 
            return (short)offset;
        }
        else return (short)-1;
    }

    public static short deltaEncoding(int valueToEncode)
    {  
        if(valueToEncode>0)
        {
        	int offsetUnaryLength=1;
        	int valueToEncodeCopy=valueToEncode;
        	while(valueToEncodeCopy>1){
            valueToEncodeCopy/=2;
            offsetUnaryLength++;        
        }
        int code= gammaEncoding(offsetUnaryLength);     
        int offsetUnaryLengthCopy=0;
        int valueToEncodeCopy2=valueToEncode;       
        while (valueToEncodeCopy2!=1)
        {
            valueToEncodeCopy2/=2;
            offsetUnaryLengthCopy++;            
        }
        int offset=1 << offsetUnaryLengthCopy;  
        offset-=1;                         
        offset=offset & valueToEncode;  
        code= code << offsetUnaryLengthCopy;   
        code= offset | code; 
        return (short)code;
        }
        else return (short)-1;
    }
   
    public static void termFrequencyOfIndex(TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        Set<String> terms= index.keySet();
        for(String term: terms)
        {
            int tF=0;
            TreeMap<Integer, Integer> termInfo= index.get(term);
            Set<Integer> termInfoDocIDs= termInfo.keySet();
            for(Integer termInfoDocID : termInfoDocIDs){
                tF+=termInfo.get(termInfoDocID);
            }
            term_frequency.put(term, tF);
        }
    }

    public static short termFrequency(String term, TreeMap<String, TreeMap<Integer, Integer>> index)
    {
        int tF=0;
        TreeMap<Integer, Integer> termInfo= index.get(term);
        Set<Integer> termInfoDocIDs= termInfo.keySet();
        for(Integer termInfoDocID : termInfoDocIDs){
            tF+=termInfo.get(termInfoDocID);
        }
        return (short)tF;
    }
  
    public static void maxtf_doclenOfDoc(int docNum)
    {
    	System.out.println("DocLength: "+ docLength.get(docNum));
    	System.out.println("Max_tf: "+ max_tf.get(docNum));
    }
    
    public static void largest_max_tf()
    {
    	Set set1 = max_tf.entrySet();
        Iterator it1 = set1.iterator();
        int max=Collections.max(max_tf.values());
        String maxTerm="";
        while(it1.hasNext())
        {
            Map.Entry me = (Map.Entry)it1.next();
            if((int)me.getValue()==max)
            	maxTerm=me.getKey().toString();
          } 
        System.out.println("\n\nThe document with the largest max_tf in collection");
        System.out.println("DocID: "+maxTerm+"\t");
    }
    
    public static void largest_docLength()
    {
    	Set set1 = docLength.entrySet();
        Iterator it1 = set1.iterator();
        int max=Collections.max(docLength.values());
        String maxTerm="";
        while(it1.hasNext())
        {
            Map.Entry me = (Map.Entry)it1.next();
            if((int)me.getValue()==max)
            	maxTerm=me.getKey().toString();
          } 
        System.out.println("\n\nThe document with the largest doc length in collection");
        System.out.println("DocID:"+maxTerm+"\tDoclen: "+max);
    }
    
   // docLength
    public static void termFrequencyFirstDoc(TreeMap<String,TreeMap<Integer, Integer>> index)
    {
    	Set set = index.get("nasa").entrySet();
        Iterator it = set.iterator();
        for(int i=0;i<3;i++)
        {
        	Map.Entry me = (Map.Entry)it.next();
        int docNum=(int) me.getKey();
    	System.out.println("Doc id"+(i+1)+" in which NASA is present: "+ me.getKey()+ " ,TF in this Doc: " +index.get("nasa").get(me.getKey()));
    	maxtf_doclenOfDoc(docNum);
        }
    }
    public static void largest_lowest_df(TreeMap<String, TreeMap<Integer, Integer>> index)
    {
    	try
    	{
    	term_df=new TreeMap<String,Integer>();
    	Set set = index.entrySet();
        Iterator it = set.iterator();   
        while(it.hasNext()) 
        {
        	Map.Entry me1 = (Map.Entry)it.next();
    	   int num=     documentFrequency(me1.getKey().toString(), indexVersion1);
    	        term_df.put( me1.getKey().toString(), num) ; 
        }
        System.out.println("\nTerm with Smallest DF    	DF");
       Set set1 = term_df.entrySet();
        Iterator it1 = set1.iterator();
        int max=Collections.max(term_df.values());
        String maxTerm="";
        while(it1.hasNext())
        {
            Map.Entry me = (Map.Entry)it1.next();
            if((int)me.getValue()==1)
            {
            System.out.println("Term: "+me.getKey()+"\t"+me.getValue());
            }
            if((int)me.getValue()==max)
            	maxTerm=me.getKey().toString();
          } 
       System.out.println("Term: "+maxTerm+" ,largest df: "+ Collections.max(term_df.values()));
    	}
    	catch(Exception E)
    	{
    		 E.printStackTrace();
    	}
    }
    
    public static void NasaResults()
    {
    	System.out.println("Doc freq of Nasa: "+documentFrequency("nasa", indexVersion1));  	
    	termFrequencyFirstDoc(indexVersion1);
    }
  
    public static void main(String[] args) throws Exception 
    {
        String directory_path=args[0];
        String stopwords_path=args[1];
        StanfordLemmatizer();
        scanFiles(directory_path,stopwords_path);  
    }
}