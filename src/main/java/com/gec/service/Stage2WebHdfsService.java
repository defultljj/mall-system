package com.gec.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class Stage2WebHdfsService {
    private final String webUrl;

    public Stage2WebHdfsService(@Value("${stage2.hdfs.web-url}") String webUrl) {
        this.webUrl = webUrl;
    }

    public void upload(Path localFile, String hdfsPath) throws Exception {
        URL createUrl = new URL(webUrl + "/webhdfs/v1" + hdfsPath + "?op=CREATE&overwrite=true&user.name=root");
        HttpURLConnection create = (HttpURLConnection) createUrl.openConnection();
        create.setRequestMethod("PUT");
        create.setInstanceFollowRedirects(false);
        if (create.getResponseCode() != 307) {
            throw new IllegalStateException("WebHDFS 创建请求失败: HTTP " + create.getResponseCode());
        }
        URI redirect = URI.create(create.getHeaderField("Location"));
        URI nameNode = URI.create(webUrl);
        URI dataNode = new URI(redirect.getScheme(), redirect.getUserInfo(), nameNode.getHost(), redirect.getPort(),
                redirect.getPath(), redirect.getQuery(), redirect.getFragment());
        HttpURLConnection write = (HttpURLConnection) dataNode.toURL().openConnection();
        write.setRequestMethod("PUT");
        write.setDoOutput(true);
        write.setFixedLengthStreamingMode(Files.size(localFile));
        try (OutputStream output = write.getOutputStream()) { Files.copy(localFile, output); }
        if (write.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
            throw new IllegalStateException("WebHDFS 写入失败: HTTP " + write.getResponseCode());
        }
    }
}
