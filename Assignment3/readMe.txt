Implementation of  a simple statistical relevance model  based on the vector relevance model: Information Retrieval CS 6322

Assignment: 3

Programming_project:  Shiva Podugu   Net_id: sxp170130
 
1) Please copy all the files(including stopwords.txt and hw3.queries) from folder "IR_assignment3\src" to your directory.

2) export corenlp=/usr/local/corenlp350

3) javac -cp .:$corenlp/stanford-corenlp-3.5.0.jar:$corenlp/stanford-corenlp-3.5.0-models.jar:$corenlp/slf4j-api.jar:$corenlp/slf4j-simple.jar IR3.java

4) java -cp .:$corenlp/stanford-corenlp-3.5.0.jar:$corenlp/stanford-corenlp-3.5.0-models.jar:$corenlp/slf4j-api.jar:$corenlp/slf4j-simple.jar IR3 /people/cs/s/sanda/cs6322/Cranfield/ stopwords.txt hw3.queries