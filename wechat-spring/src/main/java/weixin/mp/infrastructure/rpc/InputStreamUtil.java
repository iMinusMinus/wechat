package weixin.mp.infrastructure.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weixin.mp.domain.FileType;

import java.io.InputStream;
import java.util.Arrays;

public abstract class InputStreamUtil {

    private static final Logger log = LoggerFactory.getLogger(InputStreamUtil.class);

    private static final byte[] JPG = {(byte) 0xFF, (byte)0xD8};

    private static final byte[] BMP = {(byte) 0x42, (byte) 0x4D};

    private static final byte[] GIF_87A = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x37, (byte) 0x61};

    private static final byte[] GIF_89A = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61};

    private static final byte[] PNG = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

    private static final byte[] WMA = {(byte) 0x30, (byte) 0x26, (byte) 0xB2, (byte) 0x75, (byte) 0x8E, (byte) 0x66, (byte) 0xCF, (byte) 0x11,
            (byte) 0xA6, (byte) 0xD9, (byte) 0x00, (byte) 0xAA, (byte) 0x00, (byte) 0x62, (byte) 0xCE, (byte) 0x6C};

    private static final byte[] WAV = WMA;

    private static final byte[] AMR = {(byte) 0x23, (byte) 0x21, (byte) 0x41, (byte) 0x4D, (byte) 0x52};

    private static final byte[] MP3 = {(byte) 0x49, (byte) 0x44, (byte) 0x33};

    private static final byte[] MP4 = {(byte) 0x66, (byte) 0x74, (byte) 0x79, (byte) 0x70, (byte) 0x4D, (byte) 0x53, (byte) 0x4E, (byte) 0x56};

    /**
     * <a href="https://www.garykessler.net/library/file_sigs.html">根据文件头判断文件格式</a>
     * @link https://www.garykessler.net/library/file_sigs.html
     * @param is 流
     * @return 文件格式
     */
    public static FileType lookup(InputStream is) {
        if (!is.markSupported()) {
            log.warn("InputStream[{}] not support mark/reset, skip read it.", is.getClass());
            return null;
        }
        is.mark(WMA.length);
        byte[] magicNumber = new byte[WMA.length]; // longest magic number count
        try {
            int readBytes = is.read(magicNumber);
            is.reset();
            assert readBytes == magicNumber.length;
            return lookup(magicNumber);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.warn("file type unknown as not implemented or error occur");
        return null;
    }

    public static FileType lookup(byte[] data) {
        assert data.length >= WMA.length;
        if (Arrays.equals(data, 0, PNG.length, PNG, 0, PNG.length)) {
            return FileType.PNG;
        } else if (Arrays.equals(data, 0, JPG.length, JPG, 0, JPG.length)) {
            return FileType.JPG;
        } else if (Arrays.equals(data, 0, BMP.length, BMP, 0, BMP.length)) {
            return FileType.BMP;
        } else if (Arrays.equals(data, 0, GIF_87A.length, GIF_87A, 0, GIF_87A.length)
                || Arrays.equals(data, 0, GIF_89A.length, GIF_89A, 0, GIF_89A.length)) {
            return FileType.GIF;
        } else if (Arrays.equals(data, 0, WMA.length, WMA, 0, WMA.length)) {
            return FileType.WMA;
        } else if (Arrays.equals(data, 0, AMR.length, AMR, 0, AMR.length)) {
            return FileType.AMR;
        } else if (Arrays.equals(data, 0, MP3.length, MP3, 0, MP3.length)) {
            return FileType.MP3;
        } else if (Arrays.equals(data, 0, MP4.length, MP4, 0, MP4.length)) {
            return FileType.MP4;
        }
        return null;
    }

}
