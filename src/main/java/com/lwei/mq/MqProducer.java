package com.lwei.mq;

import com.alibaba.fastjson.JSON;
import com.lwei.dao.StockLogDOMapper;
import com.lwei.dataobject.StockLogDO;
import com.lwei.error.BusinessException;
import com.lwei.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


@Component
public class MqProducer {

	private DefaultMQProducer producer;

	private TransactionMQProducer transactionMQProducer;

	@Value("${mq.nameserver.addr}")
	private String nameAddr;

	@Value("${mq.topicname}")
	private String topicName;

	@Autowired
	private OrderService orderService;

	@Autowired
	private StockLogDOMapper stockLogDOMapper;

	@PostConstruct
	public void init() throws MQClientException {
		// 做mq producer的初始化
		producer = new DefaultMQProducer("producer_group");
		producer.setNamesrvAddr(nameAddr);
		producer.start();

		transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
		transactionMQProducer.setNamesrvAddr(nameAddr);
		transactionMQProducer.start();

		transactionMQProducer.setTransactionListener(new TransactionListener() {
			@Override
			public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
				Integer itemId = (Integer) ((Map) arg).get("itemId");
				Integer userId = (Integer) ((Map) arg).get("userId");
				Integer amount = (Integer) ((Map) arg).get("amount");
				Integer promoId = (Integer) ((Map) arg).get("promoId");
				String stockLogId = (String) ((Map) arg).get("stockLogId");

				// 真正要做的事，创建订单
				try {
					orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
				} catch (BusinessException e) {
					e.printStackTrace();
					StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
					stockLogDO.setStatus(3);
					stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

					return LocalTransactionState.ROLLBACK_MESSAGE;  // 若失败 回滚
				}
				return LocalTransactionState.COMMIT_MESSAGE;  // 成功 commit
			}

			@Override
			public LocalTransactionState checkLocalTransaction(MessageExt msg) {
				// 根据是否扣减库存成功，来判断要返回commit，rollback还是unknown
				String jsonString = new String(msg.getBody());
				Map<String,Object> map = JSON.parseObject(jsonString, Map.class);
				Integer itemId = (Integer) map.get("itemId");
				Integer amount = (Integer) map.get("amount");
				String stockLogId = (String) map.get("stockLogId");

				StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
				if(stockLogDO == null) {
					return LocalTransactionState.UNKNOW;
				}
				if(stockLogDO.getStatus().intValue() == 2) {
					return LocalTransactionState.COMMIT_MESSAGE;
				}else if(stockLogDO.getStatus().intValue() == 1) {
					return LocalTransactionState.UNKNOW;
				}
				return LocalTransactionState.ROLLBACK_MESSAGE;
			}
		});
	}

	// 事务型异步库存扣减消息
	public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) {
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("itemId", itemId);
		bodyMap.put("amount", amount);
		bodyMap.put("stockLogId", stockLogId);

		Map<String, Object> argsMap = new HashMap<>();
		argsMap.put("itemId", itemId);
		argsMap.put("amount", amount);
		argsMap.put("userId", userId);
		argsMap.put("promoId", promoId);
		argsMap.put("stockLogId", stockLogId);

		Message message = new Message(topicName, "increase",
				JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

		TransactionSendResult sendResult = null;

		try {
			sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
		} catch (MQClientException e) {
			e.printStackTrace();
			return false;
		}

		if(sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
			return false;
		} else if(sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
			return true;
		} else {
			return false;
		}
	}

	// 异步库存扣减消息
	public boolean asyncReduceStock(Integer itemId, Integer amount) {
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("itemId", itemId);
		bodyMap.put("amount", amount);

		Message message = new Message(topicName, "increase",
				JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
		try {
			producer.send(message, 100000);
		} catch (MQClientException e) {
			e.printStackTrace();
			return false;
		} catch (RemotingException e) {
			e.printStackTrace();
			return false;
		} catch (MQBrokerException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
