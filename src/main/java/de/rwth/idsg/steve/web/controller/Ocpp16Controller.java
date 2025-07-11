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
import de.rwth.idsg.steve.repository.ChargingProfileRepository;
import de.rwth.idsg.steve.service.ChargePointHelperService;
import de.rwth.idsg.steve.service.ChargePointServiceClient;
import de.rwth.idsg.steve.service.OcppTagService;
import de.rwth.idsg.steve.web.dto.ocpp.ChangeConfigurationParams;
import de.rwth.idsg.steve.web.dto.ocpp.ClearChargingProfileParams;
import de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyEnum;
import de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyReadWriteEnum;
import de.rwth.idsg.steve.web.dto.ocpp.GetCompositeScheduleParams;
import de.rwth.idsg.steve.web.dto.ocpp.GetConfigurationParams;
import de.rwth.idsg.steve.web.dto.ocpp.SetChargingProfileParams;
import de.rwth.idsg.steve.web.dto.ocpp.TriggerMessageEnum;
import de.rwth.idsg.steve.web.dto.ocpp.TriggerMessageParams;
import ocpp.cp._2015._10.ChargingRateUnitType;
import ocpp.cs._2015._10.RegistrationStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyReadWriteEnum.R;
import static de.rwth.idsg.steve.web.dto.ocpp.ConfigurationKeyReadWriteEnum.RW;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 15.03.2018
 */
@Controller
@RequestMapping(value = "/manager/operations/v1.6")
public class Ocpp16Controller extends Ocpp15Controller {

    private final ChargingProfileRepository chargingProfileRepository;

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    private static final String GET_COMPOSITE_PATH = "/GetCompositeSchedule";
    private static final String CLEAR_CHARGING_PATH = "/ClearChargingProfile";
    private static final String SET_CHARGING_PATH = "/SetChargingProfile";
    private static final String TRIGGER_MESSAGE_PATH = "/TriggerMessage";

    private static final List<String> SUPPORTED_OPS = Arrays.asList(
            "ChangeAvailability",
            "ChangeConfiguration",
            "ClearCache",
            "DataTransfer",
            "GetCompositeSchedule",
            "GetConfiguration",
            "GetDiagnostics",
            "GetLocalListVersion",
            "RemoteStartTransaction",
            "RemoteStopTransaction",
            "ReserveNow",
            "Reset",
            "SendLocalList",
            "SetChargingProfile",
            "TriggerMessage",
            "UnlockConnector",
            "UpdateFirmware"
    );

    public Ocpp16Controller(ChargePointHelperService chargePointHelperService, OcppTagService ocppTagService,
                            ChargePointServiceClient chargePointServiceClient,
                            ChargingProfileRepository chargingProfileRepository) {
      super(chargePointHelperService, ocppTagService, chargePointServiceClient);
        this.chargingProfileRepository = chargingProfileRepository;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Override
    protected void setCommonAttributesForTx(Model model) {
        model.addAttribute("cpList", chargePointHelperService.getChargePoints(OcppVersion.V_16));
        model.addAttribute("opVersion", "v1.6");
        model.addAttribute("supportedOps", SUPPORTED_OPS);
    }

    /**
     * From OCPP 1.6 spec: "While in pending state, the following Central
     * System initiated messages are not allowed: RemoteStartTransaction.req
     * and RemoteStopTransaction.req"
     *
     * Conversely, it means all other operations are allowed for pending state.
     */
    @Override
    protected void setCommonAttributes(Model model) {
        List<RegistrationStatus> inStatusFilter = Arrays.asList(RegistrationStatus.ACCEPTED, RegistrationStatus.PENDING);
        model.addAttribute("cpList", chargePointHelperService.getChargePoints(OcppVersion.V_16, inStatusFilter));
        model.addAttribute("opVersion", "v1.6");
        model.addAttribute("supportedOps", SUPPORTED_OPS);
    }

    @Override
    protected Map<String, String> getConfigurationKeys(ConfigurationKeyReadWriteEnum confEnum) {
        switch (confEnum) {
            case R:
                return ConfigurationKeyEnum.OCPP_16_MAP_R;
            case RW:
                return ConfigurationKeyEnum.OCPP_16_MAP_RW;
            default:
                return Collections.emptyMap();
        }
    }

    @Override
    protected String getRedirectPath() {
        return "redirect:/manager/operations/v1.6/ChangeAvailability";
    }

    @Override
    protected String getPrefix() {
        return "op16";
    }

    // -------------------------------------------------------------------------
    // Old Http methods with changed logic
    // -------------------------------------------------------------------------

    @RequestMapping(value = GET_CONF_PATH, method = RequestMethod.GET)
    public String getGetConf(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new GetConfigurationParams());
        model.addAttribute("ocppConfKeys", getConfigurationKeys(R));
        return getPrefix() + GET_CONF_PATH;
    }

    @RequestMapping(value = CHANGE_CONF_PATH, method = RequestMethod.GET)
    public String getChangeConf(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new ChangeConfigurationParams());
        model.addAttribute("configurationKeyTypes", ChangeConfigurationParams.ConfigurationKeyType.values());
        model.addAttribute("ocppConfKeys", getConfigurationKeys(RW));
        return getPrefix() + CHANGE_CONF_PATH;
    }

    @RequestMapping(value = GET_CONF_PATH, method = RequestMethod.POST)
    public String postGetConf(@Valid @ModelAttribute(PARAMS) GetConfigurationParams params,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            model.addAttribute("ocppConfKeys", getConfigurationKeys(R));
            return getPrefix() + GET_CONF_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.getConfiguration(params);
    }

    // -------------------------------------------------------------------------
    // New Http methods (GET)
    // -------------------------------------------------------------------------

    @RequestMapping(value = GET_COMPOSITE_PATH, method = RequestMethod.GET)
    public String getGetCompositeSchedule(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new GetCompositeScheduleParams());
        model.addAttribute("chargingRateUnits", ChargingRateUnitType.values());
        return getPrefix() + GET_COMPOSITE_PATH;
    }

    @RequestMapping(value = CLEAR_CHARGING_PATH, method = RequestMethod.GET)
    public String getClearChargingProfile(Model model) {
        setCommonAttributes(model);
        model.addAttribute("profileList", chargingProfileRepository.getBasicInfo());
        model.addAttribute(PARAMS, new ClearChargingProfileParams());
        return getPrefix() + CLEAR_CHARGING_PATH;
    }

    @RequestMapping(value = SET_CHARGING_PATH, method = RequestMethod.GET)
    public String getSetChargingProfile(Model model) {
        setCommonAttributes(model);
        model.addAttribute("profileList", chargingProfileRepository.getBasicInfo());
        model.addAttribute(PARAMS, new SetChargingProfileParams());
        return getPrefix() + SET_CHARGING_PATH;
    }

    @RequestMapping(value = TRIGGER_MESSAGE_PATH, method = RequestMethod.GET)
    public String getTriggerMessage(Model model) {
        setCommonAttributes(model);
        model.addAttribute(PARAMS, new TriggerMessageParams());
        model.addAttribute("triggerMessages", TriggerMessageEnum.values());
        return getPrefix() + TRIGGER_MESSAGE_PATH;
    }

    // -------------------------------------------------------------------------
    // Http methods (POST)
    // -------------------------------------------------------------------------

    @RequestMapping(value = TRIGGER_MESSAGE_PATH, method = RequestMethod.POST)
    public String postTriggerMessage(@Valid @ModelAttribute(PARAMS) TriggerMessageParams params,
                                     BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + TRIGGER_MESSAGE_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.triggerMessage(params);
    }

    @RequestMapping(value = SET_CHARGING_PATH, method = RequestMethod.POST)
    public String postSetChargingProfile(@Valid @ModelAttribute(PARAMS) SetChargingProfileParams params,
                                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + SET_CHARGING_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.setChargingProfile(params);
    }

    @RequestMapping(value = CLEAR_CHARGING_PATH, method = RequestMethod.POST)
    public String postClearChargingProfile(@Valid @ModelAttribute(PARAMS) ClearChargingProfileParams params,
                                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + CLEAR_CHARGING_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.clearChargingProfile(params);
    }

    @RequestMapping(value = GET_COMPOSITE_PATH, method = RequestMethod.POST)
    public String postGetCompositeSchedule(@Valid @ModelAttribute(PARAMS) GetCompositeScheduleParams params,
                                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            setCommonAttributes(model);
            return getPrefix() + GET_COMPOSITE_PATH;
        }
        return REDIRECT_TASKS_PATH + chargePointServiceClient.getCompositeSchedule(params);
    }
}
