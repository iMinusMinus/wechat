package weixin.mp.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public enum FileType {
    // 图片
    JPG() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(JPEG).iterator();
        }
    },
    JPEG() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(JPG).iterator();
        }
    },
    PNG,
    BMP,
    GIF,

    TIF() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(TIFF).iterator();
        }
    },
    TIFF() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(TIF).iterator();
        }
    },
    WEBP,
    PSD,
    // 音频
    WMA() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(WAV).iterator();
        }
    },
    WAV() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(WMA).iterator();
        }
    },
    AMR,
    MP3,

    M4A,
    FLAC,
    APE,
    // 视频
    MP4,

    RM() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(RMVB).iterator();
        }
    },
    RMVB() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(RM).iterator();
        }
    },
    TS() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(TSA, TSV).iterator();
        }
    },
    TSA() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(TS, TSV).iterator();
        }
    },
    TSV() {
        @Override
        public Iterator<FileType> alias() {
            return List.of(TS, TSA).iterator();
        }
    },
    AVI,
    FLV,
    MKV,
    MOV,
    // 办公
    RTF,
    PPT,
    XLS,
    DOC,


    // 压缩
    RAR,
    ZIP,
    ARJ,
    JAR

    ;

    public Iterator<FileType> alias() {
        return Collections.emptyIterator();
    }

    public static FileType getInstance(String name) {
        for (FileType instance : FileType.values()) {
            if (instance.name().equalsIgnoreCase(name)) {
                return instance;
            }
        }
        return null;
    }

}
