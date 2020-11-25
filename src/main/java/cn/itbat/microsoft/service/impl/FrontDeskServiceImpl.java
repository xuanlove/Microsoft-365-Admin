package cn.itbat.microsoft.service.impl;

import cn.itbat.microsoft.config.GraphProperties;
import cn.itbat.microsoft.model.entity.InvitationCode;
import cn.itbat.microsoft.service.FrontDeskService;
import cn.itbat.microsoft.service.InvitationCodeService;
import cn.itbat.microsoft.service.Microsoft365Service;
import cn.itbat.microsoft.vo.FontSkuSku;
import cn.itbat.microsoft.vo.FontUser;
import cn.itbat.microsoft.vo.GraphUserVo;
import cn.itbat.microsoft.vo.SubscribedSkuVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huahui.wu
 * @date 2020年11月24日 16:34:12
 */
@Service
public class FrontDeskServiceImpl implements FrontDeskService {

    @Resource
    private Microsoft365Service microsoft365Service;

    @Resource
    private InvitationCodeService invitationCodeService;

    @Resource
    private GraphProperties graphProperties;

    @Value("${spring.mail.username}")
    private List<String> subs;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FontUser create(GraphUserVo graphUserVo) {
        // 判断邀请码是否有效
        InvitationCode invitationCode = invitationCodeService.selectByCode(graphUserVo.getCode());
        if (invitationCode == null || !invitationCode.getValid()) {
            throw new RuntimeException("邀请码已失效！");
        }
        GraphUserVo userVo = microsoft365Service.create(graphUserVo);
        invitationCode.setValid(Boolean.FALSE);
        invitationCode.setExpirationTime(new Date());
        invitationCode.setInvitedUser(userVo.getUserPrincipalName());
        invitationCode.setSubscribe(StringUtils.isEmpty(graphUserVo.getSkuType()) ? "" : graphProperties.getSubConfig(graphUserVo.getSkuType()).getDisplayName());
        invitationCodeService.update(invitationCode);
        return FontUser.builder().displayName(userVo.getDisplayName()).userPrincipalName(userVo.getUserPrincipalName()).password(userVo.getPassword()).build();
    }

    @Override
    public List<FontSkuSku> listLicense() {
        List<String> appNames = graphProperties.getConfigs().stream().map(GraphProperties.GraphConfig::getAppName).collect(Collectors.toList());
        List<FontSkuSku> fontSkuSkus = new ArrayList<>();
        for (String appName : appNames) {
            List<SubscribedSkuVo> subscribed = microsoft365Service.getSubscribed(appName);
            if (!CollectionUtils.isEmpty(subscribed)) {
                for (SubscribedSkuVo subscribedSkuVo : subscribed) {
                    FontSkuSku fontSkuSku = new FontSkuSku();
                    BeanUtils.copyProperties(subscribedSkuVo, fontSkuSku);
                    fontSkuSkus.add(fontSkuSku);
                }
            }
        }
        return fontSkuSkus;
    }
}