# HENkaku Offline (Java port)

You need to host two things: the first stage ROP and the second stage dynamic ROP.
The provided Java implementation is useful for single-user use, since it is not highly performing (not as the Go implementation provided in the original henkaku repository).

## Requirements

* Java > 1.6 (and both the `javac` and the `java` command)

## Serving

Just call

```shell
./serve.sh webserver-port
```

and use the Browser app on the Vita to navigate to the address shown in the console, then tap on Install.

The webserver port is optional, and defaults to 8357

Credits
--------

Based on [HENkaku offline](https://github.com/henkaku/henkaku) by Yifan Lu

Runs off [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)

Also thanks to [codestation](https://github.com/codestation) for a working stage2 patcher implementation in java.


License
-------

The project is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
