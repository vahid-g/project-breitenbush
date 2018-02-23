package core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainScanner {

	private static Logger LOGGER = Logger.getLogger(MainScanner.class.getName());

	public static void main(String[] args) throws Exception {
		
		LOGGER.log(Level.INFO, "loading the content..");
//		URL url = new URL("https://guestui.breitenbush.com/");
//		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//		StringBuilder contentBuilder = new StringBuilder();
//		String inputLine;
//		while ((inputLine = in.readLine()) != null)
//			contentBuilder.append(inputLine);
//		in.close();
		String webPage = "https://guestui.breitenbush.com/";
		String html = Jsoup.connect(webPage).get().html();
		LOGGER.log(Level.INFO, "loading done.");
		
		 Document doc = Jsoup.parse(html);
		 System.out.println(html);
		 Elements trs = doc.getElementsByTag("tr");
		 for (Element e : trs) {
			 System.out.println(trs.text());
			 break;
		 }
		 
         System.out.printf("");

	}

}
