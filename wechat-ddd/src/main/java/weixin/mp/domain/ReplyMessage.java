package weixin.mp.domain;

import java.util.List;

public interface ReplyMessage extends AbstractMessage {

    String INDENT = "  ";

    String NEW_LINE = "\n";

    String ACK_CONTENT = "success";

    @Override
    default String to() {
        return toOpenId();
    }

    String toOpenId();

    @Override
    default String from() {
        return fromAccount();
    }

    String fromAccount();

    default MessageType msgType() {
        String sn = getClass().getSimpleName();
        return MessageType.getInstance(sn.substring(0, sn.length() - "Message".length()).toLowerCase());
    }

    String buildXmlContent();

    default String toXml() {
        return """
            <xml>
              <ToUserName><![CDATA[%1$s]]></ToUserName>
              <FromUserName><![CDATA[%2$s]]></FromUserName>
              <CreateTime>%3$s</CreateTime>
              <MsgType><![CDATA[%4$s]]></MsgType>
              %5$s
            </xml>""".formatted(toOpenId(), fromAccount(), timestamp() / 1000, msgType().getValue(), buildXmlContent());
    }

    String buildJsonContent();

    default String toJson() {
        return """
                {"touser":"%1$s","msgtype":"%2$s","%2$s":%3$s}""".formatted(toOpenId(), msgType().getValue(), buildJsonContent());
    }

    ReplyMessage ACK = new ReplyMessage() {
        @Override
        public String toOpenId() {
            return null;
        }

        @Override
        public String fromAccount() {
            return null;
        }

        @Override
        public long timestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public String buildXmlContent() {
            return null;
        }

        @Override
        public String toXml() {
            return ACK_CONTENT;
        }

        @Override
        public String buildJsonContent() {
            // should not here
            return null;
        }
    };

    ReplyMessage NO_RETRY = new ReplyMessage() {
        @Override
        public String toOpenId() {
            return null;
        }

        @Override
        public String fromAccount() {
            return null;
        }

        @Override
        public long timestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public String buildXmlContent() {
            return null;
        }

        @Override
        public String toXml() {
            return "";
        }

        @Override
        public String buildJsonContent() {
            // should not here
            return null;
        }
    };

    record TextMessage(String toOpenId, String fromAccount, long timestamp, String content) implements ReplyMessage {

        @Override
        public String buildXmlContent() {
            return "  <Content><![CDATA[%1$s]]></Content>%2$s".formatted(content, NEW_LINE);
        }

        @Override
        public String buildJsonContent() {
            return "{\"content\":\"%1$s\"}".formatted(content);
        }
    }

    record ImageMessage(String toOpenId, String fromAccount, long timestamp, String mediaId) implements ReplyMessage {
        @Override
        public String buildXmlContent() {
            return """
                      <Image>
                          <MediaId><![CDATA[%1$s]]></MediaId>
                        </Image>""".formatted(mediaId);
        }

        @Override
        public String buildJsonContent() {
            return "{\"media_id\":\"%1$s\"}".formatted(mediaId);
        }
    }

    record VoiceMessage(String toOpenId, String fromAccount, long timestamp, String mediaId) implements ReplyMessage {
        @Override
        public String buildXmlContent() {
            return """
                      <Voice>
                          <MediaId><![CDATA[%1$s]]></MediaId>
                        </Voice>""".formatted(mediaId);
        }

        @Override
        public String buildJsonContent() {
            return "{\"media_id\":\"%1$s\"}".formatted(mediaId);
        }
    }

    record VideoMessage(String toOpenId, String fromAccount, long timestamp,
                        String mediaId, String thumbMediaId, String title, String description) implements ReplyMessage {
        @Override
        public String buildXmlContent() {
            return """
                      <Video>
                          <MediaId><![CDATA[%1$s]]></MediaId>
                          <Title><![CDATA[%2$s]]></Title>
                          <Description><![CDATA[%3$s]]></Description>
                        </Video>""".formatted(mediaId, title, description);
        }

        @Override
        public String buildJsonContent() {
            return "{\"media_id\":\"%1$s\",\"thumb_media_id\":\"%2$s\",\"title\":\"%3$s\",\"description\":\"%4$s\"}".formatted(mediaId, thumbMediaId, title, description);
        }
    }

    record MusicMessage(String toOpenId, String fromAccount, long timestamp,
                        String title, String description, String url, String hqUrl, String thumb) implements ReplyMessage {
        @Override
        public String buildXmlContent() {
            return """
                      <Music>
                          <Title><![CDATA[%1$s]]></Title>
                          <Description><![CDATA[%2$s]]></Description>
                          <MusicUrl><![CDATA[%3$s]]></MusicUrl>
                          <HQMusicUrl><![CDATA[%4$s]]></HQMusicUrl>
                          <ThumbMediaId><![CDATA[%5$s]]></ThumbMediaId>
                        </Music>""".formatted(title, description, url, hqUrl, thumb);
        }

        @Override
        public String buildJsonContent() {
            return "{\"title\":\"%1$s\",\"description\":\"%2$s\",\"musicurl\":\"%3$s\",\"hqmusicurl\":\"%4$s\",\"thumb_media_id\":\"%5$s\"}".formatted(title, description, url, hqUrl, thumb);
        }
    }

    record NewsMessage(String toOpenId, String fromAccount, long timestamp, List<Article> articles) implements ReplyMessage {
        @Override
        public String buildXmlContent() {
            StringBuilder sb = new StringBuilder(64 * articles.size());
            sb.append(INDENT).append("<ArticleCount>").append(articles.size()).append("</ArticleCount>").append(NEW_LINE);
            sb.append(INDENT).append("<Articles>").append(NEW_LINE);
            for (Article article : articles) {
                sb.append(INDENT).append(INDENT).append("<item>").append(NEW_LINE);

                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("<Title><![CDATA[").append(article.title).append("]]></Title>")
                        .append(NEW_LINE);
                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("<Description><![CDATA[").append(article.description).append("]]></Description>")
                        .append(NEW_LINE);
                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("<PicUrl><![CDATA[").append(article.picUrl).append("]]></PicUrl>")
                        .append(NEW_LINE);
                sb.append(INDENT).append(INDENT).append(INDENT)
                        .append("<Url><![CDATA[").append(article.picUrl).append("]]></Url>")
                        .append(NEW_LINE);

                sb.append(INDENT).append(INDENT).append("</item>").append(NEW_LINE);
            }
            sb.append(INDENT).append("</Articles>").append(NEW_LINE);
            return sb.toString();
        }

        @Override
        public String buildJsonContent() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"articles\":[");
            for (Article article : articles) {
                sb.append("{\"title\":\"").append(article.title)
                        .append("\",\"description\":\"").append(article.description)
                        .append("\",\"url\":\"").append(article.url)
                        .append("\",\"picurl\":\"").append(article.picUrl).append("\"}");
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    record Article(String title, String description, String picUrl, String url) {}
    // TODO
    // 图文消息

    // 菜单消息

    // 卡券

    // 小程序卡片

}
