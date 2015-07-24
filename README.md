# EncoderDecoderTestForMJPEG

* **Purpose**

This is a test application for JPEG/M-JPEG throug `MediaCodec`. This could also be a good tutorial of how to using
`MediaCodec` `API` on Android.

* **How to build, how to use**

This was written as if it were a CTS test, but is not part of CTS.It should be straightforward to adapt the code to
other environments. 

1 . Place the code

You can put the `src` under `cts` directory of the whole android project. It looks Like:
```
<your-android-proj-root>/cts/tests/tests/media/src/android/media/cts/EncoderDecoderTestForMJPEG.java
```

2 . Build the code

Just build the whole `cts` test target, `EncoderDecoderTestForMJPEG.java` will be included in the `cts` target
automatically.

```
make cts -j4
```

3 . How to run the test

Just like any other `cts` test cases, follow command:

```
run cts -c EncoderDecoderTestForMJPEG -m <your-testing-method>
```

* **Current Status**

The JPEG encoder can encoder one frame successfully.

* **Working in progress**

1 . Fix infinite looping issue of JPEG encoder.

2 . Add decoder test




