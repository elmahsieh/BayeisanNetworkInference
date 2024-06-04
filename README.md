# README + WRITEUP

Partner Name: Emily de la Cruz

## Command Lines 
java BayesianNetworkInference enumeration <XMLBIF_filename> <queryVariable> [<evidenceVariable>=<value> ...]
java BayesianNetworkInference rejection <numSamples> <XMLBIF_filename> <queryVariable> [<evidenceVariable>=<value> ...]

## IMPORTANT -- Experimental Work

**See files: *DataCollection_aima-alarm.csv*, *DataCollection_aima-wet-grass.csv*, *DataCollection_dog-problem.csv* for detailed information
about the data collection.** Information in the files include: *File Name, Query Variable, Evidence Variable, Enumeration probability, 
Rejection probability, Sample Size, Absolute Difference.* 

**XML file and #of samples for an accurate algorithm --**

File Name: *aima-alarm.XML* -- Number of Samples Needed for Accurate Algorithm: 250~500

File Name: *aima-wet-grass.XML* -- Number of Samples Needed for Accurate Algorithm: 500~1000

File Name: *aima-alarm.XML* -- Number of Samples Needed for Accurate Algorithm: 250~750

## Project Information

**Part I:** Exact Inference --

We constructed a system representing the relationships between random variables and calculated the conditional distribution 
of a query variable given observed evidence. The program accepts input via command-line arguments, including the Bayesian 
network file, query variable, and evidence variables. The output consists of the posterior distribution of the query variable 
based on the provided evidence. Thorough data documentation can be found in csv files. 

**Part II:** Approximate Inference --

This utilizes rejection sampling, which calculates precise probabilities, approximate methods provide estimations based on sampled data.
The program accepts the number of samples as a command-line argument and output the distribution of the query variable based on the sampled data. 
Thorough data documentation can be found in csv files. 

**Part III:** Evaluation --

We assessed the performance and accuracy of the implemented inference techniques. This involves testing 
the code with provided Bayesian network examples and quantifying the number of samples necessary for the approximate 
inference method to achieve results within a 1% margin of error compared to exact inference. We carefully selected the 
queries, evidence, and networks for testing, as it is crucial in evaluating the robustness and scalability of the implemented 
techniques. Documentation of the testing process, including choices made and results obtained, is documented in the csv files.

**Algorithm Design & Implementation**

*Main Method (main):*
The main method serves as the entry point for the program.
It checks the command-line arguments to determine which type of inference to perform: rejection sampling or enumeration.
It then initializes a BayesianNetwork and an InferenceEngine object based on the provided network structure.
Depending on the chosen inference type, it calls either the rejectionSampling or enumerationAsk method of the InferenceEngine and prints the results.

*BayesianNetwork Class:*
This class represents the Bayesian network and its structure.
It contains a map of BayesianNode objects where each node represents a variable in the network.
The loadFromXMLBIF method parses an XML file containing the network structure and initializes the nodes and their conditional probability tables (CPTs).
It uses the DOM (Document Object Model) parser to read and parse the XML file.
Each node is associated with its outcomes and parents, and the CPT is populated accordingly.

*BayesianNode Class:*
Represents a node in the Bayesian network.
Contains information about the node's name, outcomes, parents, and conditional probability table (CPT).
The setCPT method parses the raw table content from the XML file and populates the CPT for the node.

*InferenceEngine Class:*
Performs probabilistic inference using rejection sampling or enumeration.
The rejectionSampling method generates samples from the Bayesian network and calculates probabilities based on the provided evidence.
The generateSample method generates a single sample by traversing the network and randomly selecting outcomes for each variable.
The enumerationAsk method performs exact inference using variable enumeration.
The enumerateAll method recursively computes the probability of a query variable given evidence by summing over all possible assignments of other variables.
Both methods handle evidence and query variables by incorporating them into the computation.
The matchesEvidence method checks if a generated sample matches the given evidence.