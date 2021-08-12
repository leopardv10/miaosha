package com.lwei.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> {
	@Override
	public void customize(ConfigurableJettyWebServerFactory configurableWebServerFactory) {
		((TomcatServletWebServerFactory)configurableWebServerFactory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
			@Override
			public void customize(Connector connector) {
				Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
				// 定制化Keepalivetimeout
				protocol.setKeepAliveTimeout(30000);
				// 当客户端发送超过10000个请求自动断开keepalive连接
				protocol.setMaxKeepAliveRequests(10000);
			}
		});
	}
}
