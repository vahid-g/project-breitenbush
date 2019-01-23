package core;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainScanner {

	private static Logger LOGGER = Logger.getLogger(MainScanner.class.getName());

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private String username;

	private String pwd;

	private String from;

	private String to;

	public MainScanner(String username, String password, String from, String to) {
		this.username = username;
		this.pwd = password;
		this.from = from;
		this.to = to;
	}

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
				int dayPointer = 0;
				StringBuilder buffer = new StringBuilder();
				while (dayPointer++ < 60) {
					Element tr = tableRows.get(dayPointer++);
					Elements tableData = tr.getElementsByTag("td");
					String day = tableData.get(0).text();
					String date = tableData.get(1).text();
					if (date.contains("W")) {
						continue;
					}
					if (day.equals("Fri")) {
						tr = tableRows.get(dayPointer++);
						Elements satData = tr.getElementsByTag("td");
						tr = tableRows.get(dayPointer);
						Elements sunData = tr.getElementsByTag("td");
						boolean friCabin = tableData.get(3).attr("class").contains(" available");
						boolean friShared = tableData.get(3).attr("class").contains(" shared");
						boolean friLodge = tableData.get(4).attr("class").contains(" available");
						boolean satCabin = satData.get(3).attr("class").contains(" available");
						boolean satShared = satData.get(3).attr("class").contains(" shared");
						boolean satLodge = tableData.get(4).attr("class").contains(" available");
						boolean sunCabin = sunData.get(3).attr("class").contains(" available");
						boolean sunShared = sunData.get(3).attr("class").contains(" shared");
						boolean sunLodge = tableData.get(4).attr("class").contains(" available");
						// cabins
						if (friCabin && !satCabin) {
							buffer.append(date + " " + day + " non-bath cabin is available\n");
						} else if (friShared && !satShared) {
							buffer.append(date + " " + day + " non-bath shared is available\n");
						}
						if (friCabin && satCabin) {
							buffer.append(date + " Fri-Sat non-bath cabin is available\n");
						} else if (friShared && satShared) {
							buffer.append(date + " Fri-Sat non-bath shared is available\n");
						}
						if (satCabin && !sunCabin) {
							buffer.append(date + " Sat-Sun non-bath cabin is available\n");
						} else if (satShared && !sunShared) {
							buffer.append(date + " Sat-Sun non-bath shared is available\n");
						}

						// lodge room
						if (friLodge && !satLodge) {
							buffer.append(date + " " + day + " lodge-room is available\n");
						}
						if (friLodge && satLodge) {
							buffer.append(date + " Fri-Sat lodge-room is available\n");
						}
						if (satLodge && !sunLodge) {
							buffer.append(date + " Sat lodge-room is available\n");
						}
					}
				}
				if (buffer.length() > 1) {
					LOGGER.log(Level.INFO, "emailing the results..");
					System.out.println(buffer.toString());
					// sendEmailNotif(buffer.toString());
					LOGGER.log(Level.INFO, "email sent.");
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

	public void runSingleDayChecker(String targetDate) {
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
				int dayPointer = 0;
				StringBuilder buffer = new StringBuilder();
				while (dayPointer++ < 60) {
					Element tr = tableRows.get(dayPointer++);
					Elements tableData = tr.getElementsByTag("td");
					String day = tableData.get(0).text();
					String date = tableData.get(1).text();
					if (date.contains("W")) {
						continue;
					}
					if (date.equals(targetDate)) {
						LOGGER.log(Level.INFO, "Found target date");
						tr = tableRows.get(dayPointer++);
						tr = tableRows.get(dayPointer);
						boolean fancyCabin = tableData.get(2).attr("class").contains(" available");
						boolean sharedFancyCabin = tableData.get(2).attr("class").contains(" shared");
						boolean cabin = tableData.get(3).attr("class").contains(" available");
						boolean shared = tableData.get(3).attr("class").contains(" shared");
						boolean lodge = tableData.get(4).attr("class").contains(" available");
						if (fancyCabin || sharedFancyCabin) {
							buffer.append(date + " " + day + " some fancy cabin is available\n");
						}
						if (cabin && !shared) {
							buffer.append(date + " " + day + " cabin is available\n");
						}
						if (cabin && shared) {
							buffer.append(date + " " + day + " shared cabin is available\n");
						}
						if (lodge) {
							buffer.append(date + " " + day + " lodge room is available\n");
						}
					}
				}
				if (buffer.length() > 1) {
					LOGGER.log(Level.INFO, "emailing the results..");
					System.out.println(buffer.toString());
					sendEmailNotif(buffer.toString());
					LOGGER.log(Level.INFO, "email sent.");
				}
			}
		};
		final ScheduledFuture<?> breitHandle = scheduler.scheduleAtFixedRate(checker, 0, 2, TimeUnit.HOURS);
		scheduler.schedule(new Runnable() {
			public void run() {
				breitHandle.cancel(true);
			}
		}, 24, TimeUnit.HOURS);
	}

	public void runLodgeRoomChecker() {
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
				int dayPointer = 0;
				StringBuilder buffer = new StringBuilder();
				while (dayPointer++ < 60) {
					Element tr = tableRows.get(dayPointer);
					Elements tableData = tr.getElementsByTag("td");
					String day = tableData.get(0).text();
					String date = tableData.get(1).text();
					if (date.contains("W")) {
						continue;
					}
					if (day.equals("Fri") || day.equals("Sat") || day.equals("Sun")) {
						if (tableData.get(4).attr("class").contains(" available")) {
							buffer.append(date + " " + day + " lodge room is available\n");
						}
					}
				}
				if (buffer.length() > 1) {
					LOGGER.log(Level.INFO, "emailing the results..");
					System.out.println(buffer.toString());
					// sendEmailNotif(buffer.toString());
					LOGGER.log(Level.INFO, "email sent.");
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

	public void sendEmailNotif(String text) {
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", "smtp.mail.yahoo.com");
		properties.setProperty("mail.smtp.port", "465");
		properties.setProperty("mail.smtp.auth", "true");
		properties.setProperty("mail.smtp.starttls.enable", "true");
		properties.setProperty("mail.smtp.ssl.enable", "true");
		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, pwd);
			}
		};
		Session session = Session.getInstance(properties, auth);
		session.setDebug(true);
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject("Project Breitenbush Notification");
			message.setText(text);
			Transport transport = session.getTransport("smtp");
			transport.connect(username, pwd);
			transport.sendMessage(message, message.getAllRecipients());
		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		MainScanner ms = new MainScanner(args[0].replace("\"", ""), args[1].replace("\"", ""),
				args[2].replace("\"", ""), args[3].replace("\"", ""));
		// ms.runTheChecker();
		// ms.runSingleDayChecker("1-19-19");
		ms.runLodgeRoomChecker();
	}

}
