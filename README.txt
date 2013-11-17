 This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

----------------------------------------------------------------------------------

UsageMonitoring application is a continous authentication application for Android based devices.

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

