//shivapodugu : sxp170130

//package tokenisation;
import java.io.*;
//import org.apache.commons.io.FileUtils;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.*;

public class tokenisation 
{
	static int FilesCount=0;
	static String path;
	static TreeMap<String, Integer> tokens = new TreeMap<String, Integer>();
	static int tokensCount=0;
	static int stemsCount=0;
	static TreeMap<String, Integer> stems = new TreeMap<String, Integer>();
	
	public static void LineSeparator()
	{
		System.out.println("-------------------------------------------------------------------------------------");
	}
    public static void ReadCorrectPath()
    {
        System.out.println("Please enter directory path only");
        Scanner scanner = new Scanner(System.in);
        path=scanner.next().toString();
        LoadFiles(path);
    }
	
	public static void LoadFiles(String p)
	{
		//Can get the path manually..by commenting out the below 3 Lines
		
		//System.out.println("Please enter the Cranfield directory path: ");
		//Scanner scanner = new Scanner(System.in);
		 //path=scanner.next();
		//path="/Users/shivapodugu/Desktop/IR/Cranfield";
        path=p;
        File CranfieldDir = new File(path);
        boolean isDirectory = CranfieldDir.isDirectory();
        
        if(isDirectory)
        {
		FilesCount= new File(path).listFiles().length;
            ReadFilesContent();
        }
        else
        {
            ReadCorrectPath();
        }
	}
	
	public static void ReadFilesContent() 
	{
		System.out.println("StringTokenizing under process...");
		LineSeparator();
		LineSeparator();
		File CranfieldDir = new File(path);
      //  boolean exists =      CranfieldDir.exists();
        
		File[] listOfFiles = CranfieldDir.listFiles();
		for (int j = 0; j < listOfFiles.length; j++) 
		{
		  File file = listOfFiles[j];
		  //System.out.println("-"+file.getName());
		  if (file.isFile() ) 
		  {
		    String content;
			try 
			{
				Scanner scan = new Scanner(file);  
				  scan.useDelimiter("\\Z");  
				  content = scan.next(); 
				//content = FileUtils.readFileToString(file,"UTF-8");
				content.toLowerCase();
				//System.out.println("Content Length: "+content.length());
				content=content.replaceAll("\\<.*?\\>", "");
				content=content.replaceAll("\n", " ");
				content=content.replaceAll("'", "");
				content=content.replaceAll("[^a-zA-Z0-9' ']", " ");
				content=content.replaceAll("(\\s+[b-z](?=\\s))"," ");
				content=content.replaceAll("(\\s+[0-9](?=\\s))"," ");
				content=content.replaceAll("( )+", " ");
				//System.out.println(content);
				
				StringTokenizer st = new StringTokenizer(content," ");
				while (st.hasMoreElements()) 
				{
					tokensCount++;
					String word=st.nextElement().toString();
					if (tokens.containsKey(word)!= true)
					   tokens.put(word, 1);
                    else
                       tokens.put(word,tokens.get(word)+1);  
					//find stems here
                    Stemmer stemmer= new Stemmer();
                    char stemChars[] = word.toCharArray();
                    stemmer.add(stemChars, stemChars.length);
                    stemmer.stem();

                    //push stems of entire collection into TreeMap here
                    stemsCount++;
                    if(stems.get(stemmer.toString()) == null)
                        stems.put(stemmer.toString(), 1);
                    else
                        stems.put(stemmer.toString(), stems.get(stemmer.toString()) + 1);
				}
				//System.out.println(tokens);		
			} 
			catch (IOException e) 
			{
				System.out.println(e.toString());
			}	
		  } 
       }
	}
	
//The number of words that occur only once in the Cranfield text collection
public static int OneOccurence(TreeMap<String, Integer> T)
{
	 int OneOccurenceCount=0;
	Iterator<Map.Entry<String, Integer>> countIterator = T.entrySet().iterator();
    while(countIterator.hasNext())
    {
        Map.Entry<String, Integer> entry = countIterator.next();
        if(entry.getValue()==1)
        OneOccurenceCount++;
    }
    return OneOccurenceCount;
}

public static TreeMap<String, Integer> DecreasingSort(final TreeMap<String, Integer> tokens)
{
    Comparator<String> keycomparator = new Comparator<String>() 
    {
        public int compare(String s1, String s2) 
        {
            if (tokens.get(s2).compareTo(tokens.get(s1)) == 0)
                return 1;
            else
                return tokens.get(s2).compareTo(tokens.get(s1));
        }
    };
    
    TreeMap<String, Integer> sortedTokensByValue = new TreeMap<String, Integer>(keycomparator);
    sortedTokensByValue.putAll(tokens);
    return sortedTokensByValue;
}

    public static void main(String[] args) 
	{
		long ProcessStartTime = System.currentTimeMillis();	
		LoadFiles(args[0]);
		System.out.println("Total number of files in the Cranfield directory are "+FilesCount);
		LineSeparator();
		//ReadFilesContent();
		System.out.println("1. Total number of Tokens in the Cranfield text collection: "+tokensCount);
		LineSeparator();
		System.out.println("2. Total number of unique words in the Cranfield text collection: "+tokens.size());
		LineSeparator();
		System.out.println("3. The number of words that occur only once in the Cranfield text collection: "+OneOccurence(tokens));
		LineSeparator();
		System.out.println("4. The 30 most frequent words in the Cranfield text collection :\n");
		TreeMap<String, Integer> sortedTokensByValue= DecreasingSort(tokens);
	    Iterator<Map.Entry<String, Integer>> sortedIterator = sortedTokensByValue.entrySet().iterator();
	    for(int count=1;count<=30;count++)
	    {
	        Map.Entry<String, Integer> entry = sortedIterator.next();
	        System.out.println(count + ".\t" + entry.getKey() + "\t" + entry.getValue());
	    }
	    LineSeparator();
	    System.out.println("5. The average number of word tokens per document: " + tokensCount/FilesCount);   
        
        LineSeparator();
        System.out.println("The time taken by program to acquire the text characteristics approximately: "+(System.currentTimeMillis()-ProcessStartTime)+" milli seconds");
        
	    LineSeparator();
        LineSeparator();
        System.out.println("\nStemming under process...\n");
        LineSeparator();
        System.out.println("The number of distinct stems in the Cranfield text collection: "+stems.size());
        LineSeparator();
        System.out.println("The number of stems that occur only once in the Cranfield text collection: "+OneOccurence(stems));
        LineSeparator();
        System.out.println("The 30 most frequent stems in the Cranfield text collection: ");
        TreeMap<String, Integer> sortedStemTokens= DecreasingSort(stems);
        Iterator<Map.Entry<String, Integer>> sortedStemIterator = sortedStemTokens.entrySet().iterator();
        for(int count=1;count<=30;count++)
	    {
            Map.Entry<String, Integer> entry = sortedStemIterator.next();
            System.out.println(count + ".\t" + entry.getKey() + "\t\t" + entry.getValue());
	    }
        LineSeparator();
        System.out.println("The average number of word stems per document: "+stemsCount/FilesCount);
        LineSeparator();
        
	}
}
