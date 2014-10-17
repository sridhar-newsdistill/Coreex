import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeVisitor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by blin on 10/11/14.
 */
public class Coreex {

    public static void main(String[] args) {
//        String html = "<html><body>Note:<p>A paragraph <a href=\"http://www.google.com\"> with a link</a> in it.</p><ul><li>Some <em>emphatic words</em> here.</li><li>More words.</li></ul></body></html>";
//        Document doc = Jsoup.parse(html);
        File input = new File("businessweek.html");
        Document doc = null;
        try {
            doc = Jsoup.parse(input, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.select("script, select, form, input, textarea, option").remove();


        final Map<Node, Double> textCnt = new HashMap<Node, Double>();
        final Map<Node, Double> linkCnt = new HashMap<Node, Double>();
        final Map<Node, Double> basicScore = new HashMap<Node, Double>();
        final Map<Node, Double> weightedScore = new HashMap<Node, Double>();
        final Map<Node, Double> subsetBasicScore = new HashMap<Node, Double>();
        final Map<Node, Double> subsetWeightedScore = new HashMap<Node, Double>();
        final Map<Node, Double> setTextCnt = new HashMap<Node, Double>();
        final Map<Node, Double> setLinkCnt = new HashMap<Node, Double>();
        final Map<Node, Set<Node>> S = new HashMap<Node, Set<Node>>();

        // total number of words in page used for weighted scoring function
        int wordCount = 0;
        // this includes the words in the <a href=/> tag "with a link". 14 vs 11 words for pageText either one doesn't match up to papers numbers
        StringTokenizer tokenizer = new StringTokenizer(doc.text());
        wordCount += tokenizer.countTokens();

        final double pageText = (double) wordCount;
        final double weightRatio = .99;
        final double weightText = .01;
        final double threshold = .9;

//        System.out.println("text: " + doc.text());
//        System.out.println("pageText: " + pageText);
//        System.out.println(doc);
//        System.out.println();

        // jsoup has callback functions for doing DFS on the dom, tail will get called after all its descendants are
        // visited which is what we want. We want to do a postorder traversal of the dom.
        doc.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
            }

            @Override
            public void tail(Node node, int depth) {
                S.put(node, new HashSet<Node>());
                setTextCnt.put(node, 0.0);
                setLinkCnt.put(node, 0.0);
                textCnt.put(node, 0.0);
                linkCnt.put(node, 0.0);
                // Elements node represent html tags like <p>, <body>, <li>, <ul>
                // TextNode represent actual text contained in the elements
                if (node instanceof Element) {
                    Element e = (Element) node;
//                    System.out.println("html tag: " + e.tag());
                    if (e.tag().getName().equals("a")) {
                        // This is a terminal/leaf node
                        textCnt.put(node, 1.0);
                        linkCnt.put(node, 1.0);
                    } else {
                        // This is a non terminal/leaf node
                        for (Node childNode : e.childNodes()) {
                            textCnt.put(node, textCnt.get(node) + textCnt.get(childNode));
                            linkCnt.put(node, linkCnt.get(node) + linkCnt.get(childNode));
                            double childRatio = (textCnt.get(childNode) - linkCnt.get(childNode)) / textCnt.get(childNode);
                            if (childRatio > threshold) {
                                setTextCnt.put(node, setTextCnt.get(node) + textCnt.get(childNode));
                                setLinkCnt.put(node, setLinkCnt.get(node) + linkCnt.get(childNode));
                                S.get(node).add(childNode);
                            }
                        }
//                        System.out.println("setTextCnt: " + setTextCnt.get(node));
//                        System.out.println("setLinkCnt: " + setLinkCnt.get(node));
//                        System.out.println("set S size: " + S.get(node).size());
//                        System.out.println("set S: " + S.get(node));
                    }
//                    System.out.println("textCnt: " + textCnt.get(node));
//                    System.out.println("linkCnt: " + linkCnt.get(node));
                } else if (node instanceof TextNode) {
                    // This is a terminal/leaf node
                    TextNode t = (TextNode) node;
                    StringTokenizer tokenizer = new StringTokenizer(t.getWholeText());
                    textCnt.put(node, (double) tokenizer.countTokens());
                    linkCnt.put(node, 0.0);
//                    System.out.println("TextNode: " + t.text());
//                    System.out.println("textCnt: " + textCnt.get(node));
//                    System.out.println("linkCnt: " + linkCnt.get(node));
                } else {
//                    System.out.println("Illegal Node: " + node.toString());
                    return;
                }
                double nodeBasicScore = (textCnt.get(node) - linkCnt.get(node)) / textCnt.get(node);
                double nodeWeightedScore = weightRatio * nodeBasicScore + weightText * (textCnt.get(node) / pageText);
                double nodeSubsetBasicScore = (setTextCnt.get(node) - setLinkCnt.get(node)) / setTextCnt.get(node);
                double nodeSubsetWeightedScore = weightRatio * nodeSubsetBasicScore + weightText * (setTextCnt.get(node) / pageText);
                basicScore.put(node, nodeBasicScore);
                weightedScore.put(node, nodeWeightedScore);
                subsetBasicScore.put(node, nodeSubsetBasicScore);
                subsetWeightedScore.put(node, nodeSubsetWeightedScore);
//                System.out.println("basicScore: " + nodeBasicScore);
//                System.out.println("weightedScore: " + nodeWeightedScore);
//                System.out.println("subsetBasicScore: " + nodeSubsetBasicScore);
//                System.out.println("subsetWeightedScore: " + nodeSubsetWeightedScore);
//                System.out.println();
            }
        });
        double maxSubsetWeightedScore = -1;
        Node maxSubsetWeightedScoreNode = null;
        for (Map.Entry<Node, Double> entry : subsetWeightedScore.entrySet()) {
            if (entry.getValue() > maxSubsetWeightedScore) {
                maxSubsetWeightedScore = entry.getValue();
                maxSubsetWeightedScoreNode = entry.getKey();
            }
        }
        System.out.println("highest subsetWeightedScore: " + maxSubsetWeightedScore);
        System.out.println("highest subsetWeightedScore Node: " + maxSubsetWeightedScoreNode);
        System.out.println();
        System.out.println("Content Node Count: " + S.get(maxSubsetWeightedScoreNode).size());
        System.out.println();
        System.out.println("Content Nodes");
        for (Node node : S.get(maxSubsetWeightedScoreNode)) {
            System.out.println(node);
            System.out.println();
        }
    }
}
