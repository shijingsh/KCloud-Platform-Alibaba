/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.admin.server.application.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.admin.server.application.service.SysMessageApplicationService;
import org.laokou.admin.server.domain.sys.entity.SysMessageDO;
import org.laokou.admin.server.domain.sys.entity.SysMessageDetailDO;
import org.laokou.admin.server.domain.sys.repository.service.SysMessageDetailService;
import org.laokou.admin.server.domain.sys.repository.service.SysMessageService;
import org.laokou.admin.client.dto.MessageDTO;
import org.laokou.admin.server.infrastructure.feign.im.ImApiFeignClient;
import org.laokou.admin.server.interfaces.qo.SysMessageQo;
import org.laokou.admin.client.vo.MessageDetailVO;
import org.laokou.admin.client.vo.SysMessageVO;
import org.apache.commons.collections.CollectionUtils;
import org.laokou.auth.client.utils.UserUtil;
import org.laokou.common.core.utils.ConvertUtil;
import org.laokou.common.core.utils.DateUtil;
import org.laokou.common.i18n.utils.ValidatorUtil;
import org.laokou.common.mybatisplus.utils.BatchUtil;
import org.laokou.im.client.PushMsgDTO;
import org.laokou.common.tenant.processor.DsTenantProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * @author laokou
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysMessageApplicationServiceImpl implements SysMessageApplicationService {

    private final SysMessageService sysMessageService;

    private final SysMessageDetailService sysMessageDetailService;
    private final ImApiFeignClient imApiFeignClient;
    private final BatchUtil<SysMessageDetailDO> batchUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS(DsTenantProcessor.TENANT)
    public Boolean insertMessage(MessageDTO dto) {
        ValidatorUtil.validateEntity(dto);
        SysMessageDO messageDO = ConvertUtil.sourceToTarget(dto, SysMessageDO.class);
        messageDO.setCreateDate(DateUtil.now());
        messageDO.setCreator(UserUtil.getUserId());
        sysMessageService.save(messageDO);
        Set<String> receiver = dto.getReceiver();
        Iterator<String> iterator = receiver.iterator();
        List<SysMessageDetailDO> detailDOList = new ArrayList<>(receiver.size());
        while (iterator.hasNext()) {
            String next = iterator.next();
            SysMessageDetailDO detailDO = new SysMessageDetailDO();
            detailDO.setMessageId(messageDO.getId());
            detailDO.setUserId(Long.valueOf(next));
            detailDO.setCreateDate(DateUtil.now());
            detailDO.setCreator(UserUtil.getUserId());
            detailDOList.add(detailDO);
        }
        if (CollectionUtils.isNotEmpty(detailDOList)) {
            batchUtil.insertBatch(detailDOList,500,sysMessageDetailService);
        }
        // 平台-发送消息
        pushMsg(receiver);
        return true;
    }

    private void pushMsg(Set<String> receiver) {
        if (CollectionUtils.isNotEmpty(receiver)) {
            PushMsgDTO pushMsgDTO = new PushMsgDTO();
            pushMsgDTO.setMsg("您有一条未读消息，请注意查收");
            pushMsgDTO.setReceiver(receiver);
            // 推送消息
            imApiFeignClient.push(pushMsgDTO);
        }
    }

    @Override
    @DS(DsTenantProcessor.TENANT)
    public IPage<SysMessageVO> queryMessagePage(SysMessageQo qo) {
        ValidatorUtil.validateEntity(qo);
        IPage<SysMessageVO> page = new Page<>(qo.getPageNum(),qo.getPageSize());
        return sysMessageService.getMessageList(page,qo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS(DsTenantProcessor.TENANT)
    public MessageDetailVO getMessageByDetailId(Long id) {
        Integer version = sysMessageDetailService.getVersion(id);
        sysMessageService.readMessage(id,version);
        return sysMessageService.getMessageByDetailId(id);
    }

    @Override
    @DS(DsTenantProcessor.TENANT)
    public MessageDetailVO getMessageById(Long id) {
        return sysMessageService.getMessageById(id);
    }

    @Override
    @DS(DsTenantProcessor.TENANT)
    public IPage<SysMessageVO> getUnReadList(SysMessageQo qo) {
        IPage<SysMessageVO> page = new Page<>(qo.getPageNum(),qo.getPageSize());
        final Long userId = UserUtil.getUserId();
        return sysMessageService.getUnReadList(page,qo.getType(),userId);
    }

    @Override
    @DS(DsTenantProcessor.TENANT)
    public Long unReadCount() {
        final Long userId = UserUtil.getUserId();
        long count = sysMessageDetailService.messageCount(userId);
        return count;
    }

}
