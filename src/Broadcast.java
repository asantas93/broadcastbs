import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.SwingUtilities;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;


public class Broadcast {
	
	private static int silenceTimer = 0;
	private static final String TWITTER_HANDLE = "@BroadcastBS";

	public static void main(String[] args) {
	    ArrayList<Long> mentionIds = new ArrayList<Long>();
		while (true) {
	        try {

                // sleep for 60 secs if hour is between from and to
                // should implement sleeping until to when I'm not lazy
                int from = 23, to = 10;
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                int t = c.get(Calendar.HOUR_OF_DAY);
                if (to > from && t >= from && t <= to
                    || to < from && (t >= from || t <= to)) {
                    try {
                        Thread.sleep(60000);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                
	            // gets Twitter instance with default credentials
	            Twitter twitter = new TwitterFactory().getInstance();
	            List<Status> mentions = twitter.getMentionsTimeline();
	            for (Status mention : mentions) {
	            	mentionIds.add(mention.getId());
	            }
	            while (true) { // check mentions every 60 seconds
	            	try {
	            	    Thread.sleep(60000); // wait 60 seconds
	            	} catch(InterruptedException ex) {
	            	    Thread.currentThread().interrupt();
	            	}
	            	if (silenceTimer > 0) {
	            		System.out.println("Silenced for " + silenceTimer + " more minutes");
	            		silenceTimer--;
	            	}
	            	updateMentions(mentionIds);
	            }	            
	        } catch (TwitterException te) {
	            te.printStackTrace();
	            System.out.println("Failed to get timeline: " + te.getMessage());
	            System.out.println("Trying again in 60 seconds");
	            try {
            	    Thread.sleep(60000); // wait 60 seconds
            	} catch(InterruptedException ex) {
            	    Thread.currentThread().interrupt();
            	}
	        }
		}
    }
    
    private static void updateMentions(ArrayList<Long> mentionIds) {
    	try {
            Twitter twitter = new TwitterFactory().getInstance();
            List<Status> mentions = twitter.getMentionsTimeline();
            for (int i = mentions.size() - 1; i >=0; i--) {
            	Status mention = mentions.get(i);
            	if (!mentionIds.contains(mention.getId())) {
                	if (mention.getText().toLowerCase().contains("\\shutup")) {
                		silenceTimer = 60;
                		System.out.println("User @" + mention.getUser().getScreenName() + " has silenced BroadcastBS for an hour.");
                    	mentionIds.add(mention.getId());
                	} else if (silenceTimer == 0) {
	                	mentionIds.add(mention.getId());
	                	System.out.println("Announcing @" + 
	                			mention.getUser().getScreenName() + 
	                			" - " + mention.getText());
	            		try {
	            			announceTweet(mention.getText());
	            			System.out.println("Success");
	            		} catch (Exception e) {
	            			System.out.println("Failed");
	            			e.printStackTrace();
	            		}
                	}
            	}
            }            
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
        }
    }
    
    private static void announceTweet(String tweet) throws Exception {

     	String language = getLanguage(tweet);
    	URL url = new URL("http://api.voicerss.org/?"
    			+ "KEY=b1a362bc35014e9c9dcd8d3536aac7ad"
    			+ "&SRC=" + formatTweet(tweet)	
    			+ "&HL=" + language
    			+ "&C=WAV"
    			+ "&F=48khz_16bit_stereo");
        playAudioStream(AudioSystem.getAudioInputStream(url));        
    }
    
    private static String getLanguage(String tweet) {
    	tweet = tweet.toLowerCase();
    	if (tweet.contains("#spanish"))
    		return "es-es";
    	else if (tweet.contains("#french"))
    		return "fr-fr";
    	else if (tweet.contains("#chinese"))
    		return "ja-jp";
    	else if (tweet.contains("#german"))
    		return "de-de";
    	else return "en-gb";
    }
	
	private static String formatTweet(String tweet) {
		System.out.println("Original Tweet: " + tweet);
		// remove @BroadcastBS
		int index = tweet.toLowerCase().indexOf(TWITTER_HANDLE.toLowerCase());
		tweet = index == 0 ? tweet.substring(0, index) + 
				tweet.substring(index + TWITTER_HANDLE.length(), tweet.length()) : tweet;
		// convert camel case to spaces
        tweet = tweet.replaceAll("([a-z])([A-Z])", "$1%20$2");
        // convert spaces to %20
        tweet = tweet.replaceAll(" ", "%20");
        // remove new lines
        tweet = tweet.replaceAll("\n", "%20");
        // convert @ to "at"
        tweet = tweet.replaceAll("@", "at%20");
        // remove illegal characters
        tweet = tweet.replaceAll(";", "");
		// remove language
		tweet = tweet.replace("#french", "").replace("#spanish", "").replace("#chinese", "").replace("#german", "");
		// convert # to "hash tag "
		tweet = tweet.replaceAll("#", "hash%20tag%20");
		System.out.println("Formatted Tweet: " + tweet);
		return tweet;
	}
	
	private static void playAudioStream(AudioInputStream stream) throws LineUnavailableException, IOException {
		DataLine.Info info = new DataLine.Info (Clip.class, stream.getFormat());
        final Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(stream);
        clip.setFramePosition(0);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                clip.start();
        		try {
        			long duration = (long) (1000 * clip.getFrameLength() / clip.getFormat().getFrameRate());
            	    Thread.sleep(duration); // wait until clip is done playing
            	} catch(InterruptedException ex) {
            	    Thread.currentThread().interrupt();
            	}
            }
        });
	}
}
