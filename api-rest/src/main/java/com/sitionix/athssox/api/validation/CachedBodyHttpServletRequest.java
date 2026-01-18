package com.sitionix.athssox.api.validation;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    private final Charset charset;

    public CachedBodyHttpServletRequest(final HttpServletRequest request, final long maxBodyBytes) throws IOException {
        super(request);
        this.charset = resolveCharset(request.getCharacterEncoding());
        this.cachedBody = readBody(request.getInputStream(), maxBodyBytes);
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), this.charset));
    }

    @Override
    public int getContentLength() {
        return this.cachedBody.length;
    }

    @Override
    public long getContentLengthLong() {
        return this.cachedBody.length;
    }

    private static Charset resolveCharset(final String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }

    private static byte[] readBody(final InputStream inputStream, final long maxBodyBytes) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[1024];
        long total = 0;
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            total += read;
            if (total > maxBodyBytes) {
                throw new RequestBodyTooLargeException("Request body exceeds limit");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        private CachedBodyServletInputStream(final byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return this.inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            // async IO not needed
        }

        @Override
        public int read() {
            return this.inputStream.read();
        }
    }
}
