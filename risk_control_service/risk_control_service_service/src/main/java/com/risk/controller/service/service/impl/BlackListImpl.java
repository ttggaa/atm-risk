package com.risk.controller.service.service.impl;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.BlackListDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.service.BlackListService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class BlackListImpl implements BlackListService {
    @Resource
    private BlacklistPhoneDao blacklistPhoneDao;
    @Resource
    private BlacklistIdcardDao blacklistIdcardDao;
    @Resource
    private BlacklistIdfaDao blacklistIdfaDao;
    @Resource
    private BlacklistImeiDao blacklistImeiDao;
    @Resource
    private BlacklistMacDao blacklistMacDao;
    @Resource
    private BlacklistAreaIdcardDao blacklistAreaIdcardDao;


    @Override
    public ResponseEntity queryBlacklist(BlackListDTO dto) {
        if (null == dto) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "接口参数异常", null);
        }
        if (StringUtils.isNotBlank(dto.getPhones())) {
            String str = dto.getPhones();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            List<String> black = blacklistPhoneDao.getListByPhones(list);
            if (black.size() > 0) {
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
            }
        }

        if (StringUtils.isNotBlank(dto.getIdCards())) {
            String str = dto.getIdCards();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            List<String> black = blacklistIdcardDao.getListByIdcards(list);
            if (black.size() > 0) {
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
            }

            for (String cardNo : list) {
                int count = blacklistAreaIdcardDao.getIdcardHitCount(cardNo);
                if (count > 0) {
                    return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
                }
            }
        }

        if (StringUtils.isNotBlank(dto.getIdfas())) {
            String str = dto.getIdfas();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            List<String> black = blacklistIdfaDao.getListByIdfas(list);
            if (black.size() > 0) {
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
            }
        }

        if (StringUtils.isNotBlank(dto.getImeis())) {
            String str = dto.getImeis();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            List<String> black = blacklistImeiDao.getListByImeis(list);
            if (black.size() > 0) {
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
            }
        }

        if (StringUtils.isNotBlank(dto.getMacs())) {
            String str = dto.getMacs();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            List<String> black = blacklistMacDao.getListByMacs(list);
            if (black.size() > 0) {
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 1);
            }
        }

        return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, 0);
    }

    @Override
    public ResponseEntity addBlacklist(BlackListDTO dto) {
        if (null == dto) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "接口参数异常", null);
        }
        if (StringUtils.isNotBlank(dto.getPhones())) {
            String str = dto.getPhones();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            for (String string : list) {
                BlacklistPhone blacklistPhone = blacklistPhoneDao.getLastOneByPhone(string);
                if (null == blacklistPhone) {
                    blacklistPhone = new BlacklistPhone();
                    blacklistPhone.setPhone(string);
                    blacklistPhone.setEnable(1);
                    blacklistPhone.setSource(null == dto.getSource() ? "" : dto.getSource());
                    blacklistPhone.setRemark("");
                    blacklistPhone.setAddTime(System.currentTimeMillis());
                    blacklistPhone.setUpdateTime(System.currentTimeMillis());
                    blacklistPhoneDao.insert(blacklistPhone);
                }
            }
        }

        if (StringUtils.isNotBlank(dto.getIdCards())) {
            String str = dto.getIdCards();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            for (String string : list) {
                BlacklistIdcard blacklistIdcard = blacklistIdcardDao.getLastOneByIdCard(string);
                if (null == blacklistIdcard) {
                    blacklistIdcard = new BlacklistIdcard();
                    blacklistIdcard.setIdCard(string);
                    blacklistIdcard.setEnable(1);
                    blacklistIdcard.setSource(null == dto.getSource() ? "" : dto.getSource());
                    blacklistIdcard.setRemark("");
                    blacklistIdcard.setAddTime(System.currentTimeMillis());
                    blacklistIdcard.setUpdateTime(System.currentTimeMillis());
                    blacklistIdcardDao.insert(blacklistIdcard);
                }
            }
        }

        if (StringUtils.isNotBlank(dto.getIdfas())) {
            String str = dto.getIdfas();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            for (String string : list) {
                BlacklistIdfa blacklistIdfa = blacklistIdfaDao.getLastOneByIdfa(string);
                if (null == blacklistIdfa) {
                    blacklistIdfa = new BlacklistIdfa();
                    blacklistIdfa.setIdfa(string);
                    blacklistIdfa.setEnable(1);
                    blacklistIdfa.setSource(null == dto.getSource() ? "" : dto.getSource());
                    blacklistIdfa.setRemark("");
                    blacklistIdfa.setAddTime(System.currentTimeMillis());
                    blacklistIdfa.setUpdateTime(System.currentTimeMillis());
                    blacklistIdfaDao.insert(blacklistIdfa);
                }
            }
        }

        if (StringUtils.isNotBlank(dto.getImeis())) {
            String str = dto.getImeis();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            for (String string : list) {
                BlacklistImei blacklistImei = blacklistImeiDao.getLastOneByImei(string);
                if (null == blacklistImei) {
                    blacklistImei = new BlacklistImei();
                    blacklistImei.setImei(string);
                    blacklistImei.setEnable(1);
                    blacklistImei.setSource(null == dto.getSource() ? "" : dto.getSource());
                    blacklistImei.setRemark("");
                    blacklistImei.setAddTime(System.currentTimeMillis());
                    blacklistImei.setUpdateTime(System.currentTimeMillis());
                    blacklistImeiDao.insert(blacklistImei);
                }
            }
        }

        if (StringUtils.isNotBlank(dto.getMacs())) {
            String str = dto.getMacs();
            String[] objs = str.split(",");
            List<String> list = Arrays.asList(objs);
            for (String string : list) {
                BlacklistMac blacklistMac = blacklistMacDao.getLastOneByMac(string);
                if (null == blacklistMac) {
                    blacklistMac = new BlacklistMac();
                    blacklistMac.setMac(string);
                    blacklistMac.setEnable(1);
                    blacklistMac.setSource(null == dto.getSource() ? "" : dto.getSource());
                    blacklistMac.setRemark("");
                    blacklistMac.setAddTime(System.currentTimeMillis());
                    blacklistMac.setUpdateTime(System.currentTimeMillis());
                    blacklistMacDao.insert(blacklistMac);
                }
            }
        }
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }
}
