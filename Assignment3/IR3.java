import java.io.*;
import java.util.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class IR3 
{
	   public static StanfordCoreNLP pipeline;
	   public static TreeMap<String, Integer> Query_terms;//= new TreeMap<String, Integer>();
	   public static TreeMap<String, Integer> stopwordsMap= new TreeMap<String, Integer>();
	   public static TreeMap<String,Integer> term_df= new TreeMap<String, Integer>();
	   public static TreeMap<String,Integer> doclen= new TreeMap<String, Integer>();
	   public static TreeMap<String,String> headLine= new TreeMap<String, String>();
	   public static TreeMap<String, TreeMap<String, Integer>> term_frequency_each_doc= new TreeMap<String, TreeMap<String, Integer>>();
	   public static TreeMap<String, TreeMap<String, Integer>> max_tf= new TreeMap<String, TreeMap<String, Integer>>();
	   public static int collectionSize=0;
	   public static double avgDocLen=0;
	   public static TreeMap<String, TreeMap<String, Double>> w1_score_each_term_each_doc;//= new TreeMap<String, TreeMap<String, Double>>();
	   public static TreeMap<String, TreeMap<String, Double>> w2_score_each_term_each_doc;//= new TreeMap<String, TreeMap<String, Double>>();
	
	   public static TreeMap<String, Double> Total_w1_score_each_doc;//= new TreeMap<String,Double>();
	   public static TreeMap<String, Double> Total_w2_score_each_doc;//= new TreeMap<String,Double>();
	   public static TreeMap<String, Double> w1_score_each_term_query;// =new TreeMap<String, Double>();
	   public static TreeMap<String, Double> w2_score_each_term_query;//= new TreeMap<String, Double>();
	   static String[] queries = new String[20];
		
	public static void main(String[] args) throws Exception 
	{
	    long startTimeVersion1= System.currentTimeMillis();
		StanfordLemmatizer();
		String directory_path=args[0];
		fetch_Stopwords(args[1]);
		FilesProcessor(directory_path);
		readHW3Queries(args[2]);
		System.out.println("files processed");
		System.out.println("collectionSize: "+collectionSize);
		findAvgDoclen();
		
		for(int i=0;i<queries.length;i++)
		{
			QueryProcessor(queries[i]);
			calculateTotalScoreOfEachDoc();
			W1_normalizationOfScore();
			W2_normalizationOfScore();
	 	    // System.out.println("Norm Total_w1_score_each_doc: "+Total_w1_score_each_doc);
			
			calculateQueryWeightVector();
			System.out.print("\nw1_score_each_term_query");
			System.out.print(w1_score_each_term_query);
			System.out.println("\nResult w.r.t W1, Query: "+(i+1));
			w1_top_5_docs();
			System.out.print("\nw2_score_each_term_query");
			System.out.print(w2_score_each_term_query);
			System.out.println("\nResult w.r.t W2, Query: "+(i+1));
			w2_top_5_docs();
		}
		System.out.println("time taken"+(startTimeVersion1-System.currentTimeMillis())/1000+" seconds");
	}
	
	public static void calculateQueryWeightVector()
	{
		 w1_score_each_term_query =new TreeMap<String, Double>();
		 w2_score_each_term_query= new TreeMap<String, Double>();
		 Set set1 = Query_terms.entrySet();
	        Iterator it1 = set1.iterator();   
	        int maxTf= Collections.max(Query_terms.values());
	        while(it1.hasNext()) 
	        {
	        	Map.Entry me1 = (Map.Entry)it1.next();
	        	String term=me1.getKey().toString();
	        	int tf=Integer.parseInt(me1.getValue().toString());
	        	double w1=calculateWeight1(tf,maxTf,1,Query_terms.size());
	        	double w2=calculateWeight2(tf,1,Query_terms.size(),Query_terms.size(),Query_terms.size());
	        	w1_score_each_term_query.put(term, w1);
	        	w2_score_each_term_query.put(term, w2);
	        }
	}
	public static void w1_top_5_docs()
	{
		//W1_normalizationOfScore();
		TreeMap<String, Double> sortedTokensByValue= DecreasingSort(Total_w1_score_each_doc);
	    Iterator<Map.Entry<String, Double>> sortedIterator = sortedTokensByValue.entrySet().iterator();
	    Iterator<Map.Entry<String, Double>> sortedIterator1 = sortedTokensByValue.entrySet().iterator();
	    for(int count=1;count<=5;count++)
	    {
	        Map.Entry<String, Double> entry = sortedIterator.next();
	        System.out.println("Rank: "+count + "\t,DocName: " + entry.getKey() + "\t,Score: " + entry.getValue()+" ,Headline:"+headLine.get(entry.getKey()));
	        //System.out.println("w1_score_each_term_each_doc"+w1_score_each_term_each_doc.get(entry.getKey()));
	        //System.out.println("w1_score_each_term_each_doc"+w1_score_each_term_each_doc);
	    }
	    System.out.println("\nVector representation of top 5 ranked documents\n");
	    for(int count=1;count<=5;count++)
	    {
	    	Map.Entry<String, Double> entry = sortedIterator1.next();
	        System.out.println("DocName: " + entry.getKey()+"\t"+w1_score_each_term_each_doc.get(entry.getKey()));
	        //System.out.println("w1_score_each_term_each_doc"+w1_score_each_term_each_doc);
	    }
	   // w1_score_each_term_each_doc;
	   // System.out.println("sortedTokensByValue"+sortedTokensByValue);
	}
	
	public static void w2_top_5_docs()
	{
		TreeMap<String, Double> sortedTokensByValue= DecreasingSort(Total_w2_score_each_doc);
	    Iterator<Map.Entry<String, Double>> sortedIterator = sortedTokensByValue.entrySet().iterator();
	    Iterator<Map.Entry<String, Double>> sortedIterator1 = sortedTokensByValue.entrySet().iterator();
	    for(int count=1;count<=5;count++)
	    {
	        Map.Entry<String, Double> entry = sortedIterator.next();
	        System.out.println("Rank: "+count + "\t,DocName: " + entry.getKey() + "\t,Score: " + entry.getValue()+" ,Headline:"+headLine.get(entry.getKey()));
	      // System.out.println("w2_score_each_term_each_doc"+w2_score_each_term_each_doc.get(entry.getKey()));
	       // System.out.println("Vector representation of top 5 ranked documents\n");
	    }
	    System.out.println("\nVector representation of top 5 ranked documents\n");
	    for(int count=1;count<=5;count++)
	    {
	    	Map.Entry<String, Double> entry = sortedIterator1.next();
	        System.out.println("DocName: " + entry.getKey()+"\t"+w2_score_each_term_each_doc.get(entry.getKey()));
	        //System.out.println("w1_score_each_term_each_doc"+w1_score_each_term_each_doc);
	    }
	}
	public static void calculateTotalScoreOfEachDoc()
	{
		 Total_w1_score_each_doc= new TreeMap<String,Double>();
		 Total_w2_score_each_doc= new TreeMap<String,Double>();
		 w1_score_each_term_each_doc=new TreeMap<String, TreeMap<String, Double>>();
		 w2_score_each_term_each_doc=new TreeMap<String, TreeMap<String, Double>>();
		System.out.println("\n\nQuery_terms"+Query_terms);
		//find score with respect to each document
		String docname="";
		TreeMap<String, Integer> term_tf_temp;
		TreeMap<String, Double> term_weight1,term_weight2;
		term_weight1= new TreeMap<String, Double>();
     	term_weight2=new TreeMap<String, Double>();
		//int tf=0,df=0,maxTf=0;
		//double w1=0,w2=0;
		 Set set1 = term_frequency_each_doc.entrySet();
	        Iterator it1 = set1.iterator();   
	        while(it1.hasNext()) 
	        {
	        	Map.Entry me1 = (Map.Entry)it1.next();
	        	docname=me1.getKey().toString();
	        	term_tf_temp= term_frequency_each_doc.get(docname);
	        
	        	Set set2 = Query_terms.entrySet();
	 	        Iterator it2 = set2.iterator();   
	 	        double sum1=0,sum2=0;
	 	        int tf=0,df=0,maxTf=0;
	 			double w1=0,w2=0;
	 			term_weight1= new TreeMap<String, Double>();
 	        	term_weight2=new TreeMap<String, Double>();
 	       	///
	        	Set set3 = term_tf_temp.entrySet();
		        Iterator it3 = set3.iterator();   
		        while(it3.hasNext()) 
		        {
		        	Map.Entry me3 = (Map.Entry)it3.next();
		        	term_weight1.put(me3.getKey().toString(), 0.0);
		        	term_weight2.put(me3.getKey().toString(), 0.0);
		        }
	        	////
	 	        while(it2.hasNext()) 
	 	        {
	 	        	
	 	        	//tf=0;
	 	        	Map.Entry me2 = (Map.Entry)it2.next();
	 	        	String term=me2.getKey().toString();
	 	        	if(term_tf_temp.containsKey(term))
	 	        		tf=Integer.parseInt(me2.getValue().toString());
	 	        	if(term_df.containsKey(term))
	 	        		df=term_df.get(term);
	 	        	maxTf=Max_tf_doc(docname);
	 	        	//System.out.println("maxTf: "+maxTf);
	 	        	w1=calculateWeight1(tf,maxTf,df,collectionSize);
	 	        	w2=calculateWeight2(tf,df,doclen.get(docname),collectionSize,avgDocLen);
	 	        	//System.out.println("term"+term);
	 	        	//if(!term_weight1.containsKey(term))
	 	        	term_weight1.put(term, w1);
	 	        	//System.out.println("term_weight1"+term_weight1);
	 	        	term_weight2.put(term, w2);
	 	        	sum1=sum1+w1;
	 	        	sum2=sum2+w2;
	 	        	// System.out.println("w1: "+w1);
	 		 	      // System.out.println("w2: "+w2);
	 	        }
	 	   
	 	       Total_w1_score_each_doc.put(docname, sum1);
	 	  
	 	       Total_w2_score_each_doc.put(docname, sum2);
	 	       //System.out.println("term_weight1"+term_weight1);
	 	       w1_score_each_term_each_doc.put(docname, term_weight1);
	 	       w2_score_each_term_each_doc.put(docname, term_weight2);
	        }
	        
	}
	public static void W1_normalizationOfScore()
	{
		 double mean=0,variance=0,standardDeviation=0; 
		 mean=getMean(Total_w1_score_each_doc,Total_w1_score_each_doc.size());
		 variance=getVariance(mean,Total_w1_score_each_doc.size(),Total_w1_score_each_doc);
		 standardDeviation=getStdDev(variance);
		 Set set1 = Total_w1_score_each_doc.entrySet();
	        Iterator it1 = set1.iterator();   
	        while(it1.hasNext()) 
	        {
	        	Map.Entry me1 = (Map.Entry)it1.next();
	        	//if(standardDeviation!=0)
	        	//{
	        		Total_w1_score_each_doc.put(me1.getKey().toString(),(Double.parseDouble(me1.getValue().toString())-mean)/standardDeviation);
	        	//}
	        }
	}
	
	public static void W2_normalizationOfScore()
	{

		 double mean=0,variance=0,standardDeviation=0; 
		 mean=getMean(Total_w2_score_each_doc,Total_w2_score_each_doc.size());
		 variance=getVariance(mean,Total_w2_score_each_doc.size(),Total_w2_score_each_doc);
		 standardDeviation=getStdDev(variance);
		 Set set1 = Total_w2_score_each_doc.entrySet();
	        Iterator it1 = set1.iterator();   
	        while(it1.hasNext()) 
	        {
	        	Map.Entry me1 = (Map.Entry)it1.next();
	        /*	if(standardDeviation==0)
    	    	{
	        		Total_w2_score_each_doc.put(me1.getKey().toString(),0.0);
		        	
    	    	}
    	    	*/
	        	if(standardDeviation!=0)
	        	{
	        		Total_w2_score_each_doc.put(me1.getKey().toString(),(Double.parseDouble(me1.getValue().toString())-mean)/standardDeviation);
	        	}
	        }
		
	}
	
	public static double getMean(TreeMap<String, Double> tempMap,int size)
    {
        double sum = 0.0;
        for (double f : tempMap.values()) 
        {
            sum += f;
        }
        return sum/size;
    }

 public static double getVariance(double mean,int size,TreeMap<String, Double> tempMap)
    {
        double temp = 0;
        for(double a : tempMap.values())
            temp += (a-mean)*(a-mean);
        return temp/size;
    }

    public static  double getStdDev(double var)
    {
        return Math.sqrt(var);
    }
    
	public static TreeMap<String, Double> DecreasingSort(final TreeMap<String, Double> temp)
	{
	    Comparator<String> keycomparator = new Comparator<String>() 
	    {
	        public int compare(String s1, String s2) 
	        {
	            if (temp.get(s2).compareTo(temp.get(s1)) == 0)
	                return 1;
	            else
	                return temp.get(s2).compareTo(temp.get(s1));
	        }
	    };
	    
	    TreeMap<String, Double> sortedTokensByValue = new TreeMap<String, Double>(keycomparator);
	    sortedTokensByValue.putAll(temp);
	    return sortedTokensByValue;
	}
	
	public static double calculateWeight1(int tf,int maxTf,int df, int cs)
	{
		double w1=0;
		try{
				w1 = (0.4 + 0.6 *  Math.log(tf + 0.5) / Math.log(maxTf + 1.0))*(Math.log(cs / df)/ Math.log(cs))  ;
			}
		catch(ArithmeticException ae)
		{
			w1=0;
		}
		return w1;
	}
	
	public static double calculateWeight2(int tf, int df, int dl,int cs, double avglen)
	{
		double w2=0;
		try{
		w2= (0.4 + 0.6 * (tf / (double)(tf + 0.5 + 1.5 *(double)(dl / avglen))) * (double) Math.log10 (cs / (double)df)/Math.log10 (cs));
		if (Double.isNaN(w2)) 
		{
		   w2=0;
		}
		//System.out.println("w2: "+w2);
	 	}
		catch(ArithmeticException ae)
		{
			w2=0;
		}
		return w2;
	}
	
	public static int Max_tf_doc(String docName)
	{
		int maxTf=0;
		TreeMap<String, Integer> max_tf_temp = new TreeMap<String, Integer>();
		max_tf_temp=max_tf.get(docName);
		maxTf=Collections.max(max_tf_temp.values());
		return maxTf;
	}
	public static void findAvgDoclen()
	{
		int sum=0;
		Set set = doclen.entrySet();
        Iterator it = set.iterator();   
        while(it.hasNext()) 
        {
        	Map.Entry me = (Map.Entry)it.next();
        	sum=sum+Integer.parseInt(me.getValue().toString());
        }
        avgDocLen=(double)sum/collectionSize;
        System.out.println("avgDocLen: "+avgDocLen);
	}
	
	public static void SaveHeadLine(Scanner inputFile,String docName)
	{
		while (inputFile.hasNextLine()) 
		{
		  if (inputFile.nextLine().contains("<TITLE>"))
		    break; 
		}
		String hl=inputFile.nextLine();
		headLine.put(docName, hl);
	}
	
	public static void FilesProcessor(String directoryPath) throws IOException 
    {
        File file = new File(directoryPath);
        File listOfFiles[] = file.listFiles();
        Arrays.sort(listOfFiles);
        collectionSize=listOfFiles.length;

        System.out.println("\nCreating index");
        List<String> UniqueTermsInCurrentDoc;
        TreeMap<String, Integer> lemmas_temp,max_tf_temp;
 
        for(int j = 0; j < listOfFiles.length; j++)
        {   
        	UniqueTermsInCurrentDoc=new ArrayList<>();
        	lemmas_temp = new TreeMap<String, Integer>();
       
        	int doclength=0;
        	File current_file = listOfFiles[j];
        	if(current_file.getName().contains("cranfield"))
        	{
        		String content=parseFiles(current_file);
        		StringTokenizer st = new StringTokenizer(content," ");
        		while (st.hasMoreElements()) 
        		{  
        			
    				String lemma="";
    				String word=st.nextElement().toString();
    				if(!stopwordsMap.containsKey(word))
    				{
    					doclength++;
    					lemma=lemmatize(word);
    					if (lemmas_temp.containsKey(lemma)!= true)
    						lemmas_temp.put(lemma, 1);
    					else
    						lemmas_temp.put(word,lemmas_temp.get(lemma)+1);  
    					if(!UniqueTermsInCurrentDoc.contains(lemma))
    						UniqueTermsInCurrentDoc.add(lemma);
    				
    				 }
        		}
        	}
        	term_frequency_each_doc.put(current_file.getName(), lemmas_temp);
        	
        	doclen.put(current_file.getName(), doclength);
        	int maxValueInMap=(Collections.max(lemmas_temp.values())); 
        	String term=FindTermWithMaxTfInTreeMap(lemmas_temp);
        	max_tf_temp= new TreeMap<String, Integer>();
        	max_tf_temp.put(term, maxValueInMap);
        	max_tf.put(current_file.getName(),max_tf_temp);
        	Set_term_df(UniqueTermsInCurrentDoc);
        }
    }
	

	public static void Set_term_df(List<String> UniqueTermsInCurrentDoc)
	{
		for(int i=0;i<UniqueTermsInCurrentDoc.size();i++)
		{
			if(!term_df.containsKey(UniqueTermsInCurrentDoc.get(i)))
			{
				term_df.put(UniqueTermsInCurrentDoc.get(i), 1);
			}
			else
			{
				term_df.put(UniqueTermsInCurrentDoc.get(i), term_df.get(UniqueTermsInCurrentDoc.get(i))+1);
			}
		}
	}
	
	public static String FindTermWithMaxTfInTreeMap(TreeMap<String, Integer> tm)
	{
		int maxValueInMap=(Collections.max(tm.values())); 
		String term="";
		Set set = tm.entrySet();
        Iterator it = set.iterator();  
        while(it.hasNext()) 
        {
        	Map.Entry me = (Map.Entry)it.next();
        	if(Integer.parseInt(me.getValue().toString())==maxValueInMap)
        	{
        		term=me.getKey().toString();
        		break;
        	}
        }
		return term;
	}
	    public static void QueryProcessor(String query_content)
	    {
	    	Query_terms= new TreeMap<String, Integer>();
	    	query_content.toLowerCase();
	    	query_content=query_content.replaceAll("\\<.*?\\>", "");
	   		query_content=query_content.replaceAll("\n", " ");
	   		query_content=query_content.replaceAll("'", "");
	   		query_content=query_content.replaceAll("[^a-zA-Z0-9' ']", " ");
	   		query_content=query_content.replaceAll("(\\s+[b-z](?=\\s))"," ");
    		query_content=query_content.replaceAll("( )+", " ");	    		
    		query_content=query_content.replaceAll("\\d","");
    		StringTokenizer st = new StringTokenizer(query_content," ");
    		while (st.hasMoreElements()) 
    		{  
    			String lemma="";
    			String word=st.nextElement().toString();
    			if(!stopwordsMap.containsKey(word))
    			{
    				lemma=lemmatize(word);
    				if (Query_terms.containsKey(lemma)!= true)
    					Query_terms.put(lemma, 1);
    				else
    					Query_terms.put(word,Query_terms.get(lemma)+1);  
    			}
    		}
	    }
	    public static void fetch_Stopwords(String stopwordpath) throws FileNotFoundException 
	    {
	        Scanner read= new Scanner(new File(stopwordpath));
	        while(read.hasNext())
	        {
	            String stopword=read.next();
	            stopwordsMap.put(stopword, 1);
	        }
	        read.close(); 
	    }
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
	        Annotation document = new Annotation(documentText);
	        pipeline.annotate(document);
	        String temp="";
	        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	        for(CoreMap sentence: sentences) 
	        {
	            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	            	temp=token.get(LemmaAnnotation.class);
	            	    lemmas_.add(temp);
	            }
	        }
	        return lemmas_.get(0).toString();
	    }
	    public static String parseFiles(File file)
	    {
	    	String content=null;
	    	Scanner scan;
	    	try 
	    	{
	    		scan = new Scanner(file);
	    		SaveHeadLine(scan,file.getName());
	    		scan.useDelimiter("\\Z");  
	    		content = scan.next(); 
	    		content.toLowerCase();
	    		content=content.replaceAll("\\<.*?\\>", "");
	    		content=content.replaceAll("\n", " ");
	    		content=content.replaceAll("'", "");
	    		content=content.replaceAll("[^a-zA-Z0-9' ']", " ");
	    		content=content.replaceAll("(\\s+[b-z](?=\\s))"," ");
	    		content=content.replaceAll("\\d","");
	    		content=content.replaceAll("( )+", " ");
	    		
	    		/*content = content.replaceAll("\\<.*?>", " ");
	    		content = content.replaceAll("[\\d+]", "");
	    		content = content.replaceAll("[+^:,?;=%#&~`$!@*_)/(}{\\.]", "");
	    		content = content.replaceAll("\\'s", "");
	    		content = content.replaceAll("\\'", " ");
	    		content = content.replaceAll("-", " ");
	    		content = content.replaceAll("\\s+", " ");
	    		content=content.replaceAll("( )+", " ");
	    		*/
	    	}
	    	catch (FileNotFoundException e) 
	    	{
	    		// TODO Auto-generated catch block
	    		e.printStackTrace();
	    	}  
	    	return content;
	    	}
	    public static void readHW3Queries(String QueryPath) throws FileNotFoundException
		{
			File file = new File(QueryPath);
			if(file.getName()!=".DS_Store")
			{
				Scanner scan = new Scanner(file);
				int count=0;
				while(scan.hasNextLine())
				{
					String temp=scan.nextLine();
					if(temp.contains(":"))
					{}
					else if(temp.isEmpty())
						count++;
					else
					{
						if(queries[count]!=null)
							queries[count]=queries[count]+" "+temp;
						else
							queries[count]=temp;
					}	
				}
			}
			//for(int i=0;i<queries.length;i++)
			//System.out.println(queries[i]);
		}

}
