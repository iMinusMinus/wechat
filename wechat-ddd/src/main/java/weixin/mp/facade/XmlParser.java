package weixin.mp.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weixin.mp.domain.EventType;
import weixin.mp.domain.InterestedEvent;
import weixin.mp.domain.MessageType;
import weixin.mp.domain.RequestMessage;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class XmlParser {

    private static final Logger log = LoggerFactory.getLogger(XmlParser.class);

    public static final String ENCRYPT_TAG = "Encrypt";

    private static final String TO_TAG = "ToUserName";

    private static final String FROM_TAG = "FromUserName";

    private static final String TIMESTAMP_TAG = "CreateTime";

    private static final String MSG_TYPE_TAG = "MsgType";

    private static final String MSG_ID_TAG = "MsgId";

    private static final String MSG_DATA_ID_TAG = "MsgDataId";

    private static final String ID_TAG = "Id";

    private static final String MEDIA_ID_TAG = "MediaId";

    protected String toAccount;

    protected String fromOpenId;

    protected long timestamp;

    protected String msgType;

    protected String msgId;

    protected String msgDataId;

    protected List<String> idx = Collections.emptyList();

    abstract void handle(String tagName, String text);

    abstract RequestMessage build();

    public static Map<String, String> xmlToMap(String xml) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        Map<String, String> kv = new LinkedHashMap<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            XMLEventReader reader = factory.createXMLEventReader(bais);
            String tagName = null;
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    tagName = event.asStartElement().getName().toString();
                } else if (event.isEndElement()) {
                    tagName = null;
                } else if (tagName != null && event.isCharacters()) {
                    kv.put(tagName, event.asCharacters().getData());
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return kv;
    }

    public static RequestMessage parse(String xml) {
        Map<String, String> kv = xmlToMap(xml);
        return mapToBean(kv);
    }

    static RequestMessage mapToBean(Map<String, String> kv) {
        XmlParser xmlParser;
        MessageType msgType = MessageType.getInstance(kv.get(MSG_TYPE_TAG));
        assert msgType != null;
        String eventType = kv.get(InterestedEvent.EVENT_TAG);
        switch (msgType) {
            case TEXT-> xmlParser = new TextXmlParser();
            case IMAGE -> xmlParser = new ImageXmlParser();
            case VOICE -> xmlParser = new VoiceXmlParser();
            case VIDEO -> xmlParser = new VideoXmlParser();
            case VIDEOLET -> xmlParser = new VideoletXmlParser();
            case LOCATION -> xmlParser = new LocationXmlParser();
            case LINK -> xmlParser = new LinkXmlParser();
            case EVENT -> xmlParser = newEventXmlParser(eventType);
            default -> throw new IllegalArgumentException("msgType not found: " + msgType);
        }
        for (Map.Entry<String, String> pair : kv.entrySet()) {
            switch (pair.getKey()) {
                case TO_TAG -> xmlParser.toAccount = pair.getValue();
                case FROM_TAG -> xmlParser.fromOpenId = pair.getValue();
                case TIMESTAMP_TAG -> xmlParser.timestamp = Long.parseLong(pair.getValue(), 10) * 1000;
                case MSG_TYPE_TAG -> xmlParser.msgType = pair.getValue();
                case MSG_ID_TAG -> xmlParser.msgId = pair.getValue();
                case MSG_DATA_ID_TAG -> xmlParser.msgDataId = pair.getValue();
                default -> {
                    if (pair.getKey().startsWith(ID_TAG)) {
                        if (xmlParser.idx.isEmpty()) {
                            xmlParser.idx = new ArrayList<>();
                        }
                        xmlParser.idx.add(pair.getValue());
                    } else {
                        xmlParser.handle(pair.getKey(), pair.getValue());
                    }
                }
            }
        }
        return xmlParser.build();
    }

    private static XmlParser newEventXmlParser(String eventType) {
        EventType event = EventType.getInstance(eventType);
        assert event != null;
        XmlParser eventXmlParser;
        switch (event) {
            case SUBSCRIBE -> eventXmlParser = new SubscribeEventXmlParser();
            case UNSUBSCRIBE -> eventXmlParser = new UnsubscribeEventXmlParser();
            case SCAN -> eventXmlParser = new ScanEventXmlParser();
            case LOCATION -> eventXmlParser = new LocationEventXmlParser();
            case MENU_CLICKED -> eventXmlParser = new ClickEventXmlParser();
            case MENU_VIEW -> eventXmlParser = new ViewEventXmlParser();
            case MENU_SCAN -> eventXmlParser = new MenuScanEventXmlParser();
            case MENU_SCANNING -> eventXmlParser = new MenuScanningEventXmlParser();
            case MENU_PHOTO -> eventXmlParser = new MenuPhotoEventXmlParser();
            case MENU_PICTURE -> eventXmlParser = new MenuPictureEventXmlParser();
            case MENU_ALBUM -> eventXmlParser = new MenuAlbumEventXmlParser();
            case MENU_LOCATING -> eventXmlParser = new MenuLocatingEventXmlParser();
            case MENU_VIEW_APPLET -> eventXmlParser = new MenuAppletEventXmlParser();

            case TEMPLATE_MSG_PUSH_RESULT -> eventXmlParser = new TemplateMsgPushResultEventXmlParser();

            case QUALIFICATION_VERIFY_SUCCESS -> eventXmlParser = new CertificationEventXmlParser();
            case QUALIFICATION_VERIFY_FAIL -> eventXmlParser = new CertificationFailedEventXmlParser();
            case NAMING_VERIFY_SUCCESS -> eventXmlParser = new TitleReviewEventXmlParser();
            case NAMING_VERIFY_FAIL -> eventXmlParser = new TitleReviewFailedEventXmlParser();
            case ANNUAL_RENEW -> eventXmlParser = new AnnualReviewEventXmlParser(false);
            case ANNUAL_VERIFY_EXPIRED -> eventXmlParser = new AnnualReviewEventXmlParser(true);
            default -> throw new UnsupportedOperationException("unknown event:" + eventType);
        }
        return eventXmlParser;
    }

    private static class TextXmlParser extends XmlParser {

        private static final String CONTENT_TAG = "Content";

        private String content;

        @Override
        void handle( String tagName, String text) {
            if (CONTENT_TAG.equals(tagName)) {
                content = text;
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.TextMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, content, idx);
        }
    }

    private static class ImageXmlParser extends XmlParser {

        private static final String PICTURE_URL_TAG = "PicUrl";

        private String pictureUrl;
        private String mediaId;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case PICTURE_URL_TAG-> pictureUrl = text;
                case MEDIA_ID_TAG -> mediaId = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.ImageMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, pictureUrl, mediaId, idx);
        }
    }

    private static class VoiceXmlParser extends XmlParser {

        private static final String FORMAT_TAG = "Format";

        private String mediaId;
        private String audioFormat;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case FORMAT_TAG-> audioFormat = text;
                case MEDIA_ID_TAG-> mediaId = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.VoiceMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, mediaId, audioFormat, idx);
        }
    }

    private static class VideoXmlParser extends XmlParser {

        private static final String THUMB_ID_TAG = "ThumbMediaId";

        protected String mediaId;
        protected String thumbId;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case THUMB_ID_TAG-> thumbId = text;
                case MEDIA_ID_TAG-> mediaId = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.VideoMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, mediaId, thumbId, idx);
        }
    }

    private static class VideoletXmlParser extends VideoXmlParser {

        @Override
        RequestMessage build() {
            return new RequestMessage.VideoletMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, mediaId, thumbId, idx);
        }
    }

    private static class LocationXmlParser extends XmlParser {

        private static final String LONGITUDE_TAG = "Location_X";
        private static final String LATITUDE_TAG = "Location_Y";
        private static final String SCALE_TAG = "Scale";
        private static final String LABEL_TAG = "Label";

        private String longitude;
        private String latitude;
        private int scale;
        private String label;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case LONGITUDE_TAG -> longitude = text;
                case LATITUDE_TAG -> latitude = text;
                case SCALE_TAG -> scale = Integer.parseInt(text, 10);
                case LABEL_TAG -> label = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.LocationMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, longitude, latitude, scale, label, idx);
        }
    }

    private static class LinkXmlParser extends XmlParser {

        private static final String TITLE_TAG = "Title";
        private static final String DESCRIPTION_TAG = "Description";
        private static final String URL_TAG = "Url";

        private String title;
        private String description;
        private String url;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case TITLE_TAG -> title = text;
                case DESCRIPTION_TAG -> description = text;
                case URL_TAG -> url = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new RequestMessage.LinkMessage(fromOpenId, toAccount, timestamp, msgId, msgDataId, title, description, url, idx);
        }

    }

    private static class TemplateMsgPushResultEventXmlParser extends XmlParser {
        private static final String MSG_ID_TAG = "MsgID";
        private static final String STATUS_TAG_TAG = "Status";

        private String msgId;
        private String status;

        @Override
        void handle( String tagName, String text) {
            switch (tagName) {
                case MSG_ID_TAG -> msgId = text;
                case STATUS_TAG_TAG -> status = text;
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.TemplateMsgPushResultEvent(fromOpenId, toAccount, timestamp, msgId, status);
        }

    }

    abstract static class EventXmlParser extends XmlParser {
        protected static final String EVENT_KEY_TAG = "EventKey";

        protected String eventKey;

        @Override
        void handle(String tagName, String text) {
            if (EVENT_KEY_TAG.equals(tagName)) {
                eventKey = text;
            }
        }

    }

    private static class UnsubscribeEventXmlParser extends XmlParser {

        @Override
        void handle(String tagName, String text) {

        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.UnsubscribeEvent(fromOpenId, toAccount, timestamp);
        }
    }

    private static class SubscribeEventXmlParser extends EventXmlParser {

        private static final String TICKET_TAG = "Ticket";

        protected String ticket;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            if (TICKET_TAG.equals(tagName)) {
                ticket = text;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.SubscribeEvent(fromOpenId, toAccount, timestamp, eventKey, ticket);
        }
    }

    private static class ScanEventXmlParser extends SubscribeEventXmlParser {

        @Override
        RequestMessage build() {
            return new InterestedEvent.ScanEvent(fromOpenId, toAccount, timestamp, eventKey, ticket);
        }
    }

    private static class LocationEventXmlParser extends XmlParser {

        private static final String LATITUDE_TAG = "Latitude";
        private static final String LONGITUDE_TAG = "Longitude";
        private static final String PRECISION_TAG = "Precision";

        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal precision;

        @Override
        void handle(String tagName, String text) {
            switch (tagName) {
                case LATITUDE_TAG -> latitude = new BigDecimal(text);
                case LONGITUDE_TAG -> longitude = new BigDecimal(text);
                case PRECISION_TAG -> precision = new BigDecimal(text);
                default -> log.warn("unknown tag: <{}> with value [{}]", tagName, text);
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.LocationEvent(fromOpenId, toAccount, timestamp, longitude, latitude, precision);
        }
    }

    private static class ClickEventXmlParser extends EventXmlParser {

        protected String eventKey;

        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuClickEvent(fromOpenId, toAccount, timestamp, eventKey);
        }
    }

    private static class ViewEventXmlParser extends EventXmlParser {

        private static final String MENU_ID_TAG = "MenuID";

        protected String menuId;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            if (MENU_ID_TAG.equals(tagName)) {
                menuId = text;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuViewEvent(fromOpenId, toAccount, timestamp, eventKey, menuId);
        }
    }

    private static class MenuScanEventXmlParser extends EventXmlParser {
        private static final String SCAN_INFO_TAG = "ScanCodeInfo";
        private static final String SCAN_TYPE_TAG = "ScanType";
        private static final String SCAN_RESULT_TAG = "ScanResult";

        protected boolean stepIn = false;
        protected String scanType;
        protected String scanResult;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            switch (tagName) {
                case SCAN_INFO_TAG -> stepIn = true;
                case SCAN_TYPE_TAG -> {
                    if (stepIn) {
                        scanType = text;
                        stepIn = false;
                    }
                }
                case SCAN_RESULT_TAG -> scanResult = text;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuScanEvent(fromOpenId, toAccount, timestamp, eventKey, scanType, scanResult);
        }
    }

    private static class MenuScanningEventXmlParser extends MenuScanEventXmlParser {
        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuScanningEvent(fromOpenId, toAccount, timestamp, eventKey, scanType, scanResult);
        }
    }

    private static class MenuPhotoEventXmlParser extends EventXmlParser {

        private static final String SEND_SUMMARY_TAG = "SendPicsInfo";
        private static final String COUNT_TAG = "Count";
        private static final String PICTURE_LIST_TAG = "PicList";
        private static final String ITEM_TAG = "item";
        private static final String DIGEST_TAG = "PicMd5Sum";

        protected boolean steppingCount = false;
        private boolean steppingItems = false;
        private boolean steppingDigest = false;
        protected int size;
        protected List<String> digests = new ArrayList<>();

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            switch (tagName) {
                case SEND_SUMMARY_TAG -> steppingCount = true;
                case COUNT_TAG -> {
                    if (steppingCount) {
                        size = Integer.parseInt(text);
                        steppingCount = false;
                    }
                }
                case PICTURE_LIST_TAG -> steppingItems = true;
                case ITEM_TAG -> steppingDigest = true;
                case DIGEST_TAG -> {
                    if (steppingItems && steppingDigest) {
                        digests.add(text);
                        steppingDigest = false;
                    }
                }
            }
        }

        @Override
        RequestMessage build() {
            assert size == digests.size();
            return new InterestedEvent.MenuPhotoEvent(fromOpenId, toAccount, timestamp, eventKey, digests);
        }
    }

    private static class MenuPictureEventXmlParser extends MenuPhotoEventXmlParser {
        @Override
        RequestMessage build() {
            assert size == digests.size();
            return new InterestedEvent.MenuPictureEvent(fromOpenId, toAccount, timestamp, eventKey, digests);
        }
    }

    private static class MenuAlbumEventXmlParser extends MenuPhotoEventXmlParser {
        @Override
        RequestMessage build() {
            assert size == digests.size();
            return new InterestedEvent.MenuAlbumEvent(fromOpenId, toAccount, timestamp, eventKey, digests);
        }
    }

    private static class MenuLocatingEventXmlParser extends EventXmlParser {
        private static final String LOCATION_TAG = "SendLocationInfo";
        private static final String LATITUDE_TAG = "Location_X";
        private static final String LONGITUDE_TAG = "Location_Y";
        private static final String SCALE_TAG = "Scale";
        private static final String ADDRESS_TAG = "Label";
        private static final String POI_NAME_TAG = "Poiname";

        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal scale;
        private String address;
        private String poiName;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            switch (tagName) {
                case LOCATION_TAG -> {}
                case LATITUDE_TAG -> latitude = new BigDecimal(text);
                case LONGITUDE_TAG -> longitude = new BigDecimal(text);
                case SCALE_TAG -> scale = new BigDecimal(text);
                case ADDRESS_TAG -> address = text;
                case POI_NAME_TAG -> poiName = text;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuLocatingEvent(fromOpenId, toAccount, timestamp, latitude, longitude, scale, address, poiName);
        }
    }

    private static class MenuAppletEventXmlParser extends ViewEventXmlParser {
        @Override
        RequestMessage build() {
            return new InterestedEvent.MenuViewEvent(fromOpenId, toAccount, timestamp, eventKey, menuId);
        }
    }


    private static class CertificationEventXmlParser extends EventXmlParser {
        private static final String EXPIRE_TAG = "ExpiredTime";

        protected long expireAt;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            if (EXPIRE_TAG.equals(tagName)) {
                expireAt = Long.parseLong(text, 10) * 1000;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.CertificationSuccessfulEvent(fromOpenId, toAccount, timestamp, expireAt);
        }
    }

    private static class CertificationFailedEventXmlParser extends EventXmlParser {
        private static final String FAIL_TIME_TAG = "FailTime";
        private static final String FAIL_REASON_TAG = "FailReason";

        protected long failTime;
        protected String failReason;

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            switch (tagName) {
                case FAIL_TIME_TAG -> failTime = Long.parseLong(text, 10) * 1000;
                case FAIL_REASON_TAG -> failReason = text;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.CertificationFailedEvent(fromOpenId, toAccount, timestamp, failTime, failReason);
        }
    }

    private static class TitleReviewEventXmlParser extends CertificationEventXmlParser {
        @Override
        RequestMessage build() {
            return new InterestedEvent.TitleReviewSuccessfulEvent(fromOpenId, toAccount, timestamp, expireAt);
        }
    }

    private static class TitleReviewFailedEventXmlParser extends CertificationFailedEventXmlParser {
        @Override
        RequestMessage build() {
            return new InterestedEvent.TitleReviewFailedEvent(fromOpenId, toAccount, timestamp, failTime, failReason);
        }
    }

    private static class AnnualReviewEventXmlParser extends EventXmlParser {
        private static final String EXPIRE_TAG = "ExpiredTime";

        private final boolean expired;
        private long expireAt;

        public AnnualReviewEventXmlParser(boolean expired) {
            this.expired = expired;
        }

        @Override
        void handle(String tagName, String text) {
            super.handle(tagName, text);
            if (EXPIRE_TAG.equals(tagName)) {
                expireAt = Long.parseLong(text, 10) * 1000;
            }
        }

        @Override
        RequestMessage build() {
            return new InterestedEvent.AnnualRenewEvent(fromOpenId, toAccount, timestamp, expired, expireAt);
        }
    }
}
