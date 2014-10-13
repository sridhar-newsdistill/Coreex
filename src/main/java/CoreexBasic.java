import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by blin on 10/11/14.
 */
public class CoreexBasic {

    public static void main(String[] args) {
        final Map<Node, Double> textCnt = new HashMap<Node, Double>();
        final Map<Node, Double> linkCnt = new HashMap<Node, Double>();
        final Map<Node, Double> basicScore = new HashMap<Node, Double>();
        final Map<Node, Double> weightedScore = new HashMap<Node, Double>();

        String html = "<html> <body> Note: <p>A paragraph <a href=\"http://www.google.com\">with a link</a> in it. </p> <ul> <li> Some <em>emphatic words</em> here. </li> <li> More words. </li> </ul> </body> </html>";
        Document doc = Jsoup.parse(html);

        // total number of words in page used for weighted scoring function
        int wordCount = 0;
        System.out.println("text: " + doc.text());
        // this includes the words in the <a href=/> tag "with a link". 14 vs 11 words for pageText either one doesn't match up to papers numbers
        StringTokenizer tokenizer = new StringTokenizer(doc.text());
        wordCount += tokenizer.countTokens();
        final double pageText = (double) wordCount;
//        final double pageText = 11;
        final double weightRatio = .99;
        final double weightText = .01;
        System.out.println("pageText: " + pageText);

        System.out.println(doc);
        System.out.println();

        doc.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element) {
                    Element e = (Element) node;
                    if (e.tag().getName().equals("a")) {
                        textCnt.put(node, 1.0);
                        linkCnt.put(node, 1.0);
                    } else {
                        textCnt.put(node, 0.0);
                        linkCnt.put(node, 0.0);
                        for (Node childNode : e.childNodes()) {
                            textCnt.put(node, textCnt.get(node) + textCnt.get(childNode));
                            linkCnt.put(node, linkCnt.get(node) + linkCnt.get(childNode));
                        }
                    }
                    System.out.println("Element: " + e.tag());
                    System.out.println("textCnt: " + textCnt.get(node));
                    System.out.println("linkCnt: " + linkCnt.get(node));
                } else if (node instanceof TextNode) {
                    TextNode t = (TextNode) node;
                    StringTokenizer tokenizer = new StringTokenizer(t.getWholeText());
                    textCnt.put(node, (double) tokenizer.countTokens());
                    linkCnt.put(node, 0.0);
                    System.out.println("TextNode: " + t.text());
                    System.out.println("textCnt: " + textCnt.get(node));
                    System.out.println("linkCnt: " + linkCnt.get(node));
                } else {
                    System.out.println("Illegal Node: " + node.toString());
                    return;
                }
                double nodeBasicScore = (textCnt.get(node) - linkCnt.get(node)) / textCnt.get(node);
                double nodeWeightedScore = weightRatio*nodeBasicScore + weightText*(textCnt.get(node)/pageText) ;
                System.out.println("basicScore: " + nodeBasicScore);
                System.out.println("weightedScore: " + nodeWeightedScore);
                basicScore.put(node, nodeBasicScore);
                System.out.println();
            }
        });
    }
}
