package com.zalora.logback.layouts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import com.zalora.logback.beans.BunyanBean;
import com.zalora.logback.beans.BunyanConfig;
import lombok.*;

/**
 * BunyanBean formatter for Logback
 * @author Wolfram Huesken <wolfram.huesken@zalora.com>
 */
public class BunyanLayout extends LayoutBase<ILoggingEvent> {

    @Getter @Setter
    private String appName;

    @Getter @Setter
    private boolean appendEol = true;

    @Getter @Setter
    private int stackTraceLevel = Level.ERROR_INT;

    public String doLayout(ILoggingEvent event) {
        BunyanBean bunyanBean = new BunyanBean(new BunyanConfig(appName, event, stackTraceLevel));
        StringBuffer jsonLogEvent = new StringBuffer(bunyanBean.toJson());

        if (appendEol) {
            jsonLogEvent.append(CoreConstants.LINE_SEPARATOR);
        }

        return jsonLogEvent.toString();
    }

    public String getContentType() {
        return "application/json";
    }
}
