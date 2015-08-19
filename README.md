# EncoderDecoderTestForMJPEG

## Purpose

This is a test application for JPEG/M-JPEG throug `MediaCodec`. This could also be a good tutorial of how to using
`MediaCodec` `API` on Android.

## Requisites

Before testing, you should put input frames in the external storage of your target.

- For Encoder Test, put 10 YUV frames at `/storage/sdcard0/video.YUY2_<frame_index>`.
- For Decoder Test, put 10 JPEG pictures at `/storage/sdcard0/video.jpeg_<jpeg_index>`.

## How to build, how to use

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
run cts -c android.media.cts.EncoderDecoderTestForMJPEG -m <your-testing-method>
```

4. Test result

You will see a *PASS* if the test complete successfully. And 10 output frames in the same directory where you keep testing input.

- For Encoder Test, since your input is like `video.YUY2_<frame_index>` so you will get `video.jpeg_<jpeg_index>`.
- For Decoder Test, since your input is like `video.jpeg_<jpeg_index>` so you will get `video.YUY2_<frame_index>`.

## Current Status

* The JPEG encoder can encoder one frame successfully.
* Add test plans for different resolutions.    @20150804
* Change to feed 10 input buffers continuously, in this way, we can get multiple output buffers. @20150804


## Working in progress

~~1 . Fix infinite looping issue of JPEG encoder.~~

~~2 . Add decoder test.~~

3 . Fix the `info.flags` that can't got EOS flags from output `BufferInfo`.

~~4 . Fix run-time warnings.~~

~~5 . Code revise needed.~~

6 . Should handle Java `FileOutputStream`/`FileInputStream` nicely.




