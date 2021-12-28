# MTG Familiar

MTG Familiar is an Android app which has a suite of tools to play Magic: The Gathering

## Features
- Offline advanced card search with image and price lookup
- Life tracking, including Commander damage
- Mana pool / Deck count tracking
- Dice, as random as Java's PRNG
- Card trader which compares prices for lists of cards
- A Wishlist
- A configurable round timer, with 5/10/15 minute warnings
- Searchable, offline comprehensive rules
- Offline Judge documents (Magic Tournament Rules, Infraction Procedure Guide) and deck counter
- Momir, Jhoira, Stonehewer randomizer

## How To Build
[![Build Status](https://circleci.com/gh/AEFeinstein/mtg-familiar.svg?&style=shield&circle-token=aa58feb74f2eb4f089dc621f23745b3b32279fd9)](https://circleci.com/gh/AEFeinstein/mtg-familiar)
- Download and install the latest [Android Studio](https://developer.android.com/sdk/index.html)
- Make sure you have installed the lastest Android SDK Platform, Android SDK Tools, Android SDK Platform-tools, Android SDK Build-tools, Android Support Repository, Android Support Library, and Google Play Services from the Android SDK manager
- Check out this project with Android Studio's VCS tools
- Press the build button

## How to Update the Internal Database
1. Launch an Android Emulator
2. Run MTG Familiar on said Emulator
3. Force Update the database in Familiar running in the emulator
4. Run the following script to pull off and compress the database
  ~~~~
  rm data datagz
  adb root
  adb pull /data/data/com.gelakinetic.mtgfam/databases/data
  adb pull /data/data/com.gelakinetic.mtgfam.debug/databases/data
  gzip -c -f --best data > datagz
  ~~~~
5. Copy ```datagz``` into the ```/mobile/src/main/res/raw directory```
6. Increment ```DATABASE_VERSION``` in ```/mobile/src/main/java/com/gelakinetic/mtgfam/helpers/database/CardDbAdapter.java```
7. Rename and copy any new Magic Tournament Rules, Infraction Procedure Guide, or Judging at Regular documents into ```/mobile/src/main/res/raw```. This isn't really the database, but it's nice

## Get it
<a href="https://play.google.com/store/apps/details?id=com.gelakinetic.mtgfam">
	<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="240" alt="Google Play">
</a>
<a href="https://f-droid.org/repository/browse/?fdid=com.gelakinetic.mtgfam">
	<img src="https://gitlab.com/fdroid/artwork/-/raw/master/badge/get-it-on-en-us.png" width="240" alt="F-Droid">
</a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/com.gelakinetic.mtgfam">
	<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" width="240" alt="IzzyOnDroid">
</a>

## Show Support
The app is free, and the source is all here. If you'd like to say thanks and show a little support, here's a button: [![PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted)

## FAQs
-No one has asked any questions yet!

## Become an Official Beta Tester
To receive official beta builds through Google Play, first join the Google+ Community [MTG Familiar Beta Testers](https://plus.google.com/communities/110783165129365768059). Once you are a member of the community, you can opt-in to the [beta program here](https://play.google.com/apps/testing/com.gelakinetic.mtgfam)
