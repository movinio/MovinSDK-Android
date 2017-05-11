##MovinSDK-Android
This repository contains everything related to the MovinSDK for Android, which are:

* The core MovinSDK library (sdk/MovinSDK-release.aar)
* The MovinSDK GoogleMaps integration library (sdk/MovinSDKGoogleMaps-release.aar)
* A simple example app that uses these libraries (example-project/AndroidSDKDemo/)

Demo
----
To try the MovinSDK you only need to perform the following steps:

* Load the example app in Android Studio

* Get a GoogleMaps apikey and provide it in the google_maps_api.xml values file.

 * A GoogleMaps is needed for the GoogleMaps integration, so the indoor map can be shown on top of Google Maps
 
* In the MapsActivity.java file you have to provide the app with your customer, apikey and mapId. All these values can be retrieved from your Movin Portal.

To try the MovinSDK all you have to do is load the example app in Android Studio. The code speaks for itself.
In the apps build.gradle is shown how to include the library files in your project.

##LICENSE
Licensed under Creative Commons Attribution-NoDerivs 3.0 Unported

THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.

BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.

You may obtain copy of the license at [https://creativecommons.org/licenses/by-nd/3.0/legalcode](https://creativecommons.org/licenses/by-nd/3.0/legalcode)