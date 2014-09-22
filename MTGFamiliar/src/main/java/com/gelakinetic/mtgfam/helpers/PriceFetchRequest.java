package com.gelakinetic.mtgfam.helpers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.SpiceRequest;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class extends SpiceRequest for the type PriceInfo, and is used to fetch and cache price info asynchronously
 */
public class PriceFetchRequest extends SpiceRequest<PriceInfo> {

	private final String mCardName;
	private final String mSetCode;
	private final int mMultiverseID;
	private String mCardNumber;

	/**
	 * Default constructor
	 *
	 * @param cardName     The name of the card to look up
	 * @param setCode      The set code (not TCG name) of this card's set
	 * @param cardNumber   The collector's number of the card to look up
	 * @param multiverseID The multiverse ID of the card to look up
	 */
	public PriceFetchRequest(String cardName, String setCode, String cardNumber, int multiverseID) {
		super(PriceInfo.class);
		this.mCardName = cardName;
		this.mSetCode = setCode;
		this.mCardNumber = cardNumber;
		this.mMultiverseID = multiverseID;
	}

	/**
	 * This function takes a string of XML information and parses it into a Document object in order to extract prices
	 *
	 * @param xml The String of XML
	 * @return a Document describing the XML
	 * @throws ParserConfigurationException thrown by factory.newDocumentBuilder()
	 * @throws SAXException                 thrown by  builder.parse()
	 * @throws IOException                  thrown by  builder.parse()
	 */
	private static Document loadXMLFromString(String xml) throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	/**
	 * This runs as a service, builds the TCGplayer.com URL, fetches the data, and parses the XML
	 *
	 * @return a PriceInfo object with all the prices
	 * @throws SpiceException If anything goes wrong with the database, URL, or connection, this will be thrown
	 */
	@SuppressWarnings("SpellCheckingInspection")
	@Override
	public PriceInfo loadDataFromNetwork() throws SpiceException {
		boolean isAscending = true;
		int retry = 4; /* try the fetch four times, once with accent marks and again without if it fails */
		/* then the same for multicard ordering */
		SpiceException exception = null; /* Save the exception during while loops */
		SQLiteDatabase database = DatabaseManager.getInstance().openDatabase(false);
		while (retry > 0) {
			try {
				/* If the card number wasn't given, figure it out */
				if (mCardNumber == null) {
					Cursor c = CardDbAdapter.fetchCardByNameAndSet(mCardName, mSetCode, CardDbAdapter.allData, database);
					mCardNumber = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
					c.close();
				}

				/* Get the TCGplayer.com set name, why can't everything be consistent? */
				String tcgName = CardDbAdapter.getTcgName(mSetCode, database);
				/* Figure out the tcgCardName, which is tricky for split cards */
				String tcgCardName;
				int multiCardType = CardDbAdapter.isMultiCard(mCardNumber, mSetCode);
				if ((multiCardType == CardDbAdapter.TRANSFORM) && mCardNumber.contains("b")) {
					tcgCardName = CardDbAdapter.getTransformName(mSetCode, mCardNumber.replace("b", "a"), database);
				}
				else if (mMultiverseID == -1 && (multiCardType == CardDbAdapter.SPLIT ||
						multiCardType == CardDbAdapter.FUSE)) {
					int multiID = CardDbAdapter.getSplitMultiverseID(mCardName, mSetCode, database);
					if (multiID == -1) {
						throw new FamiliarDbException(null);
					}
					tcgCardName = CardDbAdapter.getSplitName(multiID, isAscending, database);
				}
				else if (mMultiverseID != -1 && (multiCardType == CardDbAdapter.SPLIT ||
						multiCardType == CardDbAdapter.FUSE)) {
					tcgCardName = CardDbAdapter.getSplitName(mMultiverseID, isAscending, database);
				}
				else {
					tcgCardName = mCardName;
				}

				/* Retry with accent marks removed */
				if (retry < 3) {
					tcgCardName = CardDbAdapter.removeAccentMarks(tcgCardName);
				}

				/* Set up retries for multicard ordering */
				if (multiCardType != CardDbAdapter.NOPE) {
					/* Next time try the other order */
					switch (retry) {
						case 4:
							isAscending = false;
							break;
						case 3:
							isAscending = true;
							break;
						case 2:
							isAscending = false;
							break;
					}
				}
				/* If it isnt a multicard, don't bother */
				else if (retry == 4) {
					retry = 2;
				}

				/* Build the URL */
				URL priceUrl = new URL("http://partner.tcgplayer.com/x3/phl.asmx/p?pk=MTGFAMILIA&s=" +
						URLEncoder.encode(tcgName.replace(Character.toChars(0xC6)[0] + "", "Ae"), "UTF-8") + "&p=" +
						URLEncoder.encode(tcgCardName.replace(Character.toChars(0xC6)[0] + "", "Ae"), "UTF-8")
				);

				/* Fetch the information from the web */
				HttpURLConnection urlConnection = (HttpURLConnection) priceUrl.openConnection();
				String result = IOUtils.toString(urlConnection.getInputStream());
				urlConnection.disconnect();

				/* Parse the XML */
				Document document = loadXMLFromString(result);
				Element element = document.getDocumentElement();

				try {
					PriceInfo pi = new PriceInfo();
					pi.mLow = Double.parseDouble(getString("lowprice", element));
					pi.mAverage = Double.parseDouble(getString("avgprice", element));
					pi.mHigh = Double.parseDouble(getString("hiprice", element));
					pi.mFoilAverage = Double.parseDouble(getString("foilavgprice", element));
					pi.mUrl = getString("link", element);

					/* Some cards, like FTV, only have a foil price. This fixed problems down the road */
					if (pi.mLow == 0 && pi.mAverage == 0 && pi.mHigh == 0 && pi.mFoilAverage != 0) {
						pi.mLow = pi.mFoilAverage;
						pi.mAverage = pi.mFoilAverage;
						pi.mHigh = pi.mFoilAverage;
					}
					return pi;
				} catch (NumberFormatException error) {
					exception = new SpiceException(error.getLocalizedMessage());
				} catch (DOMException e) {
					exception = new SpiceException(e.getLocalizedMessage());
				}
			} catch (FamiliarDbException e) {
				exception = new SpiceException(e.getLocalizedMessage());
			} catch (MalformedURLException e) {
				exception = new SpiceException(e.getLocalizedMessage());
			} catch (IOException e) {
				exception = new SpiceException(e.getLocalizedMessage());
			} catch (ParserConfigurationException e) {
				exception = new SpiceException(e.getLocalizedMessage());
			} catch (SAXException e) {
				exception = new SpiceException(e.getLocalizedMessage());
			}
			retry--;
		}
		DatabaseManager.getInstance().closeDatabase();
		if (exception != null) {
			throw exception;
		}
		else {
			throw new SpiceException("CardNotFound");
		}
	}

	/**
	 * Get a string value out of an Element given a tag name
	 *
	 * @param tagName The name of the XML tag to extract a string from
	 * @param element The Element containing XML information
	 * @return The String in the XML with the corresponding tag
	 */
	String getString(String tagName, Element element) {
		NodeList list = element.getElementsByTagName(tagName);
		if (list != null && list.getLength() > 0) {
			NodeList subList = list.item(0).getChildNodes();

			if (subList != null) {
				String returnValue = "";
				for (int i = 0; i < subList.getLength(); i++) {
					returnValue += subList.item(i).getNodeValue();
				}
				return returnValue;
			}
		}
		return null;
	}
}