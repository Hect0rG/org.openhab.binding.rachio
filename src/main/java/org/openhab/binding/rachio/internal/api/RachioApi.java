/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioEvent.RachioApiResult;
import org.openhab.binding.rachio.internal.api.RachioHttp.RachioHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link RachioApi} implements the interface to the Rachio cloud service (using http).
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */

public class RachioApi {
    private static final Logger logger = LoggerFactory.getLogger(RachioApi.class);

    protected String apikey = "";
    protected String personId = "";
    protected String userName = "";
    protected String fullName = "";
    protected String email = "";
    protected RachioApiResult lastApiResult = new RachioApiResult();
    private HashMap<String, RachioDevice> deviceList = new HashMap<String, RachioDevice>();
    private RachioHttp httpApi = null;

    class RachioCloudPersonId {
        String id = ""; // "id":"xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx"
    }

    class RachioCloudStatus {
        long createDate = -1; // "createDate":1494626927000,
        String id = ""; // "id":"xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
        String username = ""; // "username":"markus7017",
        String fullName = ""; // "fullName":"Markus Michels",
        String email = ""; // "email":"markus.michels@me.com",
        public ArrayList<RachioCloudDevice> devices = new ArrayList<>(); // "devices":[]
        boolean deleted = false; // "deleted":false
    } // class RachioCloudStatus

    public class RachioApiWebHookEntry {
        public long createDate = -1;
        public long lastUpdateDate = -1;
        public String id = "";
        public String url = "";
        public String externalId = "";
    }

    public class RachioApiWebHookList {
        public ArrayList<RachioApiWebHookEntry> webhooks = new ArrayList<>();

    }

    public class RachioCloudDelta {
        // V3: ZONE_DELTA / SCHEDULE_DELTA
        String routingId = ""; // "routingId" : "d3beb3ab-b85a-49fe-a45d-37c4d95ea9a8",
        String icon = ""; // "icon" : "NO_ICON",
        String action = ""; // "action" : "UPDATED",
        String zoneId = ""; // "zoneId" : "e49c8b55-a553-4733-b1cf-0e402b97db49",
        String externalId = ""; // "externalId" : "cc765dfb-d095-4ceb-8062-b9d88dcce911",
        String subType = ""; // "subType" : "ZONE_DELTA",
        String id = ""; // "id" : "e9d4fa9f-1619-37c4-b457-3845620643d2",
        String type = ""; // "type" : "DELTA",
        String category = ""; // "category" : "DEVICE",
        String deviceId = ""; // "deviceId" : "d3beb3ab-b85a-49fe-a45d-37c4d95ea9a8",
        String timestamp = ""; // "timestamp" : "2018-04-09T23:17:14.365Z"
    }

    public RachioApi(String personId) {
        this.personId = personId;
    }

    public RachioApiResult getLastApiResult() {
        return lastApiResult;
    }

    protected void setApiResult(RachioApiResult result) {
        lastApiResult = result;
    }

    public String getPersonId() {
        return personId;
    }

    public boolean initialize(String apikey, ThingUID bridgeUID) throws RachioApiException {
        try {
            this.apikey = apikey;
            httpApi = new RachioHttp(this.apikey);
            if (initializePersonId() && initializeDevices(bridgeUID) && initializeZones()) {
                logger.trace("Rachio API initialized");
                return true;
            }

            httpApi = null;
            logger.error("RachioApi.initialize(): API initialization failed!");
            return false;
        } catch (RachioHttpException e) {
            throw new RachioApiException(e.getMessage());
        }
    } // initialize()

    public HashMap<String, RachioDevice> getDevices() {
        return deviceList;
    }

    public RachioDevice getDevByUID(ThingUID bridgeUID, ThingUID thingUID) {
        for (HashMap.Entry<String, RachioDevice> entry : deviceList.entrySet()) {
            RachioDevice dev = entry.getValue();
            logger.trace("RachioDev.getDevByUID: bridge {} / {}, device {} / {}", bridgeUID, dev.bridge_uid, thingUID,
                    dev.dev_uid);
            if (dev.bridge_uid.equals(bridgeUID) && dev.getUID().equals(thingUID)) {
                logger.trace("RachioApi: Device '{}' found.", dev.name);
                return dev;
            }
        }
        logger.debug("RachioApi.getDevByUID: Unable map UID to device");
        return null;
    }
    // getDevByUID()

    public RachioZone getZoneByUID(ThingUID bridgeUID, ThingUID zoneUID) {
        HashMap<String, RachioDevice> deviceList = getDevices();
        if (deviceList == null) {
            return null;
        }
        for (HashMap.Entry<String, RachioDevice> de : deviceList.entrySet()) {
            RachioDevice dev = de.getValue();

            HashMap<String, RachioZone> zoneList = dev.getZones();
            for (HashMap.Entry<String, RachioZone> ze : zoneList.entrySet()) {
                RachioZone zone = ze.getValue();
                if (zone.getUID().equals(zoneUID)) {
                    return zone;
                }
            }
        }
        return null;
    } // getDevByUID()

    private Boolean initializePersonId() throws RachioApiException, RachioHttpException {
        if (!personId.isEmpty()) {
            logger.trace("RachioApi: Using cached personId ('{}').", personId);
            return true;
        }

        lastApiResult = httpApi.httpGet(APIURL_BASE + APIURL_GET_PERSON, null);
        Gson gson = new Gson();
        RachioCloudPersonId pid = gson.fromJson(lastApiResult.resultString, RachioCloudPersonId.class);
        personId = pid.id;
        logger.debug("Using personId '{}'", personId);
        if (lastApiResult.isRateLimitCritical()) {
            String errorMessage = MessageFormat.format(
                    "Rachio Cloud API Rate Limit is critical ({0} of {1}), reset at {2}", lastApiResult.rateRemaining,
                    lastApiResult.rateLimit, lastApiResult.rateReset);
            throw new RachioApiException(errorMessage, lastApiResult);
        }
        return true;
    } // initializePersonId()

    public String getUserInfo() {
        String info = "";
        if (userName != null) {
            info = fullName + "(" + userName + ", " + email + ")";
        }
        return info;
    }

    public Boolean stopWatering(String deviceId) throws Exception {
        try {
            httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_STOP, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("Watering stopped for device '{}'", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_STOP, e.getMessage());
        }
        return false;
    } // stopWatering()

    public Boolean enableDevice(String deviceId) throws Exception {
        try {
            httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_ON, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("RachioApi: Device '{}' enabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_ON, e.getMessage());
        }
        return false;
    } // enableDevice

    public Boolean disableDevice(String deviceId) throws Exception {
        try {
            httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_OFF, "{ \"id\" : \"" + deviceId + "\" }");
            logger.debug("RachioApi: Device '{}' disabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_OFF, e.getMessage());
        }
        return false;
    } // disableDevice

    public Boolean rainDelay(String deviceId, Integer delay) throws Exception {
        try {
            httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_RAIN_DELAY,
                    "{ \"id\" : \"" + deviceId + "\", \"durartion\" : " + delay + " }");
            logger.debug("RachioApi: Rain Delay for Device '{}' enabled.", deviceId);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_DEV_PUT_RAIN_DELAY, e.getMessage());
        }
        return false;
    } // rainDelay

    public boolean runMultilpeZones(String zoneListJson) {
        try {
            httpApi.httpPut(APIURL_BASE + APIURL_ZONE_PUT_MULTIPLE_START, zoneListJson);
            logger.debug("RachioApi: Multiple zones started started.");
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_ZONE_PUT_MULTIPLE_START, e.getMessage());
        }
        return false;
    } // startZone()

    public boolean runZone(String zoneId, int duration) {
        try {
            logger.debug("RachioApi: Start zone '{}' for {} sec.", zoneId, duration);
            httpApi.httpPut(APIURL_BASE + APIURL_ZONE_PUT_START,
                    "{ \"id\" : \"" + zoneId + "\", \"duration\" : " + duration + " }");
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: Running zone '{}' for {}sec failed: {}", zoneId, duration, e.getMessage());
        }
        return false;
    } // startZone()

    public Boolean getDeviceInfo(String deviceId) throws Exception {
        try {
            httpApi.httpGet(APIURL_BASE + APIURL_GET_DEVICE + "/" + deviceId, null);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_GET_DEVICE, e.getMessage());
        }
        return false;
    } // getDeviceInfo

    public boolean registerWebHook(String deviceId, String callbackUrl, String externalId, Boolean clearAllCallbacks) {
        // first check/delete existing webhooks

        String json = "";
        try {
            json = httpApi.httpGet(APIURL_BASE + APIURL_DEV_QUERY_WEBHOOK + "/" + deviceId + "/webhook",
                    null).resultString; // throws
            logger.debug("RachioApi: Registered webhooks for device '{}': {}", deviceId, json);
            json = "{\"webhooks\":" + json + "}";
            Gson gson = new Gson();
            RachioApiWebHookList list = gson.fromJson(json, RachioApiWebHookList.class);
            for (int i = 0; i < list.webhooks.size(); i++) {
                RachioApiWebHookEntry whe = list.webhooks.get(i);
                logger.debug("RachioApi: WebHook #{}: id='{}', url='{}', externalId='{}'", i, whe.id, whe.url,
                        whe.externalId);
                if (clearAllCallbacks || whe.url.equals(callbackUrl)) {
                    logger.debug("RachioApi: The callback url '{}' is already registered -> delete", callbackUrl);
                    httpApi.httpDelete(APIURL_BASE + APIURL_DEV_DELETE_WEBHOOK + "/" + whe.id, null);
                }
            }

        } catch (Exception e) {
            logger.debug("RachioApi: Deleting WebHook(s); failed: {}, JSON='{}'", e.getMessage(), json);
        }

        try {
            // {
            // "device":{"id":"2a5e7d3c-c140-4e2e-91a1-a212a518adc5"},
            // "externalId" : "external company ID",
            // "url":"https://www.mydomain.com/another_webhook",
            // "eventTypes":[{"id":"1"},{"id":"2"}]
            // }
            //
            logger.debug("RachioApi: Register WebHook, callback url = '{}'", callbackUrl);
            String jsonData = "{ " + "\"device\":{\"id\":\"" + deviceId + "\"}, " + "\"externalId\" : \"" + externalId
                    + "\", " + "\"url\" : \"" + callbackUrl + "\", " + "\"eventTypes\" : [" + "{\"id\" : \""
                    + WHE_DEVICE_STATUS + "\"}, " + "{\"id\" : \"" + WHE_RAIN_DELAY + "\"}, " + "{\"id\" : \""
                    + WEATHER_INTELLIGENCE + "\"}, " + "{\"id\" : \"" + WHE_WATER_BUDGET + "\"}, " + "{\"id\" : \""
                    + WHE_ZONE_DELTA + "\"}, " + "{\"id\" : \"" + WHE_SCHEDULE_STATUS + "\"}, " + "{\"id\" : \""
                    + WHE_ZONE_STATUS + "\"}, " + "{\"id\" : \"" + WHE_DELTA + "\"} " + "] }";
            httpApi.httpPost(APIURL_BASE + APIURL_DEV_POST_WEBHOOK, jsonData);
            return true;
        } catch (Exception e) {
            logger.error("RachioApi: {} failed: {}", APIURL_GET_DEVICE, e.getMessage());
        }
        return false;
    }

    // ------------ internal stuff

    private Boolean initializeDevices(ThingUID BridgeUID) {
        String json = "";
        try {
            if (httpApi == null) {
                logger.debug("RachioApi.initializeDevices: API not initialized");
                return false;
            }
            json = httpApi.httpGet(APIURL_BASE + APIURL_GET_PERSONID + "/" + personId, null).resultString;
            logger.trace("RachioApi: Initialize from JSON='{}'", json);

            Gson gson = new Gson();
            RachioCloudStatus cloudStatus = gson.fromJson(json, RachioCloudStatus.class);
            userName = cloudStatus.username;
            fullName = cloudStatus.fullName;
            email = cloudStatus.email;

            deviceList = new HashMap<String, RachioDevice>(); // discard current list
            for (int i = 0; i < cloudStatus.devices.size(); i++) {
                RachioCloudDevice device = cloudStatus.devices.get(i);
                if (!device.deleted) {
                    deviceList.put(device.id, new RachioDevice(device));
                    logger.trace("RachioApi: Device '{}' initialized, {} zones.", device.name, device.zones.size());
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("RachioApi.initializeDevices: Exception: {}; JSON='{}'", e.getMessage(), json);
        }
        logger.error("RachioApi.initializeDevices: Initialization failed: JSON='{}'", json);
        return false;
    } // initializeDevices()

    public Boolean initializeZones() {
        return true;
    } // initializeZones()

    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_VENDOR, RachioBindingConstants.BINDING_VENDOR);
        properties.put(RachioBindingConstants.PROPERTY_APIKEY, apikey);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_ID, personId);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_USER, userName);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_NAME, fullName);
        properties.put(RachioBindingConstants.PROPERTY_PERSON_EMAIL, email);
        return properties;
    }

    public static void copyMatchingFields(Object fromObj, Object toObj) {
        if (fromObj == null || toObj == null) {
            throw new NullPointerException("Source and destination objects must be non-null");
        }

        Class fromClass = fromObj.getClass();
        Class toClass = toObj.getClass();

        Field[] fields = fromClass.getFields(); // .getDeclaredFields();
        for (Field f : fields) {
            try {
                String fname = f.getName();
                Field t = toClass.getSuperclass().getDeclaredField(fname);

                if (t.getType() == f.getType()) {
                    // extend this if to copy more immutable types if interested
                    if (t.getType() == String.class || t.getType() == int.class || t.getType() == long.class
                            || t.getType() == double.class || t.getType() == char.class || t.getType() == boolean.class
                            || t.getType() == Double.class || t.getType() == Integer.class || t.getType() == Long.class
                            || t.getType() == Character.class || t.getType() == Boolean.class) {
                        f.setAccessible(true);
                        t.setAccessible(true);
                        t.set(toObj, f.get(fromObj));
                    } else if (t.getType() == Date.class) {
                        // dates are not immutable, so clone non-null dates into the destination object
                        Date d = (Date) f.get(fromObj);
                        f.setAccessible(true);
                        t.setAccessible(true);
                        t.set(toObj, d != null ? d.clone() : null);
                    } else if (t.getType() == java.util.ArrayList.class) {
                        // dates are not immutable, so clone non-null dates into the destination object
                        ArrayList a = (ArrayList) f.get(fromObj);
                        f.setAccessible(true);
                        t.setAccessible(true);
                        t.set(toObj, a != null ? a.clone() : null);
                    } /*
                       * else if ((t
                       * .getType() ==
                       * org.openhab.binding.rachio.internal.api.RachioCloudZone.RachioCustomeNozzle.class)
                       * || (t.getType() ==
                       * org.openhab.binding.rachio.internal.api.RachioCloudZone.RachioCustomSoil.class)
                       * || (t.getType() ==
                       * org.openhab.binding.rachio.internal.api.RachioCloudZone.RachioCustomSlope.class)
                       * || (t.getType() ==
                       * org.openhab.binding.rachio.internal.api.RachioCloudZone.RachioCustomCrop.class)
                       * || (t.getType() ==
                       * org.openhab.binding.rachio.internal.api.RachioCloudZone.RachioCustomCrop.class)) {
                       * f.setAccessible(true);
                       * t.setAccessible(true);
                       * t.set(toObj, f.get(fromObj));
                       * }
                       */ else {
                        logger.debug("RachioApiInternal: Unable to update field '{}', '{}'", t.getName(), t.getType());
                    }
                }
            } catch (NoSuchFieldException ex) {
                // skip it
            } catch (IllegalAccessException ex) {
                logger.error("Unable to copy field: {}", f.getName());
            }
        } // or (Field f : fields)
    } // copyMatchingFields

} // class
