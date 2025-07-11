/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.web.controller;

import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.service.ChargePointHelperService;
import de.rwth.idsg.steve.service.ChargePointServiceClient;
import de.rwth.idsg.steve.service.OcppTagService;
import de.rwth.idsg.steve.web.dto.ocpp.CancelReservationParams;
import de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyEnum;
import de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyReadWriteEnum;
import de.rwth.idsg.steve.web.dto.ocpp.DataTransferParams;
import de.rwth.idsg.steve.web.dto.ocpp.GetConfigurationParams;
import de.rwth.idsg.steve.web.dto.ocpp.MultipleChargePointSelect;
import de.rwth.idsg.steve.web.dto.ocpp.ReserveNowParams;
import de.rwth.idsg.steve.web.dto.ocpp.SendLocalListParams;
import de.rwth.idsg.steve.web.dto.ocpp.SendLocalListUpdateType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyReadWriteEnum.RW;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 07.11.2014
 */
@Controller
@RequestMapping(value = "/manager/operations/v1.5")
public class Ocpp15Controller extends Ocpp12Controller {

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    private static final String RESERVE_PATH = "/ReserveNow";
    private static final String CANCEL_RESERV_PATH = "/CancelReservation";
    private static final String DATA_TRANSFER_PATH = "/DataTransfer";
    protected static final String GET_CONF_PATH = "/GetConfiguration";
    private static final String GET_LIST_VERSION_PATH = "/GetLocalListVersion";
    private static final String SEND_LIST_PATH = "/SendLocalList";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final List<String> SUPPORTED_OPS = Arrays.asList(
            "ChangeAvailability",
            "ChangeConfiguration",
            "ClearCache",
            "DataTransfer",
            "GetConfiguration",
            "GetDiagnostics",
            "GetLocalListVersion",
            "RemoteStartTransaction",
            "RemoteStopTransaction",
            "ReserveNow",
            "Reset",
            "SendLocalList",
            "UnlockConnector",
            "UpdateFirmware"
    );

    public Ocpp15Controller(ChargePointHelperService chargePointHelperService, OcppTagService ocppTagService,
                            ChargePointServiceClient chargePointServiceClient) {
        super(chargePointHelperService, ocppTagService, chargePointServiceClient);
    }

    @Override
    protected void setCommonAttributes(Model model) {
        model.addAttribute("cpList", chargePointHelperService.getChargePoints(OcppVersion.V_15));
        model.addAttribute("opVersion", "v1.5");
        model.addAttribute("supportedOps", SUPPORTED_OPS);
    }

    @Override
    protected void setCommonAttributesForTx(Model model) {
        setCommonAttributes(model);
    }

    @Override
    protected Map<String, String> getConfigurationKeys(ConfigurationKeyReadWriteEnum confEnum) {
        // this conf enum is only relevant for versions >= occp 1.6
        return ConfigurationKeyEnum.OCPP_15_MAP;
    }

    @Override
    protected String getRedirectPath() {
        return "redirect:/manager/operations/v1.5/ChangeAvailability";
    }

    @Override
    protected String getPrefix() {
        return "op15";
    }

    private void setAllUserIdTagList(Model model) {
        model.addAttribute("idTagList", ocppTagService.getIdTags());
    }

    // -------------------------------------------------------------------------
    // Http methods (GET)
    // -------------------------------------------------------------------------

    @RequestMapping(value = RESERVE_PATH, method = RequestMethod.GET)
    public String getReserveNow(Model model) {
        setCommonAttributes(model);
        setActiveUserIdTagList(model);
        model.addAttribute(PARAMS, new ReserveNowParams());
        return getPrefix() + RESERVE_PATH;
    }

    @RequestMapping(value = CANCEL_RESERV_PATH, method = RequestMethod.GET)
    public String getCancelReserv(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new CancelReservationParams());
        return getPrefix() + CANCEL_RESERV_PATH;
    }

    @RequestMapping(value = DATA_TRANSFER_PATH, method = RequestMethod.GET)
    public String getDataTransfer(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new DataTransferParams());
        return getPrefix() + DATA_TRANSFER_PATH;
    }

    @RequestMapping(value = GET_CONF_PATH, method = RequestMethod.GET)
    public String getGetConf(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new GetConfigurationParams());
        model.addAttribute("ocppConfKeys", getConfigurationKeys(RW));
        return getPrefix() + GET_CONF_PATH;
    }

    @RequestMapping(value = GET_LIST_VERSION_PATH, method = RequestMethod.GET)
    public String getListVersion(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new MultipleChargePointSelect());
        return getPrefix() + GET_LIST_VERSION_PATH;
    }

    @RequestMapping(value = SEND_LIST_PATH, method = RequestMethod.GET)
    public String getSendList(Model model) {
        setCommonAttributes(model);
        setAllUserIdTagList(model);
        model.addAttribute(PARAMS, new SendLocalListParams());
        model.addAttribute("updateTypes", SendLocalListUpdateType.values());
        return getPrefix() + SEND_LIST_PATH;
    }

    // -------------------------------------------------------------------------
    // Http methods (POST)
    // -------------------------------------------------------------------------

    @RequestMapping(value = RESERVE_PATH, method = RequestMethod.POST)
    public String postReserveNow(@Valid @ModelAttribute(PARAMS) ReserveNowParams params,
                                 BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            setActiveUserIdTagList(model);
            return getPrefix() + RESERVE_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.reserveNow(params);
    }

    @RequestMapping(value = CANCEL_RESERV_PATH, method = RequestMethod.POST)
    public String postCancelReserv(@Valid @ModelAttribute(PARAMS) CancelReservationParams params,
                                   BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + CANCEL_RESERV_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.cancelReservation(params);
    }

    @RequestMapping(value = DATA_TRANSFER_PATH, method = RequestMethod.POST)
    public String postDataTransfer(@Valid @ModelAttribute(PARAMS) DataTransferParams params,
                                   BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + DATA_TRANSFER_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.dataTransfer(params);
    }

    @RequestMapping(value = GET_CONF_PATH, method = RequestMethod.POST)
    public String postGetConf(@Valid @ModelAttribute(PARAMS) GetConfigurationParams params,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            model.addAttribute("ocppConfKeys", getConfigurationKeys(RW));
            return getPrefix() + GET_CONF_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.getConfiguration(params);
    }

    @RequestMapping(value = GET_LIST_VERSION_PATH, method = RequestMethod.POST)
    public String postListVersion(@Valid @ModelAttribute(PARAMS) MultipleChargePointSelect params,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + GET_LIST_VERSION_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.getLocalListVersion(params);
    }

    @RequestMapping(value = SEND_LIST_PATH, method = RequestMethod.POST)
    public String postSendList(@Valid @ModelAttribute(PARAMS) SendLocalListParams params,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            setAllUserIdTagList(model);
            return getPrefix() + SEND_LIST_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.sendLocalList(params);
    }
}
