A continous authentication application for Android based devices.

Monitors
 - Recent Applications usage
 - Settings usage
 - Location usage

Uses
 - A Bayesian Classifier to help classify incoming data as normal or abnormal usage
 - SQLITE to store training, testing information.
 - Observers on DBs to detect settings changes, other 
     lib. functions to observe GPS/Network based location services and 
      Recent programs' informations.

TODO:

 - Incorporating WifiSettings, ContactSettings in the main app
 - More training, testing experiments
 - Some code cleaning and documentation

