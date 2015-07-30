/**
 * This was written as if it were a CTS test, but is not part of CTS. It should be straightforward
 * to adapt the code to other environments.
 *
 * It's enable YUVY422 pic buffer to JPEG using MediaCodec
 *
 * Author: Alan Wang
 * Date  : 2015-07-10
 */

package android.media.cts;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 *
 */
public class EncoderDecoderTestForMJPEG extends AndroidTestCase {
    private static final String LOG_TAG = "EncoderDecoderTestForMJPEG";

    // Where to find the input
    // And Where to store the output
    private static final String INPUT_FILE = Environment.getExternalStorageDirectory() + "/video.YUY2";
    private static final String OUTPUT_FILE = Environment.getExternalStorageDirectory() + "/video.jpeg";

    // Parameters for encoder
    private static final String MIME_TYPE = "video/mjpeg";

    private static final int TIME_OUT = 10000; //usec

    private static final int FRAME_RATE = 15;               // 15fps
    private static final int NUM_FRAMES = 10;                // Only one frame
    private static final int IFRAME_INTERVAL = 10;          // 10 secs between I-frames


    private int mWidth = -1;
    private int mHeight = -1;
    private int mBitRate = -1;

    // Allocate one BufferInfo
    private MediaCodec.BufferInfo mBufferInfo;

    /**
     * Test Encoding of MJPEG  from Buffer
     *
     */

    public void testEncodeVideoFromBufToBuf720p() throws Exception {
        Log.i(LOG_TAG, "Encode Video from Buffer To Buffer 720p");
        setParameters(1280, 720, 6000000);
        encodeVideoFromBuffer();
    }

    public void testDecodeVideoFromBufToBuf720p() throws Exception {
        Log.i(LOG_TAG, "Decode Video from Buffer to Buffer 720p");
        setParameters(1280, 720, 6000000);
        decodeVideoFromBuffer();
    }

    /**
     * Sets the desired frame size and bit rate.
     */
    private void setParameters(int width, int height, int bitRate) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(LOG_TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
    }

    private void encodeVideoFromBuffer() throws Exception {
        MediaCodec encoder = null;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE, true);
            if (codecInfo == null) {
                Log.e(LOG_TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }

            Log.i(LOG_TAG, "Found codec: " + codecInfo.getName());

            // JPEG encoder only support this colorFormat as Input
            // COLOR_FormatCbYCrY a.k.a Organized as 16bit UYVY
            int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY;

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            Log.i(LOG_TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            Log.i(LOG_TAG, "created Codec " + codecInfo.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            doEncodeVideoFromBuffer(encoder, colorFormat);

        } finally {

            Log.i(LOG_TAG, "releasing encoder");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }

        }

    }

    private void decodeVideoFromBuffer() throws Exception {
        MediaCodec decoder = null;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE, false);
            if (codecInfo == null) {
                Log.e(LOG_TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }

            Log.i(LOG_TAG, "Found codec: " + codecInfo.getName());

            // JPEG decoder only support this colorFormat as Output
            // COLOR_FormatCbYCrY a.k.a Organized as 16bit UYVY
            int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY;

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            decoder = MediaCodec.createByCodecName(codecInfo.getName());
            Log.i(LOG_TAG, "created Codec " + codecInfo.getName());
            decoder.configure(format, null, null, 0);
            decoder.start();

            doDecodeVideoFromBuffer(decoder, colorFormat);

        } finally {

            Log.i(LOG_TAG, "releasing decoder");
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

        }

    }

    /**
     * Returns the first codec capable of encoding the specified MIME_TYPE
     */
    private static MediaCodecInfo selectCodec(String mimeType, boolean needEncoder) {
        int numCodec = MediaCodecList.getCodecCount();
        Log.i(LOG_TAG, "We got " + numCodec + " Codecs");

        for (int i = 0; i < numCodec; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder() && needEncoder) {
                continue;
            }

            String[] type = codecInfo.getSupportedTypes();
            for (int j = 0; j < type.length; j++) {
                Log.i(LOG_TAG, "We got type " + type[j]);
                if (type[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }

        }

        return null;
    }

    /**
     * Do the actual work for encoding frames from buffers of byte[].
     * @param encoder
     * @param encoderColorFormat
     */
    private void doEncodeVideoFromBuffer(MediaCodec encoder, int encoderColorFormat) throws IOException {
        Log.i(LOG_TAG, "In doEncodeVideoFromBuffer...");

        int generateIndex = 0;

        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        // The size of a frame of video data, in the format we handle, is  mWidth*mHeight*2
        byte[] frameData = new byte[mWidth * mHeight * 2];

        long encodedSize = 0;

        // Encoded File
        FileOutputStream outputStream = new FileOutputStream(OUTPUT_FILE);


        boolean inputDone = false;
        boolean encodeDone = false;
        boolean outputDone = false;

        while (!outputDone) {
            Log.i(LOG_TAG, "Looping...");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone) {
                int inputBufIndex = encoder.dequeueInputBuffer(TIME_OUT);
                Log.d(LOG_TAG, "inputBufIndex = " + inputBufIndex);

                if (inputBufIndex >= 0) {
                    long ptsUsec = computePTS(generateIndex);
                    // Store input data into frameData
                    generateFrame(generateIndex, encoderColorFormat, frameData, true, true);

                    ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];
                    // Buf capacity check
                    Log.i(LOG_TAG, "inputBuf.capacity() = " + inputBuf.capacity()
                            + "frameData.length = " + frameData.length);
                    assertTrue(inputBuf.capacity() >= frameData.length);
                    inputBuf.clear();
                    inputBuf.put(frameData);

                    encoder.queueInputBuffer(inputBufIndex, 0, frameData.length, ptsUsec, 0);
                    Log.i(LOG_TAG, "Buffer " + inputBufIndex + " submitted to encode");
                } else {
                    Log.w(LOG_TAG, "Input buffer not available");
                }

                generateIndex++;
                if (generateIndex >= NUM_FRAMES) {
                    Log.d(LOG_TAG, "generateIndex = " + generateIndex + " Queuing EOS in InputBuffer");
                    encoder.queueInputBuffer(inputBufIndex, 0, 0, 0,
                                             MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    inputDone = true;
                }
            }

            // Check for output from the encoder.  If there's no output yet, we either need to
            // provide more input, or we need to wait for the encoder to work its magic.  We
            // can't actually tell which is the case, so if we can't get an output buffer right
            // away we loop around and see if it wants more input.
            //
            // Once we get EOS from the encoder, we don't need to do this anymore.
            if (!encodeDone) {
                Log.d(LOG_TAG, "dequeuing Output buffers...");
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIME_OUT);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(LOG_TAG, "No output from encoder available, try again later.");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.i(LOG_TAG, "INFO_OUTPUT_BUFFERS_CHANGED... NOT EXPECTED for and encoder");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(LOG_TAG, "INFO_OUTPUT_FORMAT_CHANGED... NOT EXPECTED for and encoder");
                } else if (encoderStatus < 0) {
                    Log.e(LOG_TAG, "Unexpected result!!!");
                    fail("encoder in wrong Status");
                } else { // it's the index of an output buffer that has been successfully decoded
                    Log.i(LOG_TAG, "Encode success, encoderStatus = " + encoderStatus);
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        fail("encoderOuputBuffer " + encoderStatus + " was null");
                    }

                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);

                    encodedSize += info.size;

                    if (outputStream != null) {
                        byte[] data = new byte[info.size];
                        encodedData.get(data); //Store input buffer into data
                        encodedData.position(info.offset);

                        Log.i(LOG_TAG, "outputStream writing data!");
                        outputDone = true;
                        outputStream.write(data);
                    }

                    //FIXME: Check info.flags is not working ???
                    if (generateIndex >= NUM_FRAMES) {
                        outputDone = true;
                    }

                    encoder.releaseOutputBuffer(encoderStatus, false);
                    encodeDone = true;

                }
            }
        }
    }

    /**
     * Do the actual work for decoding frames from buffers of byte[].
     * @param decoder
     * @param decoderColorFormat
     *
     */
    private void doDecodeVideoFromBuffer(MediaCodec decoder, int decoderColorFormat)
            throws IOException {
        Log.i(LOG_TAG, "In doDecodeVideoFromBuffer ...");

        // As for decoder, the input file is video.jpeg the output file is video.YUY2
        // which is oppsite with encoder
        String INPUT = OUTPUT_FILE;
        String OUTPUT = INPUT_FILE;

        File input = new File(INPUT);

        int generateIndex = 0;

        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        // The size of input picture
        byte[] frameData = new byte[(int)input.length()];

        // decoded File
        FileOutputStream outputStream = new FileOutputStream(OUTPUT);


        boolean inputDone = false;
        boolean decodeDone = false;
        boolean outputDone = false;

        while (!outputDone) {
            Log.i(LOG_TAG, "Loopping...");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIME_OUT);
                Log.i(LOG_TAG, "inputBufIndex = " + inputBufIndex);

                if (inputBufIndex >= 0) {
                    long ptsUsec = computePTS(generateIndex);

                    // Store input data into frameData
                    generateFrame(generateIndex, decoderColorFormat, frameData, true, false);

                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    // Buf capacity check
                    Log.i(LOG_TAG, "inputBuf.capacity() = " + inputBuf.capacity()
                            + "frameData.length = " + frameData.length);
                    assertTrue(inputBuf.capacity() >= frameData.length);
                    inputBuf.clear();
                    inputBuf.put(frameData);

                    decoder.queueInputBuffer(inputBufIndex, 0, frameData.length, ptsUsec, 0);
                    Log.i(LOG_TAG, "Buffer " + inputBufIndex + " of len = " + frameData.length + " submitted to decode");
                    generateIndex++;

                } else {
                    Log.w(LOG_TAG, "Input buffer not available");
                }

                if(generateIndex >= NUM_FRAMES) {
                     // We have enough frames, so send an empty one with EOS flag
                    //Log.d(LOG_TAG, "generateIndex = " + generateIndex + "Queuing EOS in InputBuffer");
                    //decoder.queueInputBuffer(inputBufIndex, 0, 0, 0,
                    //        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    inputDone = true;
                }
            }

            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (!decodeDone) {
                Log.d(LOG_TAG, "dequeuing Output buffers...");
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIME_OUT);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(LOG_TAG, "No output from decoder available, try again later.");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.i(LOG_TAG, "INFO_OUTPUT_BUFFERS_CHANGED... NOT EXPECTED for and decoder");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(LOG_TAG, "INFO_OUTPUT_FORMAT_CHANGED... NOT EXPECTED for and decoder");
                } else if (decoderStatus < 0) {
                    Log.e(LOG_TAG, "Unexpected result!!!");
                    fail("decoder in wrong Status");
                } else { // it's the index of an output buffer that has been successfully decoded
                    Log.i(LOG_TAG, "Decode success, decoderStatus =  " + decoderStatus + " info.size = " + info.size);

                    ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();

                    ByteBuffer decodedData = decoderOutputBuffers[decoderStatus];
                    if (decodedData == null) {
                        fail("decoderOuputBuffer " + decoderStatus + " was null");
                    }

                    Log.i(LOG_TAG, "outputStream = " + outputStream);

                    decodedData.position(info.offset);
                    decodedData.limit(info.offset + info.size);

                    if (outputStream != null) {
                        byte[] data = new byte[info.size];
                        decodedData.get(data); //Store input buffer into data
                        decodedData.position(info.offset);

                        if (info.size > 0) {
                            Log.i(LOG_TAG, "outputStream writing data! size = " + info.size);
                            outputDone = true;
                            outputStream.write(data);
                        }
                    }

                    decoder.releaseOutputBuffer(decoderStatus, false);
                    decodeDone = true;

                }
            }
        }

    }

    private long computePTS(int generateIndex) {
        return 132 + generateIndex * 1000000 / FRAME_RATE;
    }

    /**
     *
     * @param frameIndex
     * @param colorFormat
     * @param frameData
     * @param readFromDisk true: the data is read file the yuv file
     *                [WIP]false: the data is generated like:
     *
     * Generates data for frame N into the supplied buffer.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the zero-fill color.
     */
    private void generateFrame(int frameIndex, int colorFormat,
                               byte[] frameData, boolean readFromDisk,
                               boolean encoder) {
        Log.i(LOG_TAG, "Generating frame " + frameIndex + "for " + (encoder ? "encoder" : "decoder"));

        FileInputStream fileInputStream = null;
        File file = null;

        // If it's decoder the OUTPUT_FILE name is as INPUT
        if (encoder) {
            file = new File(INPUT_FILE);
        } else {
            file = new File(OUTPUT_FILE);
        }

        Log.i(LOG_TAG, "file.length = " + file.length());


        if (readFromDisk) {

            try {
                //convert file into array of bytes
                fileInputStream = new FileInputStream(file);
                fileInputStream.read(frameData);
                fileInputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            //WIP: Read from surface
            Log.w(LOG_TAG, "Not implement so far...");
        }


    }


}
