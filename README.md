# YAHapp

What started as a simple java CLI port of HENkaku offline (still available [here](https://github.com/psychowood/YAHapp/tree/offline-hosting)), is now trying to evolve in a PS Vita (HEHkaku enabled) companion app.

Current features:

* HENkaku server
* Upload a vpk directly to your PSVita (share the .vpk file with YAHapp)

That's it! :) 
It's just the first commit: other features are coming soon, in the meanwhile if you have any idea feel free to add an enhancement request in the [issues board](https://github.com/psychowood/YAHapp/issues).


## Disclaimer

This is my very first Android project, please be forgiving. I'll try to polish it while going on with the development.
Also, I'm testing it on my Nexus w/ Android 6.0 and on the Android simulator: if you have an issue don't forget to give details on your device and androd version.

## Permissions

* android.permission.INTERNET - Network access (HENkaku server, FTP upload)
* android.permission.READ_EXTERNAL_STORAGE - Needed to access files shared from the external storage (SD card, downloads...)

# EXTRAS

## HENkaku Offline (Java port)

You need to host two things: the first stage ROP and the second stage dynamic ROP.
The provided Java implementation is useful for single-user use, since it is not highly performing (not as the Go implementation provided in the original henkaku repository).

### Requirements

* Java > 1.6 (and both the `javac` and the `java` command)

### Serving

Just call

```shell
./serve.sh webserver-port
```

and use the Browser app on the Vita to navigate to the address shown in the console, then tap on Install.

The webserver port is optional, and defaults to 8357

Credits
--------

* Obviously [HENkaku offline](https://github.com/henkaku/henkaku) by Yifan Lu
* HENkaku server runs off [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)
* Reading param.sfo (for vpk validation) with [TiMESPLiNTER's sfo4j](https://github.com/TiMESPLiNTER/sfo4j) 
* Also thanks to [codestation](https://github.com/codestation) for a working stage2 patcher implementation in java.


License
-------

The project is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
