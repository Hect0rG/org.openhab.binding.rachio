/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 */
package org.openhab.binding.rachio.internal.api;

/**
 *
 * @author Markus Michels (markus7017) - Initial contribution
 */
class RachioCloudZone {
    public String id = ""; // "id":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx",
    public int zoneNumber = 0; // "zoneNumber":6,
    public String name = ""; // "name":"Zone 6 - Garage back",
    public boolean enabled = true; // "enabled":true,
    // public RachioCustomeNozzle customNozzle ;
    // public RachioCustomSoil customSoil;
    // public RachioCustomSlope customSlope;
    // public RachioCustomCrop customCrop;
    // public RachioCustomCrop customShade;
    public double availableWater = 0.0; // "availableWater":0.05,
    public double rootZoneDepth = 0.0; // "rootZoneDepth":9.0,
    public double managementAllowedDepletion = 0.0; // "managementAllowedDepletion":0.5,
    public double efficiency = 0.0; // "efficiency":0.8,
    public int yardAreaSquareFeet = 0; // "yardAreaSquareFeet":500,
    public String imageUrl = ""; // "imageUrl":"https://prod-media-photo.rach.io/daf89bd1-0744-4603-a5b4-32218fceaae6",
    public long lastWateredDate = -1; // "lastWateredDate":1523129743000,
    public boolean scheduleDataModified = false; // "scheduleDataModified":false,
    public int fixedRuntime = 0; // "fixedRuntime":0,
    // public String wateringAdjustmentRuntimes[] = new String[0]; // {"1":941,"2":784,"3":627,"4":470,"5":314},
    public double saturatedDepthOfWater = 0.0; // "saturatedDepthOfWater":0.25,
    public double depthOfWater = 0.0; // "depthOfWater":0.23,
    public int maxRuntime = 0; // "maxRuntime":10800,
    public int runtimeNoMultiplier = 0; // "runtimeNoMultiplier":627,
    public int runtime = 0; // "runtime":627

    class RachioCustomeNozzle {
        public String name = ""; // "name":"FIXED_SPRAY_HEAD",
        public Double inchesPerHour = 0.0; // "inchesPerHour":1.5
    }

    class RachioCustomSoil {
        public String name = ""; // "name":"ZERO_THREE",
        public int sortOrder = 0; // "sortOrder":0
    }

    class RachioCustomSlope {
        public String name = ""; // "name":"SAND"
    }

    class RachioCustomCrop {
        public String name = ""; // "name":"Warm Season Grass",
        public Double coefficient = 0.0; // "coefficient":0.65
    }

    class RachioCustomShade {
        public String name = ""; // "name":"LOTS_OF_SUN"
    }
} // class RachioCloudZone
