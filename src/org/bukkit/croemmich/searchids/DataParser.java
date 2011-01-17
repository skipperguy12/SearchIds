package org.bukkit.croemmich.searchids;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DataParser {
	
	protected static final Logger log = Logger.getLogger("Minecraft");
	
	public DataParser() {
		
	}
	
	public TreeMap<Integer, String> search(String query) {
		return search(query, "decimal");
	}
	
	public TreeMap<Integer, String> search(String query, String base) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DataHandler handler = new DataHandler();
			handler.setPattern(Pattern.compile(".*?"+Pattern.quote(query)+".*", Pattern.CASE_INSENSITIVE));
			saxParser.parse(SearchIds.dataXml, handler);
			return handler.getData();
		} catch (Exception e) {
		}
		return null;
	}

	class DataHandler extends DefaultHandler {
		boolean data = false;
		boolean blocks = false;
		boolean items = false;
		boolean item = false;
		
		private Pattern pattern;
		
		private TreeMap<Integer, String> hm = new TreeMap<Integer, String>();

		public void setPattern (Pattern pattern) {
			this.pattern = pattern;
		}
		
		public TreeMap<Integer, String> getData() {
			return hm;
		}
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("DATA")) {
				data = true;
			}

			if (qName.equalsIgnoreCase("BLOCKS")) {
				blocks = true;
			}

			if (qName.equalsIgnoreCase("ITEMS")) {
				items = true;
			}

			if (qName.equalsIgnoreCase("ITEM")) {
				item = true;
				
				if (SearchIds.searchType.equalsIgnoreCase("all") || 
					(SearchIds.searchType.equalsIgnoreCase("blocks") && blocks == true) ||
					(SearchIds.searchType.equalsIgnoreCase("items") && items  == true)) {
					
					String name = attributes.getValue("name");
					String value = attributes.getValue("dec"); 

					if (name != null && value != null) {
						if (pattern.matcher(name).matches()) {
							hm.put(Integer.valueOf(value) , name);
						}
					} else {
						log.severe("Name or value is null on an item");
					}
				}
				
			}
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equalsIgnoreCase("DATA")) {
				data = false;
			}

			if (qName.equalsIgnoreCase("BLOCKS")) {
				blocks = false;
			}

			if (qName.equalsIgnoreCase("ITEMS")) {
				items = false;
			}

			if (qName.equalsIgnoreCase("ITEM")) {
				item = false;
			}
		}
	}
}