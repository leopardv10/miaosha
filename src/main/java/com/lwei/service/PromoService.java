package com.lwei.service;

import com.lwei.service.model.PromoModel;

/**
 * Created by hzllb on 2018/11/18.
 */
public interface PromoService {
    // 根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    // 发布活动
    void publishPromo(Integer promoId);

    // 生成秒杀活动令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);
}
