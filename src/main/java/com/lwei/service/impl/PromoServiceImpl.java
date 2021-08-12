package com.lwei.service.impl;

import com.lwei.dao.PromoDOMapper;
import com.lwei.dataobject.PromoDO;
import com.lwei.error.BusinessException;
import com.lwei.error.EmBusinessError;
import com.lwei.service.ItemService;
import com.lwei.service.PromoService;
import com.lwei.service.UserService;
import com.lwei.service.model.ItemModel;
import com.lwei.service.model.PromoModel;
import com.lwei.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by hzllb on 2018/11/18.
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel == null){
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        // 通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        // 将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        // 将秒杀大闸的限制数字设置到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue() * 5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        // 判断库存是否售罄，若对应的key存在则下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel == null){
            return null;
        }

        // 判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }

        if(promoModel.getStatus() != 2) {
            return null;
        }

        // 判断item信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null){
            return null;
        }

        // 判断用户是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null){
            return null;
        }

        // 获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if(result < 0) {
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promo_token" + promoId + "userId" + userId
                + "itemId" + itemId, token);
        redisTemplate.expire("promo_token" + promoId + "userId" + userId
                + "itemId" + itemId, 5, TimeUnit.MINUTES);

        return token;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO){
        if(promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}