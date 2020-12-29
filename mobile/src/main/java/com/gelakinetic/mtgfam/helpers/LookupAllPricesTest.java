/*
 * Copyright 2018 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceFetcher;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class LookupAllPricesTest extends AsyncTask<FamiliarActivity, Void, Void> {

    private static final String DAPT_TAG = "CUBE";
    private int totalElapsedSuccess = 0;
    private int totalSuccess = 0;
    private int totalElapsedFailure = 0;
    private int totalFailure = 0;
//    private final FamiliarDbHandle mHandle = new FamiliarDbHandle();

    private int cubeCardIdx = 0;
    private static final String CUBE[] = {"Giver of Runes",
            "Isamaru, Hound of Konda",
            "Kytheon, Hero of Akros",
            "Mother of Runes",
            "Student of Warfare",
            "Thraben Inspector",
            "Anafenza, Kin-Tree Spirit",
            "Containment Priest",
            "Hero of Precinct One",
            "Remorseful Cleric",
            "Seeker of the Way",
            "Selfless Spirit",
            "Stoneforge Mystic",
            "Thalia, Guardian of Thraben",
            "Tithe Taker",
            "Wall of Omens",
            "Blade Splicer",
            "Brightling",
            "Brimaz, King of Oreskos",
            "Fairgrounds Warden",
            "Flickerwisp",
            "Hallowed Spiritkeeper",
            "Mentor of the Meek",
            "Monastery Mentor",
            "Ranger-Captain of Eos",
            "Recruiter of the Guard",
            "Thalia, Heretic Cathar",
            "Emeria Angel",
            "Hero of Bladehold",
            "Restoration Angel",
            "Sublime Archangel",
            "Angel of Invention",
            "Archangel Avacyn",
            "Baneslayer Angel",
            "Cloudgoat Ranger",
            "Lyra Dawnbringer",
            "Reveillark",
            "Venerated Loxodon",
            "Sun Titan",
            "Elesh Norn, Grand Cenobite",
            "Iona, Shield of Emeria",
            "Gideon Blackblade",
            "Elspeth, Knight-Errant",
            "Gideon, Ally of Zendikar",
            "Serra the Benevolent",
            "Gideon Jura",
            "Elspeth, Sun's Champion",
            "Porcelain Legionnaire",
            "Condemn",
            "Mana Tithe",
            "Path to Exile",
            "Swords to Plowshares",
            "Blessed Alliance",
            "Forsake the Worldly",
            "Unexpectedly Absent",
            "Oust",
            "Council's Judgment",
            "Lingering Souls",
            "Spectral Procession",
            "Armageddon",
            "Day of Judgment",
            "Ravages of War",
            "Wrath of God",
            "Fumigate",
            "Terminus",
            "Approach of the Second Sun",
            "Martial Coup",
            "Entreat the Angels",
            "Land Tax",
            "Legion's Landing",
            "Honor of the Pure",
            "Journey to Nowhere",
            "Banishing Light",
            "Glorious Anthem",
            "History of Benalia",
            "Oblivion Ring",
            "Cast Out",
            "Conclave Tribunal",
            "Faith's Fetters",
            "Parallax Wave",
            "Spear of Heliod",
            "Delver of Secrets",
            "Pteramander",
            "Augur of Bolas",
            "Baral, Chief of Compliance",
            "Fblthp, the Lost",
            "Jace, Vryn's Prodigy",
            "Looter il-Kor",
            "Merfolk Looter",
            "Phantasmal Image",
            "Snapcaster Mage",
            "Stratus Dancer",
            "Thing in the Ice",
            "Arcane Artisan",
            "Bazaar Trademage",
            "Champion of Wits",
            "Deceiver Exarch",
            "Kira, Great Glass-Spinner",
            "Man-o'-War",
            "Nimble Obstructionist",
            "Pestermite",
            "Spellseeker",
            "Vendilion Clique",
            "Wake Thrasher",
            "Glen Elendra Archmage",
            "Murmuring Mystic",
            "Sower of Temptation",
            "Talrand, Sky Summoner",
            "Urza, Lord High Artificer",
            "Venser, Shaper Savant",
            "Whirler Rogue",
            "Meloku the Clouded Mirror",
            "Mulldrifter",
            "Riftwing Cloudskate",
            "Consecrated Sphinx",
            "Frost Titan",
            "Jace Beleren",
            "Narset, Parter of Veils",
            "Jace, the Mind Sculptor",
            "Tamiyo, the Moon Sage",
            "Phyrexian Metamorph",
            "Torrential Gearhulk",
            "Brainstorm",
            "Opt",
            "Counterspell",
            "Cyclonic Rift",
            "Daze",
            "Impulse",
            "Into the Roil",
            "Mana Leak",
            "Miscalculation",
            "Negate",
            "Remand",
            "Remove Soul",
            "Tale's End",
            "Forbid",
            "Force of Negation",
            "Supreme Will",
            "Thirst for Knowledge",
            "Careful Consideration",
            "Cryptic Command",
            "Fact or Fiction",
            "Gifts Ungiven",
            "Force of Will",
            "Mystic Confluence",
            "Repeal",
            "Ponder",
            "Preordain",
            "Serum Visions",
            "Chart a Course",
            "Compulsive Research",
            "Show and Tell",
            "Deep Analysis",
            "Time Warp",
            "Upheaval",
            "Temporal Mastery",
            "Mass Manipulation",
            "Search for Azcanta",
            "Control Magic",
            "Opposition",
            "Treachery",
            "Cryptbreaker",
            "Dread Wanderer",
            "Gravecrawler",
            "Gutterbones",
            "Bloodghast",
            "Dark Confidant",
            "Gatekeeper of Malakir",
            "Kitesail Freebooter",
            "Oona's Prowler",
            "Pack Rat",
            "Vampire Hexmage",
            "Bone Shredder",
            "Drana, Liberator of Malakir",
            "Geralf's Messenger",
            "Hypnotic Specter",
            "Midnight Reaper",
            "Ophiomancer",
            "Rotting Regisaur",
            "Vampire Nighthawk",
            "Bloodline Keeper",
            "Gonti, Lord of Luxury",
            "Kalitas, Traitor of Ghet",
            "Nekrataal",
            "Phyrexian Obliterator",
            "Ravenous Chupacabra",
            "Spawn of Mayhem",
            "Yawgmoth, Thran Physician",
            "Cavalier of Night",
            "Gray Merchant of Asphodel",
            "Shriekmaw",
            "Demonlord Belzenlok",
            "Grave Titan",
            "Massacre Wurm",
            "Tasigur, the Golden Fang",
            "Sheoldred, Whispering One",
            "Griselbrand",
            "Liliana of the Veil",
            "Liliana, the Last Hope",
            "Ob Nixilis Reignited",
            "Liliana, Dreadhorde General",
            "Dark Ritual",
            "Entomb",
            "Fatal Push",
            "Doom Blade",
            "Go for the Throat",
            "Liliana's Triumph",
            "Malicious Affliction",
            "Ultimate Price",
            "Dismember",
            "Hero's Downfall",
            "Makeshift Mannequin",
            "Vraska's Contempt",
            "Murderous Cut",
            "Wretched Confluence",
            "Duress",
            "Inquisition of Kozilek",
            "Reanimate",
            "Thoughtseize",
            "Collective Brutality",
            "Exhume",
            "Hymn to Tourach",
            "Night's Whisper",
            "Buried Alive",
            "Never", // " // Return"
            "Read the Bones",
            "Toxic Deluge",
            "Damnation",
            "Dread Return",
            "Languish",
            "Smiting Helix",
            "Living Death",
            "Unburial Rites",
            "Mind Shatter",
            "Profane Command",
            "Animate Dead",
            "Bitterblossom",
            "Necromancy",
            "Phyrexian Arena",
            "Recurring Nightmare",
            "Whip of Erebos",
            "Brain Maggot",
            "Falkenrath Gorger",
            "Firedrinker Satyr",
            "Goblin Guide",
            "Grim Lavamancer",
            "Monastery Swiftspear",
            "Zurgo Bellstriker",
            "Abbot of Keral Keep",
            "Dismissive Pyromancer",
            "Dreadhorde Arcanist",
            "Goblin Cratermaker",
            "Rix Maadi Reveler",
            "Runaway Steam-Kin",
            "Young Pyromancer",
            "Chandra's Phoenix",
            "Dualcaster Mage",
            "Goblin Rabblemaster",
            "Imperial Recruiter",
            "Legion Warboss",
            "Manic Vandal",
            "Pia Nalaar",
            "Rampaging Ferocidon",
            "Seasoned Pyromancer",
            "Avalanche Riders",
            "Flametongue Kavu",
            "Hazoret the Fervent",
            "Hellrider",
            "Pia and Kiran Nalaar",
            "Rekindling Phoenix",
            "Thunderbreak Regent",
            "Glorybringer",
            "Goblin Dark-Dwellers",
            "Ilharg, the Raze-Boar",
            "Kiki-Jiki, Mirror Breaker",
            "Siege-Gang Commander",
            "Thundermaw Hellkite",
            "Zealous Conscripts",
            "Inferno Titan",
            "Bedlam Reveler",
            "Bogardan Hellkite",
            "Greater Gargadon",
            "Chandra, Acolyte of Flame",
            "Chandra, Torch of Defiance",
            "Koth of the Hammer",
            "Chandra, Awakened Inferno",
            "Burst Lightning",
            "Lightning Bolt",
            "Shivan Fire",
            "Abrade",
            "Ancient Grudge",
            "Fire", // " // Ice"
            "Incinerate",
            "Lightning Strike",
            "Magma Jet",
            "Searing Spear",
            "Char",
            "Stoke the Flames",
            "Through the Breach",
            "Chain Lightning",
            "Faithless Looting",
            "Firebolt",
            "Flame Slash",
            "Lava Coil",
            "Mizzium Mortars",
            "Pyroclasm",
            "Roast",
            "Tormenting Voice",
            "Anger of the Gods",
            "Collective Defiance",
            "Exquisite Firecraft",
            "Fight with Fire",
            "Rift Bolt",
            "Sweltering Suns",
            "Fiery Confluence",
            "Hour of Devastation",
            "Banefire",
            "Devil's Play",
            "Star of Extinction",
            "Bonfire of the Damned",
            "Experimental Frenzy",
            "Outpost Siege",
            "Sneak Attack",
            "Splinter Twin",
            "Arbor Elf",
            "Avacyn's Pilgrim",
            "Birds of Paradise",
            "Elves of Deep Shadow",
            "Elvish Mystic",
            "Fyndhorn Elves",
            "Hexdrinker",
            "Joraga Treespeaker",
            "Llanowar Elves",
            "Noble Hierarch",
            "Den Protector",
            "Devoted Druid",
            "Fauna Shaman",
            "Incubation Druid",
            "Lotus Cobra",
            "Overgrown Battlement",
            "Rattleclaw Mystic",
            "Rofellos, Llanowar Emissary",
            "Sakura-Tribe Elder",
            "Satyr Wayfinder",
            "Scavenging Ooze",
            "Sylvan Caryatid",
            "Wall of Blossoms",
            "Eternal Witness",
            "Jadelight Ranger",
            "Nissa, Vastwood Seer",
            "Ramunap Excavator",
            "Reclamation Sage",
            "Thrashing Brontodon",
            "Tireless Tracker",
            "Wood Elves",
            "Yavimaya Elder",
            "Beast Whisperer",
            "Master of the Wild Hunt",
            "Obstinate Baloth",
            "Oracle of Mul Daya",
            "Polukranos, World Eater",
            "Thrun, the Last Troll",
            "Wickerbough Elder",
            "Acidic Slime",
            "Biogenic Ooze",
            "Deep Forest Hermit",
            "Deranged Hermit",
            "Thragtusk",
            "Whisperwood Elemental",
            "Carnage Tyrant",
            "Greenwarden of Murasa",
            "Primeval Titan",
            "Rampaging Baloths",
            "Avenger of Zendikar",
            "Hornet Queen",
            "Craterhoof Behemoth",
            "Terastodon",
            "Woodfall Primus",
            "Garruk Relentless",
            "Garruk Wildspeaker",
            "Garruk, Primal Hunter",
            "Nissa, Who Shakes the World",
            "Vivien Reid",
            "Birthing Pod",
            "Beast Within",
            "Chord of Calling",
            "Explore",
            "Farseek",
            "Rampant Growth",
            "Cultivate",
            "Kodama's Reach",
            "Search for Tomorrow",
            "Harmonize",
            "Natural Order",
            "Plow Under",
            "Primal Command",
            "Green Sun's Zenith",
            "Tooth and Nail",
            "Genesis Wave",
            "Utopia Sprawl",
            "Fertile Ground",
            "Sylvan Library",
            "Awakening Zone",
            "Courser of Kruphix",
            "Geist of Saint Traft",
            "Teferi, Time Raveler",
            "Teferi, Hero of Dominaria",
            "Sphinx's Revelation",
            "Supreme Verdict",
            "Fractured Identity",
            "Thief of Sanity",
            "Hostage Taker",
            "The Scarab God",
            "Dragonlord Silumgar",
            "Ashiok, Nightmare Weaver",
            "Baleful Strix",
            "Falkenrath Aristocrat",
            "Murderous Redcap",
            "Angrath, the Flame-Chained",
            "Bedevil",
            "Kolaghan's Command",
            "Angrath's Rampage",
            "Bloodbraid Elf",
            "Huntmaster of the Fells",
            "Ruric Thar, the Unbowed",
            "Dragonlord Atarka",
            "Wrenn and Six",
            "Xenagos, the Reveler",
            "Voice of Resurgence",
            "Kitchen Finks",
            "Knight of Autumn",
            "Sigarda, Host of Herons",
            "Trostani Discordant",
            "Mirari's Wake",
            "Sin Collector",
            "Seraph of the Scales",
            "Magister of Worth",
            "Sorin, Solemn Visitor",
            "Anguished Unmaking",
            "Vindicate",
            "Meren of Clan Nel Toth",
            "Vraska, Relic Seeker",
            "Garruk, Apex Predator",
            "Abrupt Decay",
            "Assassin's Trophy",
            "Casualties of War",
            "Edric, Spymaster of Trest",
            "Trygon Predator",
            "Prime Speaker Vannifar",
            "Roalesk, Apex Hybrid",
            "Hydroid Krasis",
            "Shardless Agent",
            "Niv-Mizzet, Parun",
            "Dack Fayden",
            "Izzet Charm",
            "Electrolyze",
            "Figure of Destiny",
            "Aurelia, Exemplar of Justice",
            "Ajani Vengeant",
            "Nahiri, the Harbinger",
            "Lightning Helix",
            "Deafening Clarion",
            "Nicol Bolas, Dragon-God",
            "Nicol Bolas, Planeswalker",
            "Ulamog, the Ceaseless Hunger",
            "Kozilek, Butcher of Truth",
            "Ulamog, the Infinite Gyre",
            "Emrakul, the Promised End",
            "Emrakul, the Aeons Torn",
            "Karn, Scion of Urza",
            "Karn Liberated",
            "Ugin, the Spirit Dragon",
            "Bomat Courier",
            "Phyrexian Revoker",
            "Spellskite",
            "Solemn Simulacrum",
            "Duplicant",
            "Steel Hellkite",
            "Wurmcoil Engine",
            "Myr Battlesphere",
            "Sundering Titan",
            "Hangarback Walker",
            "Walking Ballista",
            "Everflowing Chalice",
            "Relic of Progenitus",
            "Coldsteel Heart",
            "Grim Monolith",
            "Lightning Greaves",
            "Mind Stone",
            "Prismatic Lens",
            "Smuggler's Copter",
            "Treasure Map",
            "Umezawa's Jitte",
            "Aethersphere Harvester",
            "Basalt Monolith",
            "Chromatic Lantern",
            "Coalition Relic",
            "Crucible of Worlds",
            "Mimic Vat",
            "Oblivion Stone",
            "Sword of Body and Mind",
            "Sword of Feast and Famine",
            "Sword of Fire and Ice",
            "Sword of Light and Shadow",
            "Sword of Sinew and Steel",
            "Sword of War and Peace",
            "Tangle Wire",
            "Worn Powerstone",
            "Coercive Portal",
            "Hedron Archive",
            "Thran Dynamo",
            "Batterskull",
            "Engineered Explosives",
            "Gilded Lotus",
            "Skysovereign, Consul Flagship",
            "Mindslaver",
            "The Immortal Sun",
            "Pact of Negation",
            "Slaughter Pact",
            "Ancestral Vision",
            "All Is Dust",
            "Ancient Tomb",
            "Arid Mesa",
            "Badlands",
            "Bayou",
            "Blackcleave Cliffs",
            "Blood Crypt",
            "Bloodstained Mire",
            "Blooming Marsh",
            "Botanical Sanctum",
            "Breeding Pool",
            "Celestial Colonnade",
            "City of Brass",
            "Clifftop Retreat",
            "Concealed Courtyard",
            "Copperline Gorge",
            "Creeping Tar Pit",
            "Darkslick Shores",
            "Dragonskull Summit",
            "Drowned Catacomb",
            "Evolving Wilds",
            "Field of Ruin",
            "Flooded Strand",
            "Gaea's Cradle",
            "Glacial Fortress",
            "Godless Shrine",
            "Hallowed Fountain",
            "Hinterland Harbor",
            "Hissing Quagmire",
            "Horizon Canopy",
            "Inspiring Vantage",
            "Isolated Chapel",
            "Karakas",
            "Lavaclaw Reaches",
            "Lumbering Falls",
            "Mana Confluence",
            "Marsh Flats",
            "Maze of Ith",
            "Mishra's Factory",
            "Misty Rainforest",
            "Mutavault",
            "Needle Spires",
            "Nykthos, Shrine to Nyx",
            "Overgrown Tomb",
            "Plateau",
            "Polluted Delta",
            "Raging Ravine",
            "Rishadan Port",
            "Rootbound Crag",
            "Sacred Foundry",
            "Savannah",
            "Scalding Tarn",
            "Scrubland",
            "Seachrome Coast",
            "Shambling Vent",
            "Shelldock Isle",
            "Spirebluff Canal",
            "Steam Vents",
            "Stirring Wildwood",
            "Stomping Ground",
            "Sulfur Falls",
            "Sunpetal Grove",
            "Taiga",
            "Tectonic Edge",
            "Temple Garden",
            "Treetop Village",
            "Tropical Island",
            "Tundra",
            "Underground Sea",
            "Verdant Catacombs",
            "Volcanic Island",
            "Volrath's Stronghold",
            "Wandering Fumarole",
            "Wasteland",
            "Watery Grave",
            "Windbrisk Heights",
            "Windswept Heath",
            "Wooded Foothills",
            "Woodland Cemetery",
            "Expansion"}; // " // Explosion"
    private double totalCubePrice = 0;

    /**
     * Get all cards from the database, then look up all of their prices
     *
     * @param activities The activity which started this task
     * @return nothing
     */
    @Override
    protected Void doInBackground(FamiliarActivity... activities) {

        // Save the activity
        FamiliarActivity activity = activities[0];

        // Delete all caches
        try {
            File cacheDir = activity.getExternalCacheDir();
            for (File cacheFile : Objects.requireNonNull(Objects.requireNonNull(cacheDir).listFiles())) {
                cacheFile.delete();
            }
            cacheDir = activity.getCacheDir();
            for (File cacheFile : Objects.requireNonNull(Objects.requireNonNull(cacheDir).listFiles())) {
                cacheFile.delete();
            }
        } catch (NullPointerException e) {
            // Eh
        }

        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            Bundle args = new Bundle();
            SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, handle);

            totalCubePrice = 0;
            ArrayList<MtgCard> cubeCards = new ArrayList<>(CUBE.length);
            for (String cName : CUBE) {
                MtgCard card = CardDbAdapter.getCubeCard(cName, database);
                if (null != card) {
                    cubeCards.add(card);
                    if (cubeCards.size() % 50 == 0) {
                        Log.d(DAPT_TAG, "Loaded " + cubeCards.size() + " cards");
                    }
                }
            }

            Runnable runWhenDone = new Runnable() {
                int cubeCardIdx = 0;

                @Override
                public void run() {
                    if (cubeCardIdx < cubeCards.size()) {
                        lookupCard(activity.mMarketPriceStore, cubeCards.get(cubeCardIdx), activity, this);
                    }
                    else {
                        Log.d(DAPT_TAG, String.format("All done, %.02f", totalCubePrice));
                    }
                    cubeCardIdx++;
                }
            };
            // Try to lookup all prices
            runWhenDone.run();

        } catch (SQLiteException | FamiliarDbException | IllegalStateException ignored) {
            /* Eh */
        } finally {
            DatabaseManager.closeDatabase(activity, handle);
        }

//        // Search for all cards
//        SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, mHandle);
//        SearchCriteria criteria = new SearchCriteria();
//        criteria.superTypes = new ArrayList<>(1);
//        criteria.superTypes.add("!asdl");
//        String[] returnTypes = {
//                CardDbAdapter.KEY_NAME,
//                CardDbAdapter.KEY_SET,
//                CardDbAdapter.KEY_SUBTYPE,
//                CardDbAdapter.KEY_SUPERTYPE,
//                CardDbAdapter.KEY_NUMBER};
//        String orderByStr = CardDbAdapter.KEY_SET + " ASC, " + CardDbAdapter.KEY_NUMBER + " ASC";
//        Set<String> searchLanguages = new HashSet<>(Collections.singletonList("en"));
//        Cursor allCards = CardDbAdapter.Search(criteria, false, returnTypes, false,
//                orderByStr, database, searchLanguages,
//                PreferenceAdapter.getHideOnlineOnly(activity),
//                PreferenceAdapter.getHideFunnyCards(activity));

//        if (null != allCards) {
//            // Log how many cards there are to lookup
//            allCards.moveToLast();
//            Log.d(DAPT_TAG, "Checking " + allCards.getPosition() + " prices");
//            allCards.moveToFirst();
//
//            // Try to lookup all prices
//            lookupCard(activity.mMarketPriceStore, allCards, activity);
//        }
        return null;
    }

    /**
     * Given a fetcher and a cursor pointed to card data in the database, lookup the card price
     *
     * @param fetcher The fetcher to fetch the card price with
     * @param cursor  The cursor pointing to card data in the database
     */
    private void lookupCard(final MarketPriceFetcher fetcher, final Cursor cursor, final FamiliarActivity activity) {
        // Make an MtgCard object from the cursor row
        try {
            MtgCard toLookup = new MtgCard(activity,
                    CardDbAdapter.getStringFromCursor(cursor, CardDbAdapter.KEY_NAME),
                    CardDbAdapter.getStringFromCursor(cursor, CardDbAdapter.KEY_SET),
                    CardDbAdapter.getStringFromCursor(cursor, CardDbAdapter.KEY_NUMBER),
                    false, 0);
            lookupCard(fetcher, toLookup, activity, new Runnable() {
                @Override
                public void run() {
                    fetchNext(fetcher, cursor, activity);
                }
            });
        } catch (InstantiationException e) {

            // Debug print
            Log.d(DAPT_TAG, "Failure [" + cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_SET)) + "] " +
                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME)) + ", " + e.getMessage());

            // Move to the next
            fetchNext(fetcher, cursor, activity);
        }
    }

    /**
     * Given a fetcher and a cursor pointed to card data in the database, lookup the card price
     *
     * @param fetcher The fetcher to fetch the card price with
     */
    private void lookupCard(final MarketPriceFetcher fetcher, final MtgCard toLookup, final FamiliarActivity activity, Runnable runWhenDone) {
        // Make an MtgCard object from the cursor row
        try {
            // Start the lookup and log the time
            long start = System.currentTimeMillis();
            fetcher.fetchMarketPrice(toLookup,
                    marketPriceInfo -> {
                        // Timing
                        long elapsed = System.currentTimeMillis() - start;
                        totalElapsedSuccess += elapsed;
                        totalSuccess++;

                        // Debug print
                        String priceStr = "";
                        if (marketPriceInfo.hasNormalPrice()) {
                            priceStr = String.format(Locale.US, "$%.2f", marketPriceInfo.getPrice(false, MarketPriceInfo.PriceType.MARKET).price);
                            totalCubePrice += marketPriceInfo.getPrice(false, MarketPriceInfo.PriceType.MARKET).price;
                        } else if (marketPriceInfo.hasFoilPrice()) {
                            priceStr = String.format(Locale.US, "$%.2f", marketPriceInfo.getPrice(true, MarketPriceInfo.PriceType.MARKET).price);
                            totalCubePrice += marketPriceInfo.getPrice(true, MarketPriceInfo.PriceType.MARKET).price;
                        }
                        Log.d(DAPT_TAG, "Success [" + toLookup.getExpansion() + "] ~" + toLookup.getName() + "~ in " + elapsed + "ms : " + priceStr);

                        // Move to the next
                        runWhenDone.run();
                    },
                    throwable -> {
                        // Timing
                        long elapsed = System.currentTimeMillis() - start;
                        totalElapsedFailure += elapsed;
                        totalFailure++;

                        // Debug print
                        Log.d(DAPT_TAG, "Failure [" + toLookup.getExpansion() + "] " + toLookup.getName() + " in " + elapsed + "ms, " + throwable.getMessage());

                        // Move to the next
                        runWhenDone.run();
                    },
                    () -> {
                    });
        } catch (InstantiationException | FamiliarDbException e) {

            // Debug print
            try {
                Log.d(DAPT_TAG, "Failure [" + CardDbAdapter.getStringFromCursor(cursor, CardDbAdapter.KEY_SET) + "] " +
                        CardDbAdapter.getStringFromCursor(cursor, CardDbAdapter.KEY_NAME) + ", " + e.getMessage());
            } catch (FamiliarDbException familiarDbException) {
                familiarDbException.printStackTrace();
            }

            // Move to the next
            runWhenDone.run();
        }
    }

    /**
     * Try to move the cursor to the next card and start looking up the price
     *
     * @param fetcher The fetcher to fetch the card price with
     * @param cursor  The cursor to advance
     */
    private void fetchNext(MarketPriceFetcher fetcher, Cursor cursor, FamiliarActivity activity) {
        cursor.moveToNext();
        if (!cursor.isAfterLast()) {
            lookupCard(fetcher, cursor, activity);
        } else {
            Log.d(DAPT_TAG, totalSuccess + " successes (avg " + (totalElapsedSuccess / (double) totalSuccess) + "ms)");
            Log.d(DAPT_TAG, totalFailure + " failures (avg " + (totalElapsedFailure / (double) totalFailure) + "ms)");
            cursor.close();
//            DatabaseManager.closeDatabase(activity, mHandle);
        }

    }
}
