import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class BayesianNetworkInference {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage for rejection sampling: <inferenceType=rejection> <numSamples> <XMLBIF filename> <queryVariable> [<evidenceVariable>=<value> ...]");
            System.out.println("Usage for enumeration: <inferenceType=enumeration> <XMLBIF filename> <queryVariable> [<evidenceVariable>=<value> ...]");
            return;
        }
    
    	String inferenceType = args[0];
        BayesianNetwork network = new BayesianNetwork();
        InferenceEngine engine = new InferenceEngine(network);
    
    	if ("rejection".equalsIgnoreCase(inferenceType)) {
            if (args.length < 5) {
                System.out.println("Invalid arguments for rejection sampling.");
                return;
            }
            int numSamples = Integer.parseInt(args[1]);
            String filename = args[2];
            String queryVariable = args[3];
            Map<String, String> evidence = parseEvidence(args, 4);
    
            network.loadFromXMLBIF(filename);
            Map<String, Double> results = engine.rejectionSampling(queryVariable, evidence, numSamples);
            printResults(queryVariable, results);
        } else if ("enumeration".equalsIgnoreCase(inferenceType)) {
            if (args.length < 4) {
                System.out.println("Invalid arguments for enumeration.");
                return;
            }
            String filename = args[1];
            String queryVariable = args[2];
            Map<String, String> evidence = parseEvidence(args, 3);
    
            network.loadFromXMLBIF(filename);
            Map<String, Double> results = engine.enumerationAsk(queryVariable, evidence);
            printResults(queryVariable, results);
        } else {
            System.out.println("Invalid inference type. Supported types are 'rejection' and 'enumeration'.");
        }
    }
    

    private static Map<String, String> parseEvidence(String[] args, int start) {
        Map<String, String> evidence = new HashMap<>();
        for (int i = start; i < args.length; i++) {
            String[] parts = args[i].split("=");
            if (parts.length == 2) {
                evidence.put(parts[0], parts[1]);
            } else {
                System.out.println("Invalid evidence format: " + args[i]);
            }
	}
	return evidence;
    }

    private static void printResults(String queryVariable, Map<String, Double> results) {
        System.out.println("Probability distribution of '" + queryVariable + "' given the evidence:");
        results.forEach((value, probability) -> System.out.println(value + ": " + probability));
    }

    public static class BayesianNetwork {
        private Map<String, BayesianNode> nodes = new HashMap<>();

        public void loadFromXMLBIF(String filename) {
            try {
                File inputFile = new File(filename);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputFile);
                doc.getDocumentElement().normalize();

                NodeList variableList = doc.getElementsByTagName("VARIABLE");
                for (int i = 0; i < variableList.getLength(); i++) {
                    Element variableElement = (Element) variableList.item(i);
                    String variableName = variableElement.getElementsByTagName("NAME").item(0).getTextContent();
                    List<String> outcomes = new ArrayList<>();
                    NodeList outcomeNodes = variableElement.getElementsByTagName("OUTCOME");
                    for (int j = 0; j < outcomeNodes.getLength(); j++) {
                        outcomes.add(outcomeNodes.item(j).getTextContent());
                    }
                    nodes.put(variableName, new BayesianNode(variableName, outcomes));
                }

                NodeList definitionList = doc.getElementsByTagName("DEFINITION");
                for (int i = 0; i < definitionList.getLength(); i++) {
                    Element definitionElement = (Element) definitionList.item(i);
                    String nodeName = definitionElement.getElementsByTagName("FOR").item(0).getTextContent();
                    BayesianNode node = nodes.get(nodeName);

                    NodeList givenList = definitionElement.getElementsByTagName("GIVEN");
                    List<String> parents = new ArrayList<>();
                    for (int j = 0; j < givenList.getLength(); j++) {
                        parents.add(givenList.item(j).getTextContent());
                    }
                    node.setParents(parents);

                    String tableContent = definitionElement.getElementsByTagName("TABLE").item(0).getTextContent();
                    node.setCPT(tableContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

	    public BayesianNode getNode(String name) {
            return nodes.get(name);
        }
    }

    public static class BayesianNode {
        private String name;
        private List<String> outcomes;
        private List<String> parents;
        private Map<String, Double> cpt; 

        public BayesianNode(String name, List<String> outcomes) {
            this.name = name;
            this.outcomes = outcomes;
            this.parents = new ArrayList<>();
            this.cpt = new HashMap<>();
        }

        public void setParents(List<String> parents) {
            this.parents = parents;
        }

        public void setCPT(String tableContent) {
            //System.out.println("Raw CPT Data: " + tableContent);
        
            String normalized = tableContent.replaceAll("\\s+", " ").trim();
            String[] probs = normalized.split(" ");
            try {
                for (int i = 0; i < outcomes.size(); i++) {
                    cpt.put(outcomes.get(i), Double.parseDouble(probs[i]));
                }
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse probabilities: " + Arrays.toString(probs));
                e.printStackTrace();
            }
        }
        

       	public double getProbability(String outcome) {
            return cpt.getOrDefault(outcome, 0.0);
        }

        public List<String> getOutcomes() {
            return outcomes;
        }

        public List<String> getParents() {
            return parents;
        }
    }

    public static class InferenceEngine {
        private BayesianNetwork network;
        private Random random;

        public InferenceEngine(BayesianNetwork network) {
            this.network = network;
            this.random = new Random();
        }
        

       	public Map<String, Double> rejectionSampling(String queryVariable, Map<String, String> evidence, int numSamples) {
            Map<String, Double> counts = new HashMap<>();
            for (String outcome : network.getNode(queryVariable).getOutcomes()) {
                counts.put(outcome, 0.0);
            }

            for (int i = 0; i < numSamples; i++) {
                Map<String, String> sample = generateSample(evidence);
                if (matchesEvidence(sample, evidence)) {
                    String outcome = sample.get(queryVariable);
                    counts.put(outcome, counts.get(outcome) + 1);
                }
            }

            double total = counts.values().stream().mapToDouble(x -> x).sum();
            counts.forEach((key, value) -> counts.put(key, value / total));
            return counts;
        }

	    private Map<String, String> generateSample(Map<String, String> evidence) {
            Map<String, String> sample = new HashMap<>(evidence);
            List<String> variables = new ArrayList<>(network.nodes.keySet()); 
            for (String v : variables) {
                if (!sample.containsKey(v)) {
                    BayesianNode node = network.getNode(v);
                    if (node == null) {
                        System.err.println("Node not found: " + v);  
                        continue;  
                    }
                    double p = random.nextDouble();
                    double cumulativeProbability = 0.0;
                    for (String outcome : node.getOutcomes()) {
                        cumulativeProbability += node.getProbability(outcome);
                        if (p < cumulativeProbability) {
                            sample.put(v, outcome);
                            break;
                        }
                    }
                }
            }
            return sample;
        }

        public Map<String, Double> enumerationAsk(String queryVariable, Map<String, String> evidence) {
            BayesianNode queryNode = network.getNode(queryVariable);
            Map<String, Double> results = new HashMap<>();
    
            for (String value : queryNode.getOutcomes()) {
                Map<String, String> extendedEvidence = new HashMap<>(evidence);
                extendedEvidence.put(queryVariable, value);
                double prob = enumerateAll(new ArrayList<>(network.nodes.keySet()), extendedEvidence);
                results.put(value, prob);
            }
    
           
            normalize(results);
            return results;
        }
    
        private double enumerateAll(List<String> vars, Map<String, String> evidence) {
            if (vars.isEmpty()) return 1.0;
            String Y = vars.remove(0);  
    
            BayesianNode node = network.getNode(Y);
            if (evidence.containsKey(Y)) {
                double prob = node.getProbability(evidence.get(Y));
                return prob * enumerateAll(vars, evidence); 
            } else {
                double sum = 0.0;
                for (String y : node.getOutcomes()) {  
                    evidence.put(Y, y);
                    sum += node.getProbability(y) * enumerateAll(new ArrayList<>(vars), evidence);
                    evidence.remove(Y);
                }
                return sum;
            }
        }
    
    	private void normalize(Map<String, Double> distribution) {
            double total = distribution.values().stream().mapToDouble(f -> f).sum();
            for (Map.Entry<String, Double> entry : distribution.entrySet()) {
                entry.setValue(entry.getValue() / total);
            }
	}


        private boolean matchesEvidence(Map<String, String> sample, Map<String, String> evidence) {
            return evidence.entrySet().stream()
                .allMatch(e -> e.getValue().equals(sample.get(e.getKey())));
        }
    }
}





    


