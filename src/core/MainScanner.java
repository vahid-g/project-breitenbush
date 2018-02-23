package core;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainScanner {

	private static Logger LOGGER = Logger.getLogger(MainScanner.class.getName());

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public void runTheChecker() {
		final Runnable checker = new Runnable() {
			public void run() {
				LOGGER.log(Level.INFO, "loading the content..");
				String webPage = "https://guestui.breitenbush.com/";
				String html = "";
				try {
					html = Jsoup.connect(webPage).get().html();
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
				LOGGER.log(Level.INFO, "loading done.");

				Document doc = Jsoup.parse(html);
				Elements tableRows = doc.getElementsByTag("tbody").first().getElementsByTag("tr");
				for (Element tr : tableRows) {
					Elements tableData = tr.getElementsByTag("td");
					String day = tableData.get(0).text();
					String date = tableData.get(1).text();
					if (date.contains("W"))
						continue;
					if (day.equals("Fi") || day.equals("Sat")) {
						boolean available = tableData.get(3).attr("class").contains(" available");
						boolean shared = tableData.get(3).attr("class").contains(" shared");
						if (available) {
							System.out.println(date + " " + day + " non-bath cabin is available");
						} else if (shared) {
							System.out.println(date + " " + day + " non-bath is shared-available");
						}
						if (tableData.get(3).attr("class").contains(" available")) {
							System.out.println(date + " " + day + " lodge-room is available");
						}
					}
				}
			}
		};

		final ScheduledFuture<?> breitHandle = scheduler.scheduleAtFixedRate(checker, 0, 12, TimeUnit.HOURS);
		scheduler.schedule(new Runnable() {
			public void run() {
				breitHandle.cancel(true);
			}
		}, 10, TimeUnit.DAYS);
	}

	public static void main(String[] args) throws Exception {
		MainScanner ms = new MainScanner();
		ms.runTheChecker();
	}

}
