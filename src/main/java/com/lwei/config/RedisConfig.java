package com.lwei.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lwei.serializer.JodaDateTimeDeserializer;
import com.lwei.serializer.JodaDateTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;


@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {
	@Bean
	public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate redisTemplate = new RedisTemplate();
		redisTemplate.setConnectionFactory(redisConnectionFactory);

		// 首先解决key的序列化方式
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		redisTemplate.setKeySerializer(stringRedisSerializer);

		// 解决value的序列化方式
		//Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		redisTemplate.setValueSerializer(jackson2JsonRedisSerializer());

		return redisTemplate;
	}

	private Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer(){

		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
				new Jackson2JsonRedisSerializer<>(Object.class);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.configure(MapperFeature.USE_ANNOTATIONS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		// 此项必须配置，否则会报java.lang.ClassCastException: java.util.LinkedHashMap cannot be cast to XXX

		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(DateTime.class, new JodaDateTimeJsonSerializer());
		simpleModule.addDeserializer(DateTime.class, new JodaDateTimeDeserializer());
		objectMapper.registerModule(simpleModule);
		jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

		return jackson2JsonRedisSerializer;

	}
}
