# WifiTrilateration
#### Wifi Radio Signal Strength (RSS) based Indoor Location Determination

This is Android Application to determine one’s indoor location coordinates, given RSSI measurements from two or more 
access points whose location is already known accurately.

## Working
1.	Configure Access Points properties such as BSSID, MAC address and location coordinates.
2.	Calibrate Access Point using Curve Fitting to estimate n and A. 
  1.	The user is asked to place the device at various different distances from the AP. In this case at distances 0.1m, 0.5m, 1m and 2m
  2.	The app takes multiple samples of the RSSI values at each distance and uses the mean RSSI for the data. 
  3.	The data now consists of  and sampled and averaged RSSI at different values of d. A polynomial fitter function is used to do curve fitting on the data, and determine the value of n and A.
3.	Locate the position of the cell phone using Trilateration by scanning for wifi access points.
  1.	The app	 scans for the nearby access points and obtains their RSSI (Radio Signal Strength Index) in db.
  2.	The RSSI is used to calculate the distance from the AP using the 2 methods (described in the next section).
  3.	The measured distances and the AP Locations stored in its database are used to perform trilateration and get an estimate on the device location.
  4.	The estimated location coordinates corresponding to both the two methods and the APs used for trilateration are reported to the user.

### RSSI to Distance
The app uses two different methods to estimate the distance of the device from an Access Point.

1. Using the Free Space Path Loss formula [src] (http://rvmiller.com/2013/05/part-1-wifi-based-trilateration-on-android/)
2. Using Curve Fitting [src] (http://www.rn.inf.tu-dresden.de/dargie/papers/icwcuca.pdf)

### Trilateration
The app uses [this] (https://github.com/lemmingapex/trilateration) trilateration solver for estimating location of the device given AP locations and distances from APs.
