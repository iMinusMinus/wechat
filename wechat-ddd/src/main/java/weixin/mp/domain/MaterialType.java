package weixin.mp.domain;

public enum MaterialType {

    IMAGE("image", 10 * 1024, FileType.BMP, FileType.PNG, FileType.JPEG, FileType.JPG, FileType.GIF), // 图片，仅永久素材支持bmp
    VOICE("voice", 2 * 1024, FileType.WMA, FileType.WAV, FileType.AMR, FileType.MP3), // 语音，播放长度不超过60s，仅永久素材支持wma, wav
    VIDEO("video", 10 * 1024, FileType.MP4), // 视频
    THUMB("thumb", 64, FileType.JPG), // 缩略图

    NEWS("news", 1024 /* unknown */, new FileType[0] /* unknown*/),
    ;

    /**
     * 媒体文件类型
     */
    private final String value;

    /**
     * 素材大小（单位KB）
     */
    private final long maxSize;

    /**
     * 支持的格式
     */
    private final FileType[] supportedFormats;

    public String getValue() {
        return value;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public FileType[] getSupportedFormats() {
        return supportedFormats;
    }

    MaterialType(String value, long maxSize, FileType... supportedFormats) {
        this.value = value;
        this.maxSize = maxSize;
        this.supportedFormats = supportedFormats;
    }

    public static MaterialType getInstance(FileType fileType) {
        for (MaterialType instance : MaterialType.values()) {
            for (FileType supportFileType : instance.supportedFormats) {
                if (supportFileType == fileType) {
                    return instance;
                }
            }
        }
        return null;
    }

    public static MaterialType getInstance(String value) {
        for (MaterialType instance : MaterialType.values()) {
            if (instance.value.equals(value)) {
                return instance;
            }
        }
        return null;
    }
}
