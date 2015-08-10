package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
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
    private int mMultiverseID;
    private final Context mContext;
    private String mCardType;
    private String mCardNumber;

    /**
     * Default constructor
     *
     * @param cardName     The name of the card to look up
     * @param setCode      The set code (not TCG name) of this card's set
     * @param cardNumber   The collector's number of the card to look up
     * @param multiverseID The multiverse ID of the card to look up
     */
    public PriceFetchRequest(String cardName, String setCode, String cardNumber, int multiverseID, Context context) {
        super(PriceInfo.class);
        this.mCardName = cardName;
        this.mSetCode = setCode;
        this.mCardNumber = cardNumber;
        this.mMultiverseID = multiverseID;
        this.mContext = context;
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

    private static final int MAX_NUM_RETRIES = 8;

    /**
     * This runs as a service, builds the TCGplayer.com URL, fetches the data, and parses the XML
     *
     * @return a PriceInfo object with all the prices
     * @throws SpiceException If anything goes wrong with the database, URL, or connection, this will be thrown
     */
    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public PriceInfo loadDataFromNetwork() throws SpiceException {
        int retry = MAX_NUM_RETRIES; /* try the fetch up to eight times, for different accent mark & split card combos*/
        /* then the same for multicard ordering */
        SpiceException exception = null; /* Save the exception during while loops */
        SQLiteDatabase database = DatabaseManager.getInstance(mContext, false).openDatabase(false);
        int multiCardType = CardDbAdapter.isMultiCard(mCardNumber, mSetCode);
        while (retry > 0) {
            try {
                /* If the card number wasn't given, figure it out */
                if (mCardNumber == null || mCardType == null || mMultiverseID == -1) {
                    Cursor c = CardDbAdapter.fetchCardByNameAndSet(mCardName, mSetCode, CardDbAdapter.allData, database);

                    if (mCardNumber == null) {
                        mCardNumber = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                    }

                    if (mCardType == null) {
                        mCardType = c.getString(c.getColumnIndex(CardDbAdapter.KEY_TYPE));
                    }

                    if(mMultiverseID == -1) {
                        mMultiverseID = CardDbAdapter.getSplitMultiverseID(mCardName, mSetCode, database);
                        if (mMultiverseID == -1) {
                            c.close();
                            throw new FamiliarDbException(null);
                        }
                    }
                    c.close();
                }

				/* Get the TCGplayer.com set name, why can't everything be consistent? */
                String tcgName = CardDbAdapter.getTcgName(mSetCode, database);
                /* Figure out the tcgCardName, which is tricky for split cards */
                String tcgCardName;

				/* Set up retries for multicard ordering */
                if (multiCardType != CardDbAdapter.NOPE) {
                    /* Next time try the other order */
                    switch (retry % (MAX_NUM_RETRIES / 2)) {
                        case 0:
                            /* Try just the a side */
                            tcgCardName = CardDbAdapter.getTransformName(mSetCode, mCardNumber.replace("b", "a"), database);
                            break;
                        case 3:
                            /* Try just the b side */
                            tcgCardName = CardDbAdapter.getTransformName(mSetCode, mCardNumber.replace("a", "b"), database);
                            break;
                        case 2:
                            /* Try the combined name in one direction */
                            tcgCardName = CardDbAdapter.getSplitName(mMultiverseID, true, database);
                            break;
                        case 1:
                            /* Try the combined name in the other direction */
                            tcgCardName = CardDbAdapter.getSplitName(mMultiverseID, false, database);
                            break;
                        default:
                            /* Something went wrong */
                            tcgCardName = mCardName;
                            break;
                    }
                }
                else {
                    /* This isn't a multicard */
                    tcgCardName = mCardName;
                }

				/* Retry with accent marks removed */
                if (retry <= MAX_NUM_RETRIES / 2) {
                    tcgCardName = CardDbAdapter.removeAccentMarks(tcgCardName);
                }

				/* Build the URL */
                URL priceUrl = new URL("http://partner.tcgplayer.com/x3/phl.asmx/p?pk=MTGFAMILIA&s=" +
                        URLEncoder.encode(tcgName.replace(Character.toChars(0xC6)[0] + "", "Ae"), "UTF-8") + "&p=" +
                        URLEncoder.encode(tcgCardName.replace(Character.toChars(0xC6)[0] + "", "Ae"), "UTF-8") +
                        URLEncoder.encode((mCardType.startsWith("Basic Land") ? " (" + mCardNumber + ")" : ""), "UTF-8")
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
                    DatabaseManager.getInstance(mContext, false).closeDatabase(false); /* database close if everything was ok */
                    return pi;
                } catch (NumberFormatException | DOMException error) {
                    exception = new SpiceException(error.getLocalizedMessage());
                }

                /* If this is a single card, skip over a bunch of retry cases */
                if (retry == MAX_NUM_RETRIES && multiCardType == CardDbAdapter.NOPE) {
                    retry = 2;
                }
            } catch (FamiliarDbException | IOException | ParserConfigurationException | SAXException e) {
                exception = new SpiceException(e.getLocalizedMessage());
            }
            retry--;
        }
        DatabaseManager.getInstance(mContext, false).closeDatabase(false); /* database close if something failed */
        if (exception != null) {
            throw exception;
        } else {
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
    private String getString(String tagName, Element element) {
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