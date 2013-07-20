CatLog
=========
Graphical log reader for Android.

Author
--------
Nolan Lawson

License
--------
[WTFPL][1], although attribution would be nice.

![Screenshot][2]

Overview
---------
CatLog is a free and open-source log reader for Android.  

It shows a scrolling (tailed) view of the Android "Logcat" system log, 
hence the goofy name.  It also allows you to record logs in real time, send logs via email, 
and filter using a variety of criteria.

Download
--------------

CatLog may be downloaded from [the Google Play Store][3].  

You can also find a direct APK download link from [the CatLog page on my web site][5].

FAQs
-------------

#### Where are the logs saved?

On the SD card, under ```/sdcard/catlog/saved_logs/```.

#### I can't see any logs!

First, check to see if you're running a custom ROM.  If so, test on an alternative logging app, and if the
problem persists, contact the creator of your ROM.

Otherwise, check to see if you're running Jelly Bean (Android 4.2+).  [CatLog has issues with Jelly Bean][6].


Details
-----------

You can read all about CatLog in [my blog posts][4].


[1]: http://sam.zoy.org/wtfpl/
[2]: http://nolanwlawson.files.wordpress.com/2012/09/catlog_1_4.png?w=252&h=300
[3]: https://play.google.com/store/apps/details?id=com.nolanlawson.logcat
[4]: http://nolanlawson.com/tag/catlog/
[5]: http://nolanlawson.com/apps/#catlog
[6]: http://nolanlawson.com/2012/09/02/catlog-jives-with-jelly-bean-goes-open-source/
