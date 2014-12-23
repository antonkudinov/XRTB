package com.xrtb.tests;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Jedis;

import com.xrtb.bidder.CampaignSelector;
import com.xrtb.bidder.RTBServer;
import com.xrtb.common.Campaign;
import com.xrtb.common.Configuration;
import com.xrtb.common.HttpPostGet;
import com.xrtb.common.Utils;
import com.xrtb.pojo.Bid;
import com.xrtb.pojo.BidRequest;
import com.xrtb.pojo.BidResponse;

public class TestBidRequests {
	static Thread thread;
	static String fake;
	static RTBServer server;
	@BeforeClass
	public static void setup() {
		Configuration c = Configuration.getInstance();
		c.clear();
		try {
			c.initialize("./Campaigns/payday.json");
			server = new RTBServer();
			Thread.sleep(5000);
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@AfterClass
	public static void testCleanup() {
		if (server != null)
			server.halt();
	}
	
	@Test 
	public void respondWithNoBid() {
		HttpPostGet http = new HttpPostGet();
		
		try {
			CampaignSelector.getInstance().clear();
			String s = Utils.readFile("./SampleBids/nexage.txt");
			try {
				s = http.sendPost("http://" + Config.testHost + "/rtb/bids/nexage", s);
			} catch (Exception error) {
				fail("Can't connect to test host: " + Config.testHost);
			}
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;
			rootNode = mapper.readTree(s);
			JsonNode node = rootNode.path("reason");
			String str = node.getTextValue();
			assertEquals(str,"No campaigns loaded");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void bidRequestTwoCampaigns() {
		
	}


	@Test
	public void nobidWithReasonWithNoCamps()   {
		HttpPostGet http = new HttpPostGet();
	
		try {
			CampaignSelector.getInstance().clear();
			String s = Utils.readFile("./SampleBids/nexage.txt");
			try {
				s = http.sendPost("http://" + Config.testHost + "/rtb/bids/nexage", s);
			} catch (Exception error) {
				fail("Can't connect to test host: " + Config.testHost);
			}
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;
			rootNode = mapper.readTree(s);
			JsonNode node = rootNode.path("reason");
			String str = node.getTextValue();
			assertEquals(str,"No campaigns loaded");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}  
	
	
	@Test
	public void nobidWithReasonWithNoCampMatch() throws Exception  {
		CampaignSelector.getInstance().clear();
		CampaignSelector.getInstance().add(new Campaign());
		HttpPostGet http = new HttpPostGet();
	
		try {
			String s = "";
			
			try {
				s = http.sendPost("http://" + Config.testHost + "/rtb/bids/nexage", "{\"id\":\"123\"}");
			} catch (Exception error) {
				fail("Can't connect to the test host: " + Config.testHost);
			}
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = null;
			rootNode = mapper.readTree(s);
			System.out.println(s);
			JsonNode node = rootNode.path("id");
			String str = node.getTextValue();
			assertNotNull(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 
	
	@Test
	public void sendCrapRequest() throws Exception {
		CampaignSelector.getInstance().clear();
		HttpPostGet http = new HttpPostGet();
	
		try {
			String s = "";
			RTBServer.percentage = 100;
			RTBServer.nobid = 0;
			try {
				s = http.sendPost("http://" + Config.testHost + "/rtb/bids/nexage", "this is junk");
			} catch (Exception error) {
				fail("Can't connect to test server: " + Config.testHost);
			}
			assertTrue(s.contains("JsonParseException"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	} 
	
	@Test
	public void sendCrapTarget()  throws Exception {
		CampaignSelector.getInstance().clear();
		HttpPostGet http = new HttpPostGet();
	
		try {
			String s = "";
			RTBServer.percentage = 100;
			RTBServer.nobid = 0;
			try {
				s = http.sendPost("http://" + Config.testHost + "/rtb/bids/BAD", "this is junk");
			} catch (Exception error) {
				fail("Can't connect to test server: "  + Config.testHost);
			}
			System.out.println("---------->"+s);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}  
	
	@Test
	public void testWinProcessing() throws Exception  {
		HttpPostGet http = new HttpPostGet();
		String s = "";
		Jedis cache = new Jedis("localhost");
		cache.connect();
		cache.del("35c22289-06e2-48e9-a0cd-94aeb79fab43");
		// Make the bid
		Configuration.getInstance().initialize("./Campaigns/payday.json");
		server.halt();
		Thread.sleep(1000);
		server= new RTBServer();
		Thread.sleep(1000);
		
		s = Utils.readFile("./SampleBids/nexage.txt");
		try {
			s = http.sendPost("http://" + Config.testHost + "/rtb/bids/nexage", s);
		} catch (Exception error) {
			fail("Can't connect to test host: " + Config.testHost);
		}
		Bid bid = null;
		try {
			bid = new Bid(s);
		} catch (Exception error) {
			error.printStackTrace();
			fail();
		}
		
		// Now retrieve the bid information from the cache
		Map m = cache.hgetAll(bid.id);
		assertTrue(!m.isEmpty());
		String price = (String)m.get("PRICE");
		assertTrue(price.equals("5.0"));
		
		// Send the WIN notification
		try {
			s = http.sendPost(bid.nurl, "");
		} catch (Exception error) {
			error.printStackTrace();
			fail();
		}
		// Analyze the results
		System.out.println(s);
		
		// Check to see the bid was removed from the cache
		m = cache.hgetAll(bid.id);
		assertTrue(m.isEmpty());
		
	}
}