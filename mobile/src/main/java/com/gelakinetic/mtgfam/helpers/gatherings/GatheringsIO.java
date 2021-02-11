/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.gatherings;

import android.app.Activity;
import android.util.Xml;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class is full of static methods to do IO for gatherings. The filesDir is a parameter everywhere since it is
 * gotten with getActivity().getFilesDir(), which cannot be called from a static context
 */
@SuppressWarnings("SpellCheckingInspection")
public class GatheringsIO {
    final private static String FOLDER_PATH = "Gatherings";

    /**
     * Returns the number of gathering .xml files in the given directory
     *
     * @param filesDir The absolute path to the directory on the filesystem where files created with
     *                 openFileOutput(String, int) are stored.
     * @return The number of gatherings .xml files
     */
    public static int getNumberOfGatherings(File filesDir) {
        File path = new File(filesDir, FOLDER_PATH);
        if (!path.exists()) {
            return 0;
        }

        File[] gatheringList = path.listFiles();
        assert gatheringList != null;
        return gatheringList.length;
    }

    /**
     * Returns an ArrayList of Strings of the names of gatherings in filesDir
     *
     * @param filesDir The absolute path to the directory on the filesystem where files created with
     *                 openFileOutput(String, int) are stored.
     * @return All the names of the gatherings in filesDir
     */
    public static ArrayList<String> getGatheringFileList(File filesDir) {
        ArrayList<String> returnList = new ArrayList<>();

        File path = new File(filesDir, FOLDER_PATH);
        if (!path.exists()) {
            return returnList;
        }

        File[] gatheringList = path.listFiles();
        assert gatheringList != null;

        for (File aGatheringList : gatheringList) {
            returnList.add(aGatheringList.getName());
        }

        return returnList;
    }

    /**
     * Write the given data to a new gatherings .xml file
     *
     * @param _players       An ArrayList of player data, which includes names and starting life
     * @param _gatheringName The name of the gathering to write
     * @param _displayMode   The display mode of this gathering (normal, compact, or commander)
     * @param filesDir       The absolute path to the directory on the filesystem where files created with
     *                       openFileOutput(String, int) are stored.
     */
    public static void writeGatheringXML(ArrayList<GatheringsPlayerData> _players, String _gatheringName,
                                         int _displayMode, File filesDir) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss", Locale.ENGLISH);
        String fileName = sdf.format(date);
        writeGatheringXML(fileName, _players, _gatheringName, _displayMode, filesDir);
    }

    /**
     * Write the given data to a new gatherings .xml file
     *
     * @param _fileName      The name of the gatherings xml file to write, usually the date to the second
     * @param _players       An ArrayList of player data, which includes names and starting life
     * @param _gatheringName The name of the gathering to write
     * @param _displayMode   The display mode of this gathering (normal, compact, or commander)
     * @param filesDir       The absolute path to the directory on the filesystem where files created with
     *                       openFileOutput(String, int) are stored.
     */
    private static void writeGatheringXML(String _fileName, ArrayList<GatheringsPlayerData> _players,
                                          String _gatheringName, int _displayMode, File filesDir) {
        String dataXML;

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);

            serializer.startTag("", "gathering");
            serializer.startTag("", "name");
            serializer.text(_gatheringName);
            serializer.endTag("", "name");

            serializer.startTag("", "displaymode");
            serializer.text(String.valueOf(_displayMode));
            serializer.endTag("", "displaymode");

            serializer.startTag("", "players");

            for (GatheringsPlayerData player : _players) {

                String name = player.mName;

                String life = String.valueOf(player.mStartingLife);
                if (life.equals(""))
                    life = "0";

                serializer.startTag("", "player");

                serializer.startTag("", "name");
                serializer.text(name);
                serializer.endTag("", "name");

                serializer.startTag("", "startinglife");
                serializer.text(life);
                serializer.endTag("", "startinglife");

                serializer.endTag("", "player");
            }
            serializer.endTag("", "players");
            serializer.endTag("", "gathering");
            serializer.endDocument();

            dataXML = writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            File path = new File(filesDir, FOLDER_PATH);
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    throw new FileNotFoundException("Folders not made");
                }
            }

            File file = new File(path, _fileName + ".xml");

            BufferedWriter out = new BufferedWriter(new FileWriter(file));

            out.write(dataXML);
            out.close();
        } catch (IOException e) {
            /* eat it */
        }
    }

    /**
     * Read a gathering xml file and return a Gathering object
     *
     * @param _gatheringFileName The name of the gathering file
     * @param filesDir           The absolute path to the directory on the filesystem where files created with
     *                           openFileOutput(String, int) are stored.
     * @return A Gathering object containing players and a default display mode
     */
    public static Gathering ReadGatheringXML(String _gatheringFileName, File filesDir) {
        File path = new File(filesDir, FOLDER_PATH);
        File gathering = new File(path, _gatheringFileName);

        return ReadGatheringXML(gathering);
    }

    /**
     * Read a gathering xml file and return a Gathering object
     *
     * @param _gatheringFile A File object for the gathering
     * @return A Gathering object containing players and a default display mode
     */
    private static Gathering ReadGatheringXML(File _gatheringFile) {
        ArrayList<GatheringsPlayerData> playerList = new ArrayList<>();
        Document dom;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(_gatheringFile);
        } catch (ParserConfigurationException | IOException | SAXException pce) {
            return new Gathering(playerList, 0);
        }

        if (dom == null)
            return new Gathering(playerList, 0);

        Element docEle = dom.getDocumentElement();

        NodeList nl = docEle.getElementsByTagName("player");
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {

                Element el = (Element) nl.item(i);

                Element name = (Element) el.getElementsByTagName("name").item(0);
                String customName;
                customName = name.getChildNodes().item(0).getNodeValue();

                Element life = (Element) el.getElementsByTagName("startinglife").item(0);
                String sLife = life.getChildNodes().item(0).getNodeValue();
                int startingLife = Integer.parseInt(sLife);

                GatheringsPlayerData player = new GatheringsPlayerData();
                player.mName = customName;
                player.mStartingLife = startingLife;

                playerList.add(player);
            }
        }

        int displayMode;
        Element mode = (Element) docEle.getElementsByTagName("displaymode").item(0);
        if (mode != null) {
            String sMode = mode.getChildNodes().item(0).getNodeValue();
            displayMode = Integer.parseInt(sMode);
        } else {
            displayMode = 0;
        }

        return new Gathering(playerList, displayMode);
    }

    /**
     * Read just the name of a gathering from a gathering xml file
     *
     * @param _gatheringFileName The name of the gathering file to read from
     * @param filesDir           The absolute path to the directory on the filesystem where files created with
     *                           openFileOutput(String, int) are stored.
     * @return The name of this gathering
     */
    public static String ReadGatheringNameFromXML(String _gatheringFileName, File filesDir) {
        File path = new File(filesDir, FOLDER_PATH);
        File gathering = new File(path, _gatheringFileName);

        return ReadGatheringNameFromXML(gathering);
    }

    /**
     * Read just the name of a gathering from a gathering xml file
     *
     * @param _gatheringFile A File object for the gathering
     * @return The name of this gathering
     */
    private static String ReadGatheringNameFromXML(File _gatheringFile) {
        Document dom;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(_gatheringFile);
        } catch (ParserConfigurationException | IOException | SAXException pce) {
            return null;
        }

        if (dom == null) {
            return null;
        }

        Element docEle = dom.getDocumentElement();

        Element name = (Element) docEle.getElementsByTagName("name").item(0);

        if (name.getChildNodes().item(0) == null) {
            return "";
        }

        return name.getChildNodes().item(0).getNodeValue();
    }

    /**
     * Delete a gathering by file name
     *
     * @param fileName The file name of the gathering to delete
     * @param filesDir The absolute path to the directory on the filesystem where files created with
     *                 openFileOutput(String, int) are stored.
     */
    public static void DeleteGathering(String fileName, File filesDir, Activity activity) {
        File path = new File(filesDir, FOLDER_PATH);
        File gatheringFile = new File(path, fileName);
        if (!gatheringFile.delete()) {
            SnackbarWrapper.makeAndShowText(activity, fileName + " " + activity.getString(R.string.not_deleted), SnackbarWrapper.LENGTH_LONG);
        }
    }

    /**
     * Delete a gathering by gathering name (must be read from xml file)
     *
     * @param _name    The name of the gathering to delete
     * @param filesDir The absolute path to the directory on the filesystem where files created with
     *                 openFileOutput(String, int) are stored.
     */
    public static void DeleteGatheringByName(String _name, File filesDir, Activity activity) {
        for (String fileName : getGatheringFileList(filesDir)) {
            if (_name.equals(ReadGatheringNameFromXML(fileName, filesDir))) {
                DeleteGathering(fileName, filesDir, activity);
            }
        }
    }
}
